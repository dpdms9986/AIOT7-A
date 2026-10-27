// 조종사 메인 화면 (Leaflet 지도 연동 버전)

/* ===== 상태 이름 → 화면 글자와 색 ===== */
const STATUS = {
  READY:       { text: "배정 전", cls: "st-ready", color: "#238451" },
  ASSIGNED:    { text: "대기",    cls: "st-assigned", color: "#f2b705" },
  IN_PROGRESS: { text: "진행 중", cls: "st-progress", color: "#2f80ed" },
  COMPLETED:   { text: "완료",    cls: "st-completed", color: "#8a8f8c" },
};

/* ===== 임시 데이터 ===== */
const DRONES = [
  { id: "DR-003", label: "DR-003 · DJI Agras T40" },
  { id: "DR-004", label: "DR-004 · DJI Agras T30" },
];

const CHEMICALS = [
  { id: "CH-01", label: "약제 A (살충제) · 재고 84L" },
  { id: "CH-02", label: "약제 B (살균제) · 재고 40L" },
  { id: "CH-03", label: "약제 C (영양제) · 재고 25L" },
];

// 내 작업 (lat, lng 실제 위경도 좌표로 변경)
const myTasks = [
  { id: 1, farm: "어남동 12-3", area: 800,  date: "2026-10-23 09:00", status: "IN_PROGRESS", lat: 36.3524, lng: 127.3865, drone: "DR-003", chemical: "CH-01", amount: 12, used: null },
  { id: 2, farm: "노은동 2-6",  area: 600,  date: "2026-10-23 13:00", status: "ASSIGNED",    lat: 36.3484, lng: 127.3815, drone: null, chemical: null, amount: null, used: null },
  { id: 3, farm: "어남동 14-2", area: 1100, date: "2026-10-24 08:30", status: "ASSIGNED",    lat: 36.3544, lng: 127.3905, drone: null, chemical: null, amount: null, used: null },
  { id: 4, farm: "관저동 9-4",  area: 1050, date: "2026-10-22 10:00", status: "COMPLETED",   lat: 36.3464, lng: 127.3785, drone: "DR-004", chemical: "CH-02", amount: 10, used: 1.1 },
];

// 주변의 배정 전 농지
const OTHER_FARMS = [
  { farm: "노은동 2-8", status: "READY", lat: 36.3564, lng: 127.3825 },
  { farm: "관저동 10-2", status: "READY", lat: 36.3444, lng: 127.3925 },
];

/* ===== 화면 요소 ===== */
const listBox = document.querySelector("#pilot-task-list");
const tabButtons = document.querySelectorAll("#pilot-tabs button");
const detailBox = document.querySelector("#pilot-detail") || createDetailBox(); // 상세 영역 안전장치

let currentFilter = "all"; // all / active / done
let selectedId = (myTasks.find((t) => t.status === "IN_PROGRESS") || myTasks[0]).id;

// Leaflet 지도 위에 그려진 농지 마커들을 관리할 배열
let taskMarkers = [];

// 상세 영역이 HTML에 없을 경우 동적으로 생성해주는 보조 함수
function createDetailBox() {
  const card = document.querySelectorAll('.dash-card')[1];
  let dBox = document.querySelector("#pilot-detail");
  if (!dBox && card) {
    dBox = document.createElement("div");
    dBox.id = "pilot-detail";
    dBox.style.cssText = "margin-top: 16px; padding: 16px; background: #f8f9fa; border-radius: 8px;";
    card.appendChild(dBox);
  }
  return dBox;
}

const labelOf = (list, id) => (list.find((item) => item.id === id) || {}).label || "-";

/* ===== 필터링 ===== */
function filteredTasks() {
  if (currentFilter === "active") {
    return myTasks.filter((t) => t.status === "ASSIGNED" || t.status === "IN_PROGRESS");
  }
  if (currentFilter === "done") {
    return myTasks.filter((t) => t.status === "COMPLETED");
  }
  return myTasks;
}

/* ===== 탭 렌더링 ===== */
function renderTabs() {
  const counts = {
    all: myTasks.length,
    active: myTasks.filter((t) => t.status === "ASSIGNED" || t.status === "IN_PROGRESS").length,
    done: myTasks.filter((t) => t.status === "COMPLETED").length,
  };
  const names = { all: "전체", active: "진행 중", done: "완료" };

  tabButtons.forEach((button) => {
    const key = button.dataset.filter;
    button.textContent = `${names[key]} (${counts[key]})`;
    button.classList.toggle("active", key === currentFilter);
  });
}

/* ===== 목록 렌더링 ===== */
function renderList() {
  const tasks = filteredTasks();

  if (tasks.length === 0) {
    listBox.innerHTML = `<li class="pilot-empty" style="padding: 20px; text-align: center; color: #666;">해당하는 작업이 없습니다.</li>`;
    return;
  }

  listBox.innerHTML = tasks.map((t) => {
    const st = STATUS[t.status];
    return `
      <li>
        <button type="button" class="pilot-task ${t.id === selectedId ? "selected" : ""}" data-id="${t.id}" style="width:100%; display:flex; align-items:center; padding:10px; border:1px solid #eee; background:#fff; cursor:pointer; margin-bottom:6px; border-radius:6px;">
          <span class="pilot-dot ${st.cls}" style="width:10px; height:10px; border-radius:50%; background:${st.color}; margin-right:8px;"></span>
          <span class="pilot-task-body" style="flex:1; text-align:left;">
            <strong style="display:block; font-size:14px;">${t.farm}</strong>
            <small style="color:#666;">${t.area.toLocaleString()}㎡ · ${t.date}</small>
          </span>
          <span class="pilot-status ${st.cls}" style="font-size:12px; padding:2px 6px; border-radius:4px; background:#eee;">${st.text}</span>
        </button>
      </li>
    `;
  }).join("");
}

