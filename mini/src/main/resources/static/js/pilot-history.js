// 조종사 작업 이력 화면 (pilot-history.html)
// 대기(작업 시작) / 진행 중(완료 보고) / 완료(결과)

const PH_API = "/api/pilot/tasks"; // 목록 GET, 시작 POST /{id}/start, 완료 POST /{id}/complete

const PH_STATUS = {
  ASSIGNED:    { text: "대기",    cls: "st-assigned" },
  IN_PROGRESS: { text: "진행 중", cls: "st-progress" },
  COMPLETED:   { text: "완료",    cls: "st-completed" },
};

let PH_DRONES = [];     // 서버에서 받아옴: [{ id, label }]
let PH_CHEMICALS = [];

async function loadOptions() {
  const [dr, ch] = await Promise.all([
    fetch("/api/pilot/drones"),      // 주소는 팀원과 맞춰서 수정
    fetch("/api/pilot/chemicals"),
  ]);
  if (dr.ok) PH_DRONES = await dr.json();
  if (ch.ok) PH_CHEMICALS = await ch.json();
}



const listBox = document.querySelector("#ph-list");
const detailBox = document.querySelector("#ph-detail");
const countEl = document.querySelector("#ph-count");
const sumEl = document.querySelector("#ph-sum");
const tabButtons = document.querySelectorAll("#ph-tabs button");

let tasks = [];
let selectedId = null;
let currentFilter = "all"; // all / active / done

const esc = (v) =>
  String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const labelOf = (list, id) => (list.find((item) => item.id === id) || {}).label || "-";
const pad = (n) => String(n).padStart(2, "0");
const nowText = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

async function postTo(path, body) {
  try {
    const res = await fetch(`${PH_API}/${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      alert("처리에 실패했습니다. 이미 다른 조종사가 시작했을 수 있습니다.");
      return false;
    }
    return true;
  } catch (error) {
    console.error("서버 요청 에러:", error);
    alert("서버와 연결할 수 없습니다.");
    return false;
  }
}

async function loadTasks() {
  const res = await fetch(PH_API);
  if (!res.ok) throw new Error(`서버 응답 ${res.status}`);
  return res.json();
}

/* ===== 그리기 ===== */
function filteredTasks() {
  if (currentFilter === "active") return tasks.filter((t) => t.status !== "COMPLETED");
  if (currentFilter === "done") return tasks.filter((t) => t.status === "COMPLETED");
  return tasks;
}

function renderTabs() {
  const counts = {
    all: tasks.length,
    active: tasks.filter((t) => t.status !== "COMPLETED").length,
    done: tasks.filter((t) => t.status === "COMPLETED").length,
  };
  const names = { all: "전체", active: "진행 중", done: "완료" };

  tabButtons.forEach((button) => {
    const key = button.dataset.filter;
    button.textContent = `${names[key]} (${counts[key]})`;
    button.classList.toggle("active", key === currentFilter);
  });
}

function renderList() {
  const list = filteredTasks();
  countEl.textContent = `${tasks.length}건`;

  const done = tasks.filter((t) => t.status === "COMPLETED");
  const totalArea = done.reduce((s, t) => s + Number(t.area || 0), 0);
  const totalUsed = done.reduce((s, t) => s + Number(t.used || 0), 0);
  sumEl.textContent = done.length
    ? `완료 ${done.length}건 · 면적 ${totalArea.toLocaleString()}㎡ · 약제 사용량 ${totalUsed.toFixed(1)}L`
    : "";

  if (!list.length) {
    listBox.innerHTML = '<li class="pilot-empty">해당하는 작업이 없습니다.</li>';
    return;
  }

  listBox.innerHTML = list
    .map((t) => {
      const st = PH_STATUS[t.status];
      return `
      <li>
        <button type="button" class="pilot-task ${t.id === selectedId ? "selected" : ""}" data-id="${t.id}">
          <span class="pilot-dot ${st.cls}"></span>
          <span class="pilot-task-body">
            <strong>${esc(t.farm)}</strong>
            <small>${Number(t.area).toLocaleString()}㎡ · ${esc(t.date)}</small>
          </span>
          <span class="pilot-status ${st.cls}">${st.text}</span>
        </button>
      </li>`;
    })
    .join("");
}

// 상세 박스: 상태에 따라 다른 입력칸과 버튼
function renderDetail() {
  const t = tasks.find((task) => task.id === selectedId);

  if (!t) {
    detailBox.innerHTML = '<p class="pilot-empty">목록에서 작업을 선택하세요.</p>';
    return;
  }

  const st = PH_STATUS[t.status];
  let info = `
    <dt>작업 일시</dt><dd>${esc(t.date)}</dd>
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
            ${PH_DRONES.map((d) => `<option value="${d.id}">${d.label}</option>`).join("")}
          </select>
        </label>
        <label class="task-field">
          <span>약제</span>
          <select id="sel-chem">
            <option value="">약제 선택</option>
            ${PH_CHEMICALS.map((c) => `<option value="${c.id}">${c.label}</option>`).join("")}
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
      <dt>드론</dt><dd>${esc(labelOf(PH_DRONES, t.drone))}</dd>
      <dt>약제</dt><dd>${esc(labelOf(PH_CHEMICALS, t.chemical))}</dd>
      <dt>살포량</dt><dd>${t.amount} L/ha</dd>
      <dt>완료 일시</dt><dd>${esc(t.completedAt)}</dd>
      <dt>실제 사용량</dt><dd>${t.used} L</dd>
    `;
  }

  detailBox.innerHTML = `
    <h3>${esc(t.farm)}</h3>
    <dl>${info}</dl>
    ${form}
  `;
}

function renderAll() {
  renderTabs();
  renderList();
  renderDetail();
}

/* ===== 클릭 동작 ===== */
listBox.addEventListener("click", (event) => {
  const item = event.target.closest("[data-id]");
  if (!item) return;
  selectedId = Number(item.dataset.id);
  renderAll();
});

tabButtons.forEach((button) => {
  button.addEventListener("click", () => {
    currentFilter = button.dataset.filter;
    renderAll();
  });
});

// 작업 시작 / 작업 완료 보고
detailBox.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-action]");
  if (!button) return;

  const t = tasks.find((task) => task.id === selectedId);

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
    if (!(await postTo(`${t.id}/start`, { drone, chemical, amount }))) return;

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
    if (!(await postTo(`${t.id}/complete`, { used }))) return;

    t.used = used;
    t.status = "COMPLETED";
    t.completedAt = nowText();
  }

  renderAll();
});

/* ===== 시작 ===== */
(async () => {
  try {
    tasks = await loadTasks();
    await loadOptions();
  } catch (error) {
    console.error("작업 목록 조회 실패:", error);
    listBox.innerHTML = '<li class="pilot-empty">작업 목록을 불러오지 못했습니다.</li>';
    countEl.textContent = "-";
    return;
  }
  const first = tasks.find((t) => t.status === "IN_PROGRESS") || tasks[0];
  selectedId = first ? first.id : null;
  renderAll();
})();