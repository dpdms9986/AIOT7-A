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

  let currentFilter = "all";               // all / active / done
  let selectedNo = list.dataset.selected;   // 서버가 정한 첫 선택
  let markers = [];

  const hasMap = () => typeof map !== "undefined" && typeof L !== "undefined";

  /* ===== 1. 탭: 개수 표시 + 보였다/숨겼다 ===== */
  const NAMES = { all: "전체", active: "진행 중", done: "완료" };

  const renderTabs = () => {
    const counts = {
      all: items().length,
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
      const show = currentFilter === "all" || li.dataset.group === currentFilter;
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
  const selectTask = (no, moveMap = true) => {
    selectedNo = String(no);
    buttons().forEach((b) => b.classList.toggle("selected", b.dataset.no === selectedNo));
    renderMarkers();

    const btn = buttons().find((b) => b.dataset.no === selectedNo);
    if (moveMap && btn && hasMap() && btn.dataset.lat && btn.dataset.lng) {
      map.setView([Number(btn.dataset.lat), Number(btn.dataset.lng)], 16);
    }
  };

  list.addEventListener("click", (e) => {
    const btn = e.target.closest(".pilot-task");
    if (btn) selectTask(btn.dataset.no);
  });

  /* ===== 3. 지도 마커 ===== */
  // 팝업 내용: 글자를 textContent 로 넣어서 안전하게
  const popupOf = (title, lines, linkHref) => {
    const box = document.createElement("div");
    const b = document.createElement("b");
    b.textContent = title;
    box.append(b);
    lines.forEach((line) => {
      box.append(document.createElement("br"), document.createTextNode(line));
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

    // (1) 주변 미배정 농지 (th:inline 으로 받은 OTHER_FARMS)
    (typeof OTHER_FARMS !== "undefined" ? OTHER_FARMS : []).forEach((f) => {
      if (f.latitude == null || f.longitude == null) return;
      const m = L.circleMarker([f.latitude, f.longitude], {
        radius: 7, fillColor: f.stColor, color: "#fff", weight: 2, fillOpacity: 0.8,
      }).addTo(map);
      m.bindPopup(popupOf(`🌱 ${f.address}`, [`상태: ${f.statusLabel}`]));
      markers.push(m);
    });

    // (2) 내 작업 중 지금 탭에 보이는 것만
    buttons()
      .filter((btn) => !btn.closest("li").hidden)
      .forEach((btn) => {
        const d = btn.dataset;
        if (!d.lat || !d.lng) return;
        const isSelected = d.no === selectedNo;
        const m = L.circleMarker([Number(d.lat), Number(d.lng)], {
          radius: isSelected ? 11 : 9,
          fillColor: d.color,
          color: isSelected ? "#000" : "#fff",
          weight: isSelected ? 3 : 2,
          fillOpacity: 0.9,
        }).addTo(map);
        m.bindPopup(popupOf(
          `🏠 ${d.address}`,
          [`상태: ${d.status}`, `면적: ${Number(d.area || 0).toLocaleString()}㎡`],
          "/pilot/history"
        ));
        m.on("click", () => selectTask(d.no, false));   // 마커 클릭 → 목록도 선택
        markers.push(m);
      });

    setTimeout(() => map.invalidateSize(), 100);
  };

  /* ===== 처음 실행 ===== */
  renderTabs();
  applyFilter();
  if (selectedNo) selectTask(selectedNo);
  else renderMarkers();
});