/* ===== Leaflet 지도 마커 렌더링 (핵심 수정 부분) ===== */
function renderMapMarkers() {
  // 1. 기존에 지도에 그려둔 농지 마커들 싹 지우기 (html 지도 자체는 유지됨)
  taskMarkers.forEach(marker => map.removeLayer(marker));
  taskMarkers = [];

  // 2. 주변 대기 농지(OTHER_FARMS) 마커 찍기
  OTHER_FARMS.forEach(f => {
    const marker = L.circleMarker([f.lat, f.lng], {
      radius: 8,
      fillColor: STATUS[f.status].color,
      color: "#fff",
      weight: 2,
      opacity: 1,
      fillOpacity: 0.9
    }).addTo(map);
    
    marker.bindPopup(`<b>🌱 ${f.farm}</b><br>상태: ${STATUS[f.status].text}`);
    taskMarkers.push(marker);
  });

  // 3. 현재 필터링된 내 작업들만 지도에 새 마커로 찍기
  const tasksToDisplay = filteredTasks();
  tasksToDisplay.forEach(t => {
    const st = STATUS[t.status];
    const isSelected = t.id === selectedId;

    const marker = L.circleMarker([t.lat, t.lng], {
      radius: isisSelected ? 11 : 9,
      fillColor: st.color,
      color: isSelected ? "#000" : "#fff",
      weight: isSelected ? 3 : 2,
      opacity: 1,
      fillOpacity: 0.9
    }).addTo(map);

    marker.bindPopup(`<b>🏠 ${t.farm}</b><br>상태: ${st.text}<br>면적: ${t.area.toLocaleString()}㎡`);
    
    // 마커 클릭 시 작업 선택 연동
    marker.on('click', () => {
      selectTask(t.id);
    });

    taskMarkers.push(marker);
  });

  // 레이아웃 변경으로 인한 지도 깨짐 방지
  setTimeout(() => {
    if (typeof map !== 'undefined' && map.invalidateSize) {
      map.invalidateSize();
    }
  }, 100);
}

/* ===== 상세 정보 렌더링 ===== */
function renderDetail() {
  if (!detailBox) return;
  const t = myTasks.find((task) => task.id === selectedId);

  if (!t) {
    detailBox.innerHTML = `<p class="pilot-empty">목록이나 지도에서 작업을 선택하세요.</p>`;
    return;
  }

  const st = STATUS[t.status];
  let info = `
    <div style="display:grid; grid-template-columns: 80px 1fr; gap:6px; font-size:13px; margin-bottom:12px;">
      <b>면적</b> <span>${t.area.toLocaleString()}㎡</span>
      <b>작업 일시</b> <span>${t.date}</span>
      <b>상태</b> <span><span class="pilot-status ${st.cls}" style="color:${st.color}; font-weight:bold;">${st.text}</span></span>
    </div>
  `;
  let form = "";

  if (t.status === "ASSIGNED") {
    form = `
      <div class="pilot-form" style="border-top:1px solid #ddd; padding-top:10px; margin-top:10px;">
        <p class="pilot-form-title" style="font-weight:bold; margin-bottom:8px;">드론 및 약제 선택</p>
        <label style="display:block; margin-bottom:6px;">
          <span>드론</span>
          <select id="sel-drone" style="width:100%; padding:4px;">
            <option value="">드론 선택</option>
            ${DRONES.map((d) => `<option value="${d.id}">${d.label}</option>`).join("")}
          </select>
        </label>
        <label style="display:block; margin-bottom:6px;">
          <span>약제</span>
          <select id="sel-chem" style="width:100%; padding:4px;">
            <option value="">약제 선택</option>
            ${CHEMICALS.map((c) => `<option value="${c.id}">${c.label}</option>`).join("")}
          </select>
        </label>
      </div>
      <button type="button" class="dash-btn-primary" data-action="start" style="margin-top:10px; width:100%; padding:8px; background:#2f80ed; color:#fff; border:none; border-radius:4px; cursor:pointer;">작업 시작</button>
    `;
  } else if (t.status === "IN_PROGRESS") {
    form = `<a class="dash-btn-primary" href="pilot-history.html" style="display:block; text-align:center; margin-top:10px; padding:8px; background:#2f80ed; color:#fff; border-radius:4px; text-decoration:none;">작업 이력에서 완료 보고 →</a>`;
  }

  detailBox.innerHTML = `
    <h3 style="margin-bottom:8px; font-size:16px;">${t.farm}</h3>
    ${info}
    ${form}
  `;
}

function renderAll() {
  renderTabs();
  renderList();
  renderMapMarkers();
  renderDetail();
}

/* ===== 이벤트 핸들러 설정 ===== */
function selectTask(id) {
  selectedId = id;
  renderAll();
  
  // 선택된 작업 위치로 지도 중심 이동
  const t = myTasks.find(task => task.id === id);
  if (t && typeof map !== 'undefined') {
    map.setView([t.lat, t.lng], 16);
  }
}

listBox.addEventListener("click", (event) => {
  const item = event.target.closest("[data-id]");
  if (item) selectTask(Number(item.dataset.id));
});

tabButtons.forEach((button) => {
  button.addEventListener("click", () => {
    currentFilter = button.dataset.filter;
    renderAll();
  });
});

/* ===== 초기 실행 ===== */
document.addEventListener("DOMContentLoaded", () => {
  renderAll();
});