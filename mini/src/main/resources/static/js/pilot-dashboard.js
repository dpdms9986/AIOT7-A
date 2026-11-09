// 조종사 메인 화면 (Leaflet 지도 연동 버전)
// 작업 시작·완료 보고·상세는 pilot-history.html 에서 처리

/* ===== 상태 이름 → 화면 글자와 색 ===== */
const STATUS = {
  READY:       { text: "배정 전", cls: "st-ready", color: "#238451" },
  ASSIGNED:    { text: "대기",    cls: "st-assigned", color: "#f2b705" },
  IN_PROGRESS: { text: "진행 중", cls: "st-progress", color: "#2f80ed" },
  COMPLETED:   { text: "완료",    cls: "st-completed", color: "#8a8f8c" },
};

/* ===== 서버 데이터 (임시 데이터 제거 및 API 연동 준비) ===== */
const TASKS_API = "/api/pilot/tasks";             // 내 작업 (lat, lng 포함)
const OTHER_FARMS_API = "/api/pilot/farms/ready"; // 주변 배정 전 농지 (lat, lng 포함)

let myTasks = [];
let OTHER_FARMS = [];

/* ===== 화면 요소 ===== */
const listBox = document.querySelector("#pilot-task-list");
const tabButtons = document.querySelectorAll("#pilot-tabs button");

let currentFilter = "all"; // all / active / done
let selectedId = null;

// Leaflet 지도 위에 그려진 농지 마커들을 관리할 배열
let taskMarkers = [];

// 화면에 넣을 글자의 특수문자를 안전하게 바꿔주는 함수 (XSS 방어)
const esc = (v) => 
  String(v ?? "").replace(/[&<>"']/g, (c) => 
    ({ "&": "&amp;", 
      "<": "&lt;", 
      ">": "&gt;", 
      '"': "&quot;", 
      "'": "&#39;" }
      [c]
    ));

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
    const st = STATUS[t.status] || STATUS.ASSIGNED;
    return `
      <li>
        <button type="button" class="pilot-task ${t.id === selectedId ? "selected" : ""}" data-id="${t.id}" style="width:100%; display:flex; align-items:center; padding:10px; border:1px solid #eee; background:#fff; cursor:pointer; margin-bottom:6px; border-radius:6px;">
          <span class="pilot-dot ${st.cls}" style="width:10px; height:10px; border-radius:50%; background:${st.color}; margin-right:8px;"></span>
          <span class="pilot-task-body" style="flex:1; text-align:left;">
            <strong style="display:block; font-size:14px;">${esc(t.farm)}</strong>
            <small style="color:#666;">${Number(t.area || 0).toLocaleString()}㎡ · ${esc(t.date)}</small>
          </span>
          <span class="pilot-status ${st.cls}" style="font-size:12px; padding:2px 6px; border-radius:4px; background:#eee;">${st.text}</span>
        </button>
      </li>
    `;
  }).join("");
}

/* ===== Leaflet 지도 마커 렌더링 (안전장치 유지 버전) ===== */
function renderMapMarkers() {
  if (typeof map === "undefined") return; // 지도가 아직 없으면 건너뜀

  // 1. 기존에 지도에 그려둔 농지 마커들 싹 지우기 (html 지도 자체는 유지됨)
  taskMarkers.forEach(marker => map.removeLayer(marker));
  taskMarkers = [];

  // 2. 주변 대기 농지(OTHER_FARMS) 마커 찍기 (안전 처리 반영)
  OTHER_FARMS.forEach(f => {
    const fs = STATUS[f.status] || STATUS.READY;
    const marker = L.circleMarker([f.lat, f.lng], {
      radius: 8,
      fillColor: fs.color,
      color: "#fff",
      weight: 2,
      opacity: 1,
      fillOpacity: 0.9
    }).addTo(map);
    
    marker.bindPopup(`<b>🌱 ${esc(f.farm)}</b><br>상태: ${fs.text}`);
    taskMarkers.push(marker);
  });

  // 3. 현재 필터링된 내 작업들만 지도에 새 마커로 찍기
  const tasksToDisplay = filteredTasks();
  tasksToDisplay.forEach(t => {
    const st = STATUS[t.status] || STATUS.ASSIGNED;
    const isSelected = t.id === selectedId;

    const marker = L.circleMarker([t.lat, t.lng], {
      radius: isSelected ? 11 : 9,
      fillColor: st.color,
      color: isSelected ? "#000" : "#fff",
      weight: isSelected ? 3 : 2,
      opacity: 1,
      fillOpacity: 0.9
    }).addTo(map);

    marker.bindPopup(
      `<b>🏠 ${esc(t.farm)}</b><br>상태: ${st.text}<br>면적: ${Number(t.area || 0).toLocaleString()}㎡` +
      `<br><a href="pilot-history.html">작업 이력에서 처리 →</a>`
    );
    
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

function renderAll() {
  renderTabs();
  renderList();
  renderMapMarkers();
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

/* ===== 초기 실행 (서버 데이터 페치 연동) ===== */
document.addEventListener("DOMContentLoaded", async () => {
  try {
    const [taskRes, farmRes] = await Promise.all([
      fetch(TASKS_API),
      fetch(OTHER_FARMS_API),
    ]);
    
    if (!taskRes.ok) throw new Error(`서버 응답 ${taskRes.status}`);
    
    myTasks = await taskRes.json();
    if (farmRes.ok) {
      OTHER_FARMS = await farmRes.json();
    }
  } catch (error) {
    console.error("작업 목록 조회 실패:", error);
    listBox.innerHTML = `<li class="pilot-empty" style="padding: 20px; text-align: center; color: #666;">작업 목록을 불러오지 못했습니다.</li>`;
    renderTabs();
    return;
  }

  const first = myTasks.find((t) => t.status === "IN_PROGRESS") || myTasks[0];
  selectedId = first ? first.id : null;
  renderAll();
});