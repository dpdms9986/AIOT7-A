// 작업 배정 화면 (task-assignment.html) - 기능정의서 ADM_002~005, ADM_016~022
// ⚠️ 농지·조종사 데이터는 화면 확인용 임시 데이터입니다. 백엔드 API 연결 후 교체하세요.

/* ===== 1. 임시 데이터 ===== */
/* const farms = [
  { id: 101, address: "어남동 12-3", region: "대전시 유성구 어남동", area: 800,  status: "READY" },
  { id: 102, address: "어남동 12-5", region: "대전시 유성구 어남동", area: 980,  status: "READY" },
  { id: 103, address: "어남동 14-2", region: "대전시 유성구 어남동", area: 1100, status: "READY" },
  { id: 104, address: "어남동 15-6", region: "대전시 유성구 어남동", area: 1330, status: "READY" },
  { id: 105, address: "어남동 17-1", region: "대전시 유성구 어남동", area: 1870, status: "ASSIGNED" },
  { id: 201, address: "노은동 2-6",  region: "대전시 유성구 노은동", area: 600,  status: "READY" },
  { id: 202, address: "노은동 2-8",  region: "대전시 유성구 노은동", area: 750,  status: "READY" },
  { id: 203, address: "노은동 3-1",  region: "대전시 유성구 노은동", area: 920,  status: "IN_PROGRESS" },
  { id: 301, address: "관저동 9-4",  region: "대전시 서구 관저동",   area: 1050, status: "READY" },
  { id: 302, address: "관저동 10-2", region: "대전시 서구 관저동",   area: 880,  status: "READY" },
  { id: 303, address: "관저동 11-7", region: "대전시 서구 관저동",   area: 1240, status: "COMPLETED" },
]; */

// 소속 조종사 (대기 중인 조종사만 선택 가능 - ADM_017)
/* const PILOTS = [
  { id: "P01", name: "김조종", status: "대기" },
  { id: "P02", name: "박비행", status: "대기" },
  { id: "P03", name: "이파일럿", status: "대기" },
  { id: "P04", name: "최운항", status: "작업 중" },
]; */


let farms = [];    // 미배정 농지 목록 (백엔드 API 연결 후 채움)
let PILOTS = [];   // 배정 가능한 조종사 목록 (백엔드 API 연결 후 채움)

/* ===== 화면 상태 ===== */
let keywords = [];
const selectedFarmIds = new Set();
const checkedPilotIds = new Set();
const quotas = {};

/* ===== 화면 요소 ===== */
const tbody = document.querySelector("#farm-tbody");
const checkAll = document.querySelector("#check-all");
const farmCount = document.querySelector("#farm-count");
const pilotList = document.querySelector("#pilot-list");
const quotaMsg = document.querySelector("#quota-msg");
const regionForm = document.querySelector("#region-form");
const regionInput = document.querySelector("#region-input");
const assignForm = document.querySelector("#assign-form");
const assignModal = document.querySelector("#assign-modal");
const doneModal = document.querySelector("#done-modal");

/* ===== 데이터 고르기 ===== */
function readyFarms() {
  return farms.filter((f) => {
    if (f.status !== "READY") return false;
    const text = `${f.region} ${f.address}`;
    return keywords.every((word) => text.includes(word));
  });
}

function selectedFarms() {
  return farms.filter((f) => selectedFarmIds.has(f.id));
}

function checkedPilots() {
  return PILOTS.filter((p) => checkedPilotIds.has(p.id));
}

function quotaSum() {
  return checkedPilots().reduce((sum, p) => sum + (quotas[p.id] || 0), 0);
}

/* ===== 균등 분배 (ADM_018) - 예) 7필지, 3명 → 3, 2, 2 ===== */
function evenDistribute() {
  const pilots = checkedPilots();
  const total = selectedFarmIds.size;
  Object.keys(quotas).forEach((key) => delete quotas[key]);
  if (pilots.length === 0) return;

  const base = Math.floor(total / pilots.length);
  const rest = total % pilots.length;
  pilots.forEach((p, i) => {
    quotas[p.id] = base + (i < rest ? 1 : 0);
  });
}

