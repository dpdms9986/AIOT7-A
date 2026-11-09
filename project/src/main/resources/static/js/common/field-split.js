// 농지 다각형 공용 모듈 (FieldSplit 프로토타입 이식)
// - 농지 관리(farmlist) : 지도에서 외곽 그리기
// - 작업 배정(task-assignment) : 1/N 경도 분할
// 필요: Leaflet(L), Turf.js 6.5 (turf)
// ※ 좌표는 항상 [위도, 경도] (Leaflet 순서). Turf 로 넘길 때만 [경도, 위도]로 바꿈.
window.FieldSplit = (() => {
  "use strict";

  /* 조각별 색상 팔레트 (원본 그대로) */
  const COLORS = [
    { fill: "#54a77b", line: "#36875f" },
    { fill: "#64a8b0", line: "#408792" },
    { fill: "#d6a34c", line: "#b88430" },
    { fill: "#9879b5", line: "#79589a" },
    { fill: "#d47e67", line: "#bd6049" },
    { fill: "#7b9cce", line: "#5d7fb1" },
    { fill: "#a8a65a", line: "#868640" },
    { fill: "#c57c9d", line: "#a95e7e" },
    { fill: "#6d9b83", line: "#4e7c65" },
    { fill: "#b38a62", line: "#956d47" },
    { fill: "#668da6", line: "#4b718a" },
    { fill: "#9c8c72", line: "#7e7059" },
  ];

  /* ===================== 1. 좌표 도우미 ===================== */

  function closeRing(coordinates) {
    const ring = coordinates.map(([lat, lng]) => [lat, lng]);
    const first = ring[0];
    const last = ring[ring.length - 1];
    if (first[0] !== last[0] || first[1] !== last[1]) ring.push([...first]);
    return ring;
  }

  /** DB 에 저장된 JSON 글자 → [[lat,lng],...] (이상하면 null) */
  function parsePolygon(text) {
    if (!text || typeof text !== "string" || !text.trim()) return null;
    let parsed;
    try { parsed = JSON.parse(text); } catch { return null; }
    if (!Array.isArray(parsed) || parsed.length < 3) return null;
    const ok = parsed.every((p) =>
      Array.isArray(p) && p.length >= 2 &&
      Number.isFinite(p[0]) && Number.isFinite(p[1]) &&
      p[0] >= -90 && p[0] <= 90 && p[1] >= -180 && p[1] <= 180);
    return ok ? parsed.map(([lat, lng]) => [lat, lng]) : null;
  }

  /** 다각형 좌표가 없는 옛 농지용: 중심 + 면적(㎡)으로 정사각형 4점 만들기 */
  function fallbackPolygon(lat, lng, areaSize) {
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
    const area = Number(areaSize) > 0 ? Number(areaSize) : 1000;
    const half = Math.sqrt(area) / 2;                       // 한 변의 절반 (m)
    const dLat = half / 111320;
    const dLng = half / (111320 * Math.cos((lat * Math.PI) / 180));
    return [
      [lat + dLat, lng - dLng],
      [lat + dLat, lng + dLng],
      [lat - dLat, lng + dLng],
      [lat - dLat, lng - dLng],
    ];
  }

  /* ===================== 2. 1/N 경도 분할 (원본 알고리즘 그대로) ===================== */

  function splitPolygonIntoN(coordinates, n) {
    if (!Array.isArray(coordinates) || coordinates.length < 3) {
      throw new Error("다각형을 만들려면 꼭짓점 좌표가 3개 이상 필요합니다.");
    }
    if (!Number.isInteger(n) || n < 2 || n > 12) {
      throw new Error("분할 개수는 2개에서 12개 사이의 정수로 입력해 주세요.");
    }
    if (!window.turf) {
      throw new Error("Turf.js를 불러오지 못했습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.");
    }

    const ring = coordinates.map(([lat, lng]) => [lng, lat]);
    const first = ring[0];
    const last = ring[ring.length - 1];
    if (first[0] !== last[0] || first[1] !== last[1]) {
      ring.push([...first]);
    }

    let polygon;
    try {
      polygon = turf.polygon([ring]);
      // Turf 6.5 exports kinks, but does not include booleanValid.
      if (typeof turf.kinks === "function" && turf.kinks(polygon).features.length > 0) {
        throw new Error("좌표가 서로 교차하거나 다각형 모양이 올바르지 않습니다.");
      }
    } catch (error) {
      if (error instanceof Error && error.message.includes("좌표가 서로")) throw error;
      throw new Error("유효한 다각형을 만들 수 없습니다. 좌표 순서와 모양을 확인해 주세요.");
    }

    const [minLng, minLat, maxLng, maxLat] = turf.bbox(polygon);
    const width = maxLng - minLng;
    if (!Number.isFinite(width) || width <= 0) {
      throw new Error("다각형의 경도 폭이 0입니다. 서로 다른 경도 좌표를 입력해 주세요.");
    }
    const latitudePadding = Math.max((maxLat - minLat) * 0.01, 0.000001);
    const pieces = [];

    for (let i = 0; i < n; i += 1) {
      const west = minLng + (width * i) / n;
      const east = i === n - 1 ? maxLng : minLng + (width * (i + 1)) / n;
      const slice = turf.bboxPolygon([
        west,
        minLat - latitudePadding,
        east,
        maxLat + latitudePadding,
      ]);

      // Turf 6 expects the two polygon features as separate arguments.
      const intersection = turf.intersect(polygon, slice);
      if (intersection && intersection.geometry) {
        pieces.push({
          ...intersection,
          properties: {
            ...(intersection.properties || {}),
            sliceIndex: i,
            sliceCount: n,
          },
        });
      }
    }

    return pieces;
  }

  /* ===================== 3. 지도에서 외곽 그리기 ===================== */
  // 원본의 startDrawingMode / commitPendingPick / renderDrawingOutline 흐름.
  // ※ 실시간 면적 계산(점을 찍을 때마다 turf.area)은 제거했습니다.
  //
  // hooks.onStatus(message, state)  : 안내 문구 ("active" | "success" | "error" | "")
  // hooks.onChange(coordinates)     : 꼭짓점이 추가될 때마다
  // hooks.canPlace(lat, lng)        : false 를 돌려주면 그 지점은 찍히지 않음 (선택 사항)
  function createDrawer(map, hooks = {}) {
    let active = false;
    let coords = [];
    let layer = null;
    let pendingMarker = null;
    let pendingPopup = null;
    let pending = null;

    const say = (message, state = "") => hooks.onStatus && hooks.onStatus(message, state);

    function closePending() {
      if (pendingMarker) map.removeLayer(pendingMarker);
      if (pendingPopup) map.closePopup(pendingPopup);
      pendingMarker = null;
      pendingPopup = null;
      pending = null;
    }

    function clearLayer() {
      if (layer) map.removeLayer(layer);
      layer = null;
    }

    function renderOutline() {
      clearLayer();
      layer = L.layerGroup().addTo(map);

      if (coords.length >= 3) {
        L.polygon(coords, {
          color: "#247a5e", weight: 2, opacity: 0.95,
          fillColor: "#54a77b", fillOpacity: 0.14, interactive: false,
        }).addTo(layer);
      } else if (coords.length >= 2) {
        L.polyline(coords, {
          color: "#247a5e", weight: 2, dashArray: "5 4", interactive: false,
        }).addTo(layer);
      }

      coords.forEach(([latitude, longitude], index) => {
        L.circleMarker([latitude, longitude], {
          radius: 6, color: "#fff", weight: 2, fillColor: "#247a5e", fillOpacity: 1,
        })
          .bindTooltip(String(index + 1), {
            permanent: true, direction: "top", offset: [0, -5],
            className: "coordinate-index-tooltip",
          })
          .addTo(layer);
      });
    }

    function showPending(latitude, longitude, containerPoint) {
      closePending();
      pending = { latitude, longitude, containerPoint: L.point(containerPoint) };
      pendingMarker = L.circleMarker([latitude, longitude], {
        radius: 8, color: "#fff", weight: 3,
        fillColor: "#d08b35", fillOpacity: 1, bubblingMouseEvents: true,
      }).addTo(map);

      const content = document.createElement("div");
      const value = document.createElement("strong");
      value.className = "coordinate-pick-value";
      value.textContent = `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`;
      const help = document.createElement("p");
      help.className = "coordinate-pick-help";
      help.textContent = "같은 지점을 다시 누르거나 아래 버튼을 누르면 외곽 좌표에 추가됩니다.";
      const addButton = document.createElement("button");
      addButton.type = "button";
      addButton.className = "coordinate-pick-add";
      addButton.textContent = "이 점을 외곽 좌표에 추가";
      addButton.addEventListener("click", (event) => {
        event.preventDefault();
        event.stopPropagation();
        commit();
      });
      content.append(value, help, addButton);
      content.addEventListener("click", (event) => event.stopPropagation());

      pendingPopup = L.popup({
        className: "coordinate-pick-popup", closeButton: true, autoClose: true,
        closeOnClick: false, autoPan: false, offset: [12, -12],
      }).setLatLng([latitude, longitude]).setContent(content).openOn(map);
    }

    function commit() {
      if (!pending || !active) return;
      const latitude = Number(pending.latitude.toFixed(6));
      const longitude = Number(pending.longitude.toFixed(6));
      const isDuplicate = coords.some(([lat, lng]) =>
        map.distance([lat, lng], [latitude, longitude]) < 1);
      if (isDuplicate) {
        say("이미 추가된 지점과 너무 가까워요. 다른 위치를 선택해 주세요.", "error");
        closePending();
        return;
      }
      coords.push([latitude, longitude]);
      renderOutline();
      closePending();
      say(`${coords.length}개 꼭짓점 추가됨. 지도를 눌러 다음 점을 선택하세요.`, "active");
      if (hooks.onChange) hooks.onChange(coords.map((p) => [...p]));
    }

    function handleClick(event) {
      if (!active) return;
      if (pending && pending.containerPoint.distanceTo(event.containerPoint) <= 18) {
        commit();
        return;
      }

      // 허용 구역 검사: canPlace 훅이 있을 때만 동작하고, 없으면 기존처럼 어디든 찍힘
      if (hooks.canPlace && !hooks.canPlace(event.latlng.lat, event.latlng.lng)) {
        closePending();
        return;
      }

      showPending(event.latlng.lat, event.latlng.lng, event.containerPoint);
    }

    map.on("click", handleClick);

    return {
      isActive: () => active,
      getCoordinates: () => coords.map((p) => [...p]),

      /** 그리기 시작 (이전에 그린 점은 지움) */
      start() {
        closePending();
        clearLayer();
        coords = [];
        active = true;
        map.getContainer().classList.add("coordinate-picking");
        say("지점을 누른 뒤 같은 곳을 다시 누르거나 말풍선의 추가 버튼을 누르세요.", "active");
      },

      /** 그리기 끝. 꼭짓점 3개 이상 + 선이 꼬이지 않았는지 검사. */
      finish() {
        closePending();
        active = false;
        map.getContainer().classList.remove("coordinate-picking");

        if (coords.length < 3) {
          const message = `${coords.length}개 입력됨 — 다각형에는 꼭짓점이 3개 이상 필요합니다.`;
          say(message, "error");
          return { ok: false, coordinates: this.getCoordinates(), message };
        }
        try {
          const polygon = turf.polygon([closeRing(coords).map(([lat, lng]) => [lng, lat])]);
          if (typeof turf.kinks === "function" && turf.kinks(polygon).features.length > 0) {
            const message = "외곽 선이 서로 교차합니다. 꼭짓점 입력 순서를 확인하고 다시 그려 주세요." ;
            say(message, "error");
            return { ok: false, coordinates: this.getCoordinates(), message };
          }
        } catch {
          const message = "유효한 다각형을 만들 수 없습니다. 다시 그려 주세요.";
          say(message, "error");
          return { ok: false, coordinates: this.getCoordinates(), message };
        }
        const message = `${coords.length}개 꼭짓점으로 농지 외곽을 만들었어요.`;
        say(message, "success");
        return { ok: true, coordinates: this.getCoordinates(), message };
      },

      /** 그린 것 전부 지우고 그리기 모드 종료 */
      clear() {
        closePending();
        clearLayer();
        coords = [];
        active = false;
        map.getContainer().classList.remove("coordinate-picking");
      },
    };
  }

  return { COLORS, closeRing, parsePolygon, fallbackPolygon, splitPolygonIntoN, createDrawer };
})();