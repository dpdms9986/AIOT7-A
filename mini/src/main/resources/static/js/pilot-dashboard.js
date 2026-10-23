// 조종사 메인 화면 
// ⚠️ 작업·드론·약제 데이터는 화면 확인용 데이터 , 백엔드 API 연결 후 교체

/* ===== 상태 이름 → 화면 글자와 색 ===== */
const STATUS = {
  READY:       { text: "배정 전", cls: "st-ready" },
  ASSIGNED:    { text: "대기",    cls: "st-assigned" },
  IN_PROGRESS: { text: "진행 중", cls: "st-progress" },
  COMPLETED:   { text: "완료",    cls: "st-completed" },
};

/* ===== 임시 데이터 ===== */
// 사용 가능한 방제 드론
const DRONES = [
  { id: "DR-003", label: "DR-003 · DJI Agras T40" },
  { id: "DR-004", label: "DR-004 · DJI Agras T30" },
];

// 약제 (재고 표시)
const CHEMICALS = [
  { id: "CH-01", label: "약제 A (살충제) · 재고 84L" },
  { id: "CH-02", label: "약제 B (살균제) · 재고 40L" },
  { id: "CH-03", label: "약제 C (영양제) · 재고 25L" },
];

// 내 작업 (x, y = 임시 지도 위치 %)
const myTasks = [
  { id: 1, farm: "어남동 12-3", area: 800,  date: "2026-10-23 09:00", status: "IN_PROGRESS", x: 40, y: 58,
    drone: "DR-003", chemical: "CH-01", amount: 12, used: null },
  { id: 2, farm: "노은동 2-6",  area: 600,  date: "2026-10-23 13:00", status: "ASSIGNED",    x: 26, y: 38,
    drone: null, chemical: null, amount: null, used: null },
  { id: 3, farm: "어남동 14-2", area: 1100, date: "2026-10-24 08:30", status: "ASSIGNED",    x: 58, y: 26,
    drone: null, chemical: null, amount: null, used: null },
  { id: 4, farm: "관저동 9-4",  area: 1050, date: "2026-10-22 10:00", status: "COMPLETED",   x: 70, y: 60,
    drone: "DR-004", chemical: "CH-02", amount: 10, used: 1.1 },
];

// 주변의 배정 전 농지 (지도에만 표시)
const OTHER_FARMS = [
  { farm: "노은동 2-8", status: "READY", x: 82, y: 32 },
  { farm: "관저동 10-2", status: "READY", x: 18, y: 76 },
];

/* ===== 화면 요소 ===== */
const mapBox = document.querySelector("#pilot-map");
const listBox = document.querySelector("#pilot-task-list");
const detailBox = document.querySelector("#pilot-detail");
const tabButtons = document.querySelectorAll("#pilot-tabs button");

let currentFilter = "all"; // all / active / done
let selectedId = (myTasks.find((t) => t.status === "IN_PROGRESS") || myTasks[0]).id;

const labelOf = (list, id) => (list.find((item) => item.id === id) || {}).label || "-";


/* ===== 그리기 ===== */
function filteredTasks() {
  if (currentFilter === "active") {
    return myTasks.filter((t) => t.status === "ASSIGNED" || t.status === "IN_PROGRESS");
  }
  if (currentFilter === "done") {
    return myTasks.filter((t) => t.status === "COMPLETED");
  }
  return myTasks;
}

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

function renderList() {
  const tasks = filteredTasks();

  if (tasks.length === 0) {
    listBox.innerHTML = `<li class="pilot-empty">해당하는 작업이 없습니다.</li>`;
    return;
  }

  listBox.innerHTML = tasks.map((t) => {
    const st = STATUS[t.status];
    return `
      <li>
        <button type="button" class="pilot-task ${t.id === selectedId ? "selected" : ""}" data-id="${t.id}">
          <span class="pilot-dot ${st.cls}"></span>
          <span class="pilot-task-body">
            <strong>${t.farm}</strong>
            <small>${t.area.toLocaleString()}㎡ · ${t.date}</small>
          </span>
          <span class="pilot-status ${st.cls}">${st.text}</span>
        </button>
      </li>
    `;
  }).join("");
}

function renderMap() {
  const otherPins = OTHER_FARMS.map((f) => `
    <span class="pilot-pin ${STATUS[f.status].cls}" style="left:${f.x}%; top:${f.y}%" title="${f.farm} (배정 전)"></span>
  `).join("");

  const myPins = myTasks.map((t) => `
    <button type="button" class="pilot-pin ${STATUS[t.status].cls} ${t.id === selectedId ? "selected" : ""}"
            style="left:${t.x}%; top:${t.y}%" data-id="${t.id}" aria-label="${t.farm}"></button>
  `).join("");

  const selected = myTasks.find((t) => t.id === selectedId);
  const label = selected
    ? `<div class="pilot-pin-label" style="left:${selected.x}%; top:${selected.y}%">${selected.farm}</div>`
    : "";

  mapBox.innerHTML = otherPins + myPins + label;
}

