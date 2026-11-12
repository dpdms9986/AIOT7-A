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

  // 완료(COMPLETED)가 아닌 모든 상태는 진행 중으로 분류한다.
  // 진행 중 작업이 하나라도 있으면 진행 중 탭을 기본으로 보여준다.
  let currentFilter = items().some((li) => li.dataset.group === "active")
    ? "active"
    : "done";

  let selectedNo = list.dataset.selected;
  let markers = [];

  // IN_PROGRESS 작업을 항상 목록 맨 위에 배치한다.
  // 나머지 작업은 기존 순서를 유지한다.
  const pinInProgressTasks = () => {
    const taskItems = items();

    taskItems.sort((a, b) => {
      const aInProgress = a.dataset.status === "IN_PROGRESS";
      const bInProgress = b.dataset.status === "IN_PROGRESS";

      return Number(bInProgress) - Number(aInProgress);
    });

    taskItems.forEach((li) => list.appendChild(li));
  };

  pinInProgressTasks();

  const hasMap = () => typeof map !== "undefined" && typeof L !== "undefined";

  /* ===== 1. 탭: 개수 표시 + 보였다/숨겼다 ===== */
  const NAMES = {
    active: "진행 중",
    done: "완료"
  };

  const renderTabs = () => {
    const counts = {
      active: items().filter((li) => li.dataset.group === "active").length,
      done: items().filter((li) => li.dataset.group === "done").length,
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
      renderMarkers();
    });
  });

  /* ===== 2. 작업 선택 ===== */

  // 선택된 작업의 농지 다각형 및 이전 비행 궤적 레이어 보관 변수
  let currentPolygonLayer = null;
  let currentFlightPathLayer = null;

  // 이전 선택 레이어 지우기 함수
  const clearSelectedTaskLayers = () => {
    if (currentPolygonLayer && hasMap()) {
      map.removeLayer(currentPolygonLayer);
      currentPolygonLayer = null;
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
            poly.bindTooltip(`🌱 ${d.address} (내 작업 구역)`, {
              sticky: true
            });

            currentPolygonLayer = poly;
          }
        }
      } catch (e) {
        console.error("다각형 파싱 에러: ", e);
      }
    }

    // 2. 이전 조종사 비행 궤적 그리기
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

  const selectTask = (no, moveMap = true) => {
    selectedNo = String(no);

    buttons().forEach((b) => {
      b.classList.toggle("selected", b.dataset.no === selectedNo);
    });

    renderMarkers();

    const btn = buttons().find((b) => b.dataset.no === selectedNo);

    if (btn) {
      drawSelectedTaskLayers(btn);

      // 지도 중심 이동
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
      selectTask(btn.dataset.no);
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

  if (selectedNo) {
    selectTask(selectedNo);
  } else {
    renderMarkers();
  }

  if (hasMap()) {
    addHazardMarkers(map);
  }
});