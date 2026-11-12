// 조종사 대시보드 - 내 작업 목록
// - 목록 데이터 : 서버(th:each)가 그려 둔 <li> / 주변 농지 : OTHER_FARMS (th:inline)
// - JS : 탭 필터, 작업 선택, 지도 마커
// - 지도(map)는 pilot-gps.js 가 만든 것을 그대로 사용
document.addEventListener("DOMContentLoaded", () => {
  const list = document.querySelector("#pilot-task-list");
  if (!list) return;

  const emptyMsg = document.querySelector("#pilot-task-empty");
  const tabs = document.querySelectorAll("#pilot-tabs button");
  const items = () => [...list.querySelectorAll("li")];
  const buttons = () => [...list.querySelectorAll(".pilot-task")];

  // 진행 중 탭을 기본으로 보여준다.
  let currentFilter = "active";

  let selectedNo = list.dataset.selected;
  let markers = [];

  // 상태별 우선순위
  const STATUS_ORDER = {
    IN_PROGRESS: 0,
    ASSIGNED: 1,
    INTERRUPTED: 2,
    PAUSED: 2,
    STOPPED: 3
  };

  // 대시보드에 남는 작업의 상태별 우선순위 정렬
  const sortTasks = () => {
    const taskItems = items();

    taskItems.sort((a, b) => {
      const statusA = a.dataset.status;
      const statusB = b.dataset.status;

      const orderA = STATUS_ORDER[statusA] ?? 5;
      const orderB = STATUS_ORDER[statusB] ?? 5;

      if (orderA !== orderB) {
        return orderA - orderB;
      }

      return 0;
    });

    taskItems.forEach((li) => list.appendChild(li));
  };

  sortTasks();
  const hasMap = () => typeof map !== "undefined" && typeof L !== "undefined";

  /* ===== 1. 탭: 개수 표시 + 보였다/숨겼다 ===== */
  const NAMES = {
    active: "작업중",
    stopped: "중단 및 인계"
  };

  const renderTabs = () => {
    const counts = {
      active: items().filter(
        (li) => li.dataset.group === "active"
      ).length,

      stopped: items().filter(
        (li) => li.dataset.group === "stopped"
      ).length
    };

    tabs.forEach((btn) => {
      const key = btn.dataset.filter;

      btn.textContent = `${NAMES[key]} (${counts[key]})`;
      btn.classList.toggle("active", key === currentFilter);
    });
  };

  const applyFilter = () => {
    let visible = 0;

    items().forEach((li) => {
      const show = li.dataset.group === currentFilter;

      li.hidden = !show;

      if (show) visible++;
    });

    emptyMsg.hidden = visible > 0;
  };

  tabs.forEach((btn) => {
    btn.addEventListener("click", () => {
      currentFilter = btn.dataset.filter;

      renderTabs();
      applyFilter();

      // 바뀐 탭에서 현재 보이는 첫 번째 작업을 자동 선택
      const firstVisible = buttons().find((b) => !b.closest("li").hidden);
      if (firstVisible) {
        selectTask(firstVisible.dataset.no, true, firstVisible);
      } else {
        renderMarkers();
      }
    });
  });

  /* ===== 2. 작업 선택 ===== */

  // 선택된 작업의 농지 다각형 및 이전 비행 궤적 레이어 보관 변수
  let currentPolygonLayer = null;
  let currentFlightPathLayer = null;
  let currentAssignedGeometryLayer = null;

  // 이전 선택 레이어 지우기 함수
  const clearSelectedTaskLayers = () => {
    if (currentPolygonLayer && hasMap()) {
      map.removeLayer(currentPolygonLayer);
      currentPolygonLayer = null;
    }

    if (currentAssignedGeometryLayer && hasMap()) {
    map.removeLayer(currentAssignedGeometryLayer);
    currentAssignedGeometryLayer = null;
    }

    if (currentFlightPathLayer && hasMap()) {
      map.removeLayer(currentFlightPathLayer);
      currentFlightPathLayer = null;
    }
  };

  // 선택된 작업의 농지 다각형과 이전 궤적 그리기 함수
  const drawSelectedTaskLayers = (btn) => {
    if (!hasMap() || !btn) return;

    clearSelectedTaskLayers();

    const d = btn.dataset;

    // 1. 농지 외곽선 다각형 그리기
    if (d.polygon && d.polygon !== "null" && d.polygon.trim() !== "") {
      try {
        const polyCoords = JSON.parse(d.polygon);

        if (Array.isArray(polyCoords) && polyCoords.length >= 3) {
          const poly = L.polygon(polyCoords, {
            color: "#238451",
            weight: 2,
            dashArray: "4 4",
            fillColor: "#54a77b",
            fillOpacity: 0.15,
          });

          if (poly && map) {
            poly.addTo(map);
            poly.bindTooltip(`🌱 ${d.address} (농지 전체 경계)`, {
              sticky: true
            });

            currentPolygonLayer = poly;
          }
        }
      } catch (e) {
        console.error("다각형 파싱 에러: ", e);
      }
    }

    // 2. 조종사 본인에게 배정된 담당 구역
    if (
    d.assignedGeometry &&
    d.assignedGeometry !== "null" &&
    d.assignedGeometry.trim() !== ""
   ) {
    try {
    const geometries = JSON.parse(d.assignedGeometry);

    if (Array.isArray(geometries) && geometries.length > 0) {
      const features = geometries.map((geometry) => ({
        type: "Feature",
        properties: {},
        geometry: geometry.type === "Feature"
          ? geometry.geometry
          : geometry
      }));

      const assignedLayer = L.geoJSON({
        type: "FeatureCollection",
        features
      }, {
        style: {
          color: "#238451",
    weight: 2,
    dashArray: "4 4",
    fillColor: "#54a77b",
    fillOpacity: 0.15
        }
      });

      assignedLayer.addTo(map);
      assignedLayer.bindTooltip("🚁 내 담당 구역", {
        sticky: true
      });

      currentAssignedGeometryLayer = assignedLayer;
    }
  } catch (e) {
    console.error("담당 구역 좌표 파싱 에러:", e);
  }
}

    // 3. 이전 조종사 비행 궤적 그리기
    if (d.flightPath && d.flightPath !== "null" && d.flightPath.trim() !== "") {
      try {
        const pathCoords = JSON.parse(d.flightPath);

        if (Array.isArray(pathCoords) && pathCoords.length > 0) {
          const line = L.polyline(pathCoords, {
            color: "#e74c3c",
            weight: 4,
            opacity: 0.85,
            dashArray: "8 8",
          });

          if (line && map) {
            line.addTo(map);
            line.bindTooltip("⚠️ 이전 조종사 방제 궤적", {
              sticky: true
            });

            currentFlightPathLayer = line;
          }
        }
      } catch (e) {
        console.error("비행 궤적 파싱 에러:", e);
      }
    }
  };

  const selectTask = (no, moveMap = true, targetBtn = null) => {
    selectedNo = String(no);

    // 현재 탭에 보이는 버튼 중에서 우선 찾고, 
    // 직접 클릭한 targetBtn 이 있으면 그것을 최우선으로 
    const btn = targetBtn
      || buttons().find((b) => b.dataset.no === selectedNo && !b.closest("li").hidden)
      || buttons().find((b) => b.dataset.no === selectedNo);

    buttons().forEach((b) => {
      b.classList.toggle("selected", b.dataset.no === selectedNo);
    });

    renderMarkers();

    if (btn) {
      drawSelectedTaskLayers(btn);

      if (moveMap && hasMap() && btn.dataset.lat && btn.dataset.lng) {
        map.setView(
          [Number(btn.dataset.lat), Number(btn.dataset.lng)],
          16
        );
      }
    } else {
      clearSelectedTaskLayers();
    }
  };

  list.addEventListener("click", (e) => {
    const btn = e.target.closest(".pilot-task");

    if (btn) {
      selectTask(btn.dataset.no, true, btn); // 클릭한 버튼(btn) 자체를 전달!
    }
  });

  /* ===== 3. 지도 마커 ===== */

  // 팝업 내용: 글자를 textContent로 넣어서 안전하게
  const popupOf = (title, lines, linkHref) => {
    const box = document.createElement("div");
    const b = document.createElement("b");

    b.textContent = title;
    box.append(b);

    lines.forEach((line) => {
      box.append(
        document.createElement("br"),
        document.createTextNode(line)
      );
    });

    if (linkHref) {
      const a = document.createElement("a");

      a.href = linkHref;
      a.textContent = "작업 이력에서 처리 →";

      box.append(document.createElement("br"), a);
    }

    return box;
  };

  const renderMarkers = () => {
    if (!hasMap()) return;

    markers.forEach((m) => map.removeLayer(m));
    markers = [];

    // (1) 주변 미배정 농지
    (typeof OTHER_FARMS !== "undefined" ? OTHER_FARMS : []).forEach((f) => {
      if (f.latitude == null || f.longitude == null) return;

      const m = L.circleMarker([f.latitude, f.longitude], {
        radius: 7,
        fillColor: f.stColor,
        color: "#fff",
        weight: 2,
        fillOpacity: 0.8,
      }).addTo(map);

      m.bindPopup(
        popupOf(`🌱 ${f.address}`, [`상태: ${f.statusLabel}`])
      );

      markers.push(m);
    });

    // (2) 현재 탭에 표시되는 내 작업만 지도에 표시
    buttons()
      .filter((btn) => !btn.closest("li").hidden)
      .forEach((btn) => {
        const d = btn.dataset;

        if (!d.lat || !d.lng) return;

        const isSelected = d.no === selectedNo;

        const m = L.circleMarker(
          [Number(d.lat), Number(d.lng)],
          {
            radius: isSelected ? 11 : 9,
            fillColor: d.color,
            color: isSelected ? "#000" : "#fff",
            weight: isSelected ? 3 : 2,
            fillOpacity: 0.9,
          }
        ).addTo(map);

        m.bindPopup(
          popupOf(
            `🏠 ${d.address}`,
            [
              `상태: ${d.status}`,
              `면적: ${Number(d.area || 0).toLocaleString()}㎡`
            ],
            "/pilot/history"
          )
        );

        m.on("click", () => selectTask(d.no, false));

        markers.push(m);
      });

    setTimeout(() => map.invalidateSize(), 100);
  };

  /* ===== 처음 실행 ===== */
  renderTabs();
  applyFilter();

  // 숨겨진(hidden) 완료 작업이 아니라, 현재 탭에서 보이는 작업 중에서만 초기 선택
  const visibleMatch = selectedNo
   ? buttons().find((b) => b.dataset.no === String(selectedNo) && !b.closest("li").hidden)
   : null;
  const firstVisible = visibleMatch || buttons().find((b) => !b.closest("li").hidden);

  if (firstVisible) {
    selectTask(firstVisible.dataset.no, true, firstVisible);
  } else {
    renderMarkers();
  }
  if (hasMap()) {
    addHazardMakers(map);
  }
})