/* ===== 그리기 ===== */
function renderTable() {
  const list = readyFarms();
  farmCount.textContent = `${list.length}건`;

  if (list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="task-empty-cell">해당 지역에 미배정 농지가 없습니다.</td></tr>`;
    checkAll.checked = false;
    checkAll.disabled = true;
    return;
  }

  checkAll.disabled = false;
  checkAll.checked = list.every((f) => selectedFarmIds.has(f.id));

  tbody.innerHTML = list.map((f) => {
    const checked = selectedFarmIds.has(f.id);
    return `
      <tr class="${checked ? "selected" : ""}" data-id="${f.id}">
        <td><input type="checkbox" ${checked ? "checked" : ""} aria-label="${f.address} 선택"></td>
        <td>${f.address}</td>
        <td>${f.region}</td>
        <td>${f.area.toLocaleString()}</td>
        <td><span class="pilot-status st-ready">READY</span></td>
      </tr>
    `;
  }).join("");
}

// 선택한 농지 수, 면적 (목록 아래 + 팝업 위)
function renderSelection() {
  const list = selectedFarms();
  const count = list.length;
  const area = list.reduce((sum, f) => sum + f.area, 0).toLocaleString();
  document.querySelector("#sel-count").textContent = count;
  document.querySelector("#sel-area").textContent = area;
  document.querySelector("#modal-count").textContent = count;
  document.querySelector("#modal-area").textContent = area;
}

function renderPilots() {
  pilotList.innerHTML = PILOTS.map((p) => {
    const busy = p.status !== "대기";
    const checked = checkedPilotIds.has(p.id);
    return `
      <li class="task-pilot ${busy ? "busy" : ""} ${checked ? "checked" : ""}">
        <label>
          <input type="checkbox" class="task-pilot-check" value="${p.id}" ${checked ? "checked" : ""} ${busy ? "disabled" : ""}>
          <span>${p.name}</span>
          <small>${p.status}</small>
        </label>
        <span class="task-quota-box">
          <input type="number" class="task-quota-input" data-id="${p.id}" min="0"
                 value="${checked ? quotas[p.id] || 0 : 0}" ${checked ? "" : "disabled"}> 필지
        </span>
      </li>
    `;
  }).join("");
}

// 합계 검증 (ADM_020)
function renderQuotaMsg() {
  const total = selectedFarmIds.size;
  const sum = quotaSum();

  if (checkedPilotIds.size === 0) {
    quotaMsg.className = "task-quota";
    quotaMsg.textContent = "조종사를 선택하면 자동으로 균등 분배됩니다.";
  } else if (sum === total) {
    quotaMsg.className = "task-quota ok";
    quotaMsg.textContent = `배정 합계 ${sum} / ${total}필지 ✔`;
  } else {
    quotaMsg.className = "task-quota warn";
    quotaMsg.textContent = `배정 합계 ${sum} / ${total}필지 — 선택한 농지 수와 맞지 않습니다.`;
  }
}

function renderAll() {
  renderTable();
  renderSelection();
  renderPilots();
  renderQuotaMsg();
}

/* ===== 농지 선택 ===== */
function toggleFarm(id) {
  if (selectedFarmIds.has(id)) selectedFarmIds.delete(id);
  else selectedFarmIds.add(id);
  evenDistribute();
  renderAll();
}

tbody.addEventListener("click", (event) => {
  const row = event.target.closest("tr[data-id]");
  if (row) toggleFarm(Number(row.dataset.id));
});

checkAll.addEventListener("change", () => {
  readyFarms().forEach((f) => {
    if (checkAll.checked) selectedFarmIds.add(f.id);
    else selectedFarmIds.delete(f.id);
  });
  evenDistribute();
  renderAll();
});

/* ===== 행정구역 검색 (ADM_002~003) ===== */
function clearSelection() {
  selectedFarmIds.clear();
  evenDistribute();
}

regionForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const value = regionInput.value.trim();

  if (value.length < 2) {
    alert("행정구역을 2글자 이상 입력하세요. (예: 유성구, 어남동)");
    return;
  }

  keywords = value.split(/\s+/);
  clearSelection();
  renderAll();

  if (readyFarms().length === 0) {
    alert("해당 지역에 미배정(READY) 농지가 없습니다.");
  }
});

document.querySelector("#region-reset").addEventListener("click", () => {
  keywords = [];
  regionInput.value = "";
  clearSelection();
  renderAll();
});

/* ===== 분배 설정 팝업 (ADM_016~020) ===== */
document.querySelector("#open-assign").addEventListener("click", () => {
  if (selectedFarmIds.size === 0) {
    alert("배정할 농지를 먼저 선택하세요.");
    return;
  }
  evenDistribute();
  renderAll();
  assignModal.hidden = false;
});

pilotList.addEventListener("change", (event) => {
  if (!event.target.classList.contains("task-pilot-check")) return;

  const id = event.target.value;
  if (event.target.checked) checkedPilotIds.add(id);
  else checkedPilotIds.delete(id);

  evenDistribute();
  renderPilots();
  renderQuotaMsg();
});

// 배정 수 직접 수정 (목록을 다시 그리지 않아야 입력 중 커서가 안 사라짐)
pilotList.addEventListener("input", (event) => {
  if (!event.target.classList.contains("task-quota-input")) return;
  quotas[event.target.dataset.id] = Math.max(0, Number(event.target.value) || 0);
  renderQuotaMsg();
});

document.querySelector("#even-btn").addEventListener("click", () => {
  evenDistribute();
  renderPilots();
  renderQuotaMsg();
});

/* ===== 배정 확정 (ADM_021~022) ===== */
assignForm.addEventListener("submit", (event) => {
  event.preventDefault();

  const pickedFarms = selectedFarms();
  const pilots = checkedPilots();

  if (pilots.length === 0) {
    alert("투입할 조종사를 선택하세요.");
    return;
  }
  if (quotaSum() !== pickedFarms.length) {
    alert(`조종사별 배정 수 합계(${quotaSum()})가 선택한 농지 수(${pickedFarms.length})와 다릅니다.`);
    return;
  }

  // 농지 ID - 조종사 ID 짝 배열 (ADM_021)
  const assignments = [];
  let index = 0;
  pilots.forEach((p) => {
    for (let i = 0; i < (quotas[p.id] || 0); i++) {
      assignments.push({ landId: pickedFarms[index].id, pilotId: p.id });
      index++;
    }
  });

  console.log("배정 요청 데이터:", assignments);

  // 나중에 백엔드 연결: POST /api/admin/assign-team
  // const res = await fetch("/api/admin/assign-team", {
  //   method: "POST",
  //   headers: { "Content-Type": "application/json" },
  //   body: JSON.stringify(assignments),
  // });
  // if (!res.ok) { alert("배정에 실패했습니다."); return; }
  // ※ await를 쓰려면 위의 (event) => 를 async (event) => 로 바꿔야 합니다.

  // READY → ASSIGNED (ADM_022)
  pickedFarms.forEach((f) => (f.status = "ASSIGNED"));

  document.querySelector("#done-desc").textContent =
    `${pickedFarms.length}필지가 ${pilots.length}명의 조종사에게 정상적으로 배정되었습니다.`;

  selectedFarmIds.clear();
  checkedPilotIds.clear();
  evenDistribute();
  renderAll();

  assignModal.hidden = true;
  doneModal.hidden = false;
});

/* ===== 팝업 닫기 (×, 취소, 확인, 바깥 클릭, Esc) ===== */
document.querySelectorAll(".task-modal").forEach((modal) => {
  modal.addEventListener("click", (event) => {
    if (event.target === modal || event.target.closest("[data-close]")) {
      modal.hidden = true;
    }
  });
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    document.querySelectorAll(".task-modal").forEach((modal) => (modal.hidden = true));
  }
});

/* ===== 위쪽 탭 전환 ===== */
const tabs = document.querySelectorAll(".task-tab");
const panels = document.querySelectorAll(".task-panel");

tabs.forEach((tab) => {
  tab.addEventListener("click", () => {
    tabs.forEach((t) => t.classList.toggle("active", t === tab));
    panels.forEach((panel) => {
      panel.hidden = panel.dataset.panel !== tab.dataset.tab;
    });
  });
});

/* ===== 시작 ===== */
renderAll();