// 작업 상세: 상태에 따라 다른 입력칸과 버튼
function renderDetail() {
  const t = myTasks.find((task) => task.id === selectedId);

  if (!t) {
    detailBox.innerHTML = `<p class="pilot-empty">목록이나 지도에서 작업을 선택하세요.</p>`;
    return;
  }

  const st = STATUS[t.status];
  let info = `
    <dt>면적</dt><dd>${t.area.toLocaleString()}㎡</dd>
    <dt>작업 일시</dt><dd>${t.date}</dd>
    <dt>상태</dt><dd><span class="pilot-status ${st.cls}">${st.text}</span></dd>
  `;
  let form = "";

  if (t.status === "ASSIGNED") {
    // 대기 → 드론·약제 선택 후 작업 시작
    form = `
      <div class="pilot-form">
        <p class="pilot-form-title">드론 및 약제 선택</p>
        <label class="task-field">
          <span>드론</span>
          <select id="sel-drone">
            <option value="">드론 선택</option>
            ${DRONES.map((d) => `<option value="${d.id}">${d.label}</option>`).join("")}
          </select>
        </label>
        <label class="task-field">
          <span>약제</span>
          <select id="sel-chem">
            <option value="">약제 선택</option>
            ${CHEMICALS.map((c) => `<option value="${c.id}">${c.label}</option>`).join("")}
          </select>
        </label>
        <label class="task-field">
          <span>살포량 (L/ha)</span>
          <input type="number" id="sel-amount" value="12" min="1" max="50">
        </label>
      </div>
      <button type="button" class="dash-btn-primary" data-action="start">작업 시작</button>
    `;
  } else if (t.status === "IN_PROGRESS") {
    // 진행 중 → 실제 사용량 입력 후 완료 보고 
    info += `
      <dt>드론</dt><dd>${labelOf(DRONES, t.drone)}</dd>
      <dt>약제</dt><dd>${labelOf(CHEMICALS, t.chemical)}</dd>
      <dt>살포량</dt><dd>${t.amount} L/ha</dd>
    `;
    form = `
      <div class="pilot-form">
        <p class="pilot-form-title">방제 완료 보고</p>
        <label class="task-field">
          <span>실제 약제 사용량 (L)</span>
          <input type="number" id="used-amount" min="0" step="0.1" placeholder="예: 1.2">
        </label>
      </div>
      <button type="button" class="dash-btn-primary" data-action="finish">작업 완료 보고</button>
    `;
  } else if (t.status === "COMPLETED") {
    info += `
      <dt>드론</dt><dd>${labelOf(DRONES, t.drone)}</dd>
      <dt>약제</dt><dd>${labelOf(CHEMICALS, t.chemical)}</dd>
      <dt>사용량</dt><dd>${t.used} L</dd>
    `;
  }

  detailBox.innerHTML = `
    <h3>${t.farm}</h3>
    <dl>${info}</dl>
    ${form}
  `;
}

function renderAll() {
  renderTabs();
  renderList();
  renderMap();
  renderDetail();
}

/* ===== 클릭 동작 ===== */
function selectTask(id) {
  selectedId = id;
  renderAll();
}

listBox.addEventListener("click", (event) => {
  const item = event.target.closest("[data-id]");
  if (item) selectTask(Number(item.dataset.id));
});

mapBox.addEventListener("click", (event) => {
  const pin = event.target.closest("[data-id]");
  if (pin) selectTask(Number(pin.dataset.id));
});

tabButtons.forEach((button) => {
  button.addEventListener("click", () => {
    currentFilter = button.dataset.filter;
    renderAll();
  });
});

// 작업 시작 / 작업 완료 보고
detailBox.addEventListener("click", (event) => {
  const button = event.target.closest("[data-action]");
  if (!button) return;

  const t = myTasks.find((task) => task.id === selectedId);

  if (button.dataset.action === "start") {
    const drone = document.querySelector("#sel-drone").value;
    const chemical = document.querySelector("#sel-chem").value;
    const amount = Number(document.querySelector("#sel-amount").value);

    if (!drone || !chemical) {
      alert("드론과 약제를 선택하세요.");
      return;
    }
    if (!amount || amount <= 0) {
      alert("살포량을 입력하세요.");
      return;
    }

    // 나중에 백엔드 연결
    // 다른 조종사가 먼저 시작했으면 서버가 에러
    // const res = await fetch(`/api/pilot/tasks/${t.id}/start`, { method: "POST", headers: {...}, body: JSON.stringify({ drone, chemical, amount }) });

    t.drone = drone;
    t.chemical = chemical;
    t.amount = amount;
    t.status = "IN_PROGRESS";
  }

  if (button.dataset.action === "finish") {
    const used = Number(document.querySelector("#used-amount").value);

    if (!used || used <= 0) {
      alert("실제 약제 사용량을 입력하세요.");
      return;
    }

    // 나중에 백엔드 연결: 완료 보고 + 약제 재고 차감
    // await fetch(`/api/pilot/tasks/${t.id}/complete`, { method: "POST", headers: {...}, body: JSON.stringify({ used }) });

    t.used = used;
    t.status = "COMPLETED";
  }

  renderAll();
});


/* ===== 시작 ===== */
renderAll();