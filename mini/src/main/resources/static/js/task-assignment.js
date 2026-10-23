// 작업 배정 화면 (task-assignment.html) 전용
// ⚠️ 지금 데이터는 화면 확인용 임시 데이터입니다. 백엔드 API 연결 후 교체하세요.

/* ===== 1. 임시 데이터 ===== */
const TASK_TYPES = [
  { value: "spray",  icon: "🚁", name: "방제 작업",   count: 5 },
  { value: "growth", icon: "📷", name: "생육 촬영",   count: 3 },
  { value: "pest",   icon: "🐛", name: "병해충 점검", count: 2 },
  { value: "soil",   icon: "🌱", name: "토양 분석",   count: 1 },
  { value: "etc",    icon: "⋯",  name: "기타 작업",   count: 1 },
];

const FARMS = [
  { value: "F01", label: "농지 01 (벼)" },
  { value: "F02", label: "농지 02 (콩)" },
  { value: "F03", label: "농지 03 (고추)" },
  { value: "F04", label: "농지 04 (벼)" },
  { value: "F05", label: "농지 05 (벼)" },
  { value: "F07", label: "농지 07 (배추)" },
];

const DRONES = [
  { value: "DR-001", label: "DR-001 (촬영 드론)" },
  { value: "DR-002", label: "DR-002 (촬영 드론)" },
  { value: "DR-003", label: "DR-003 (방제 드론)" },
  { value: "DR-004", label: "DR-004 (방제 드론)" },
];

const CHEMICALS = [
  { value: "A", label: "약제 A (농약)" },
  { value: "B", label: "약제 B (살균제)" },
  { value: "C", label: "약제 C (살충제)" },
];

const PILOTS = [
  { value: "P01", label: "김정훈 (드론 조종사)" },
  { value: "P02", label: "박민수 (드론 조종사)" },
  { value: "P03", label: "이서연 (드론 조종사)" },
];

/* ===== 2. 화면 요소 가져오기 ===== */
const form = document.querySelector("#task-form");
const typeSelect = document.querySelector("#task-type");
const farmSelect = document.querySelector("#task-farm");
const droneSelect = document.querySelector("#task-drone");
const chemSelect = document.querySelector("#task-chem");
const pilotSelect = document.querySelector("#task-pilot");
const amountInput = document.querySelector("#task-amount");
const durationInput = document.querySelector("#task-duration");
const dateInput = document.querySelector("#task-date");
const timeInput = document.querySelector("#task-time");
const zoneAll = document.querySelector("#zone-all");
const zoneBoxes = document.querySelectorAll(".zone-box");
const typeList = document.querySelector("#task-type-list");
const modal = document.querySelector("#task-modal");

/* ===== 3. 선택 상자(select) 채우기 ===== */
function fillSelect(select, items, placeholder) {
  select.innerHTML =
    `<option value="" disabled>${placeholder}</option>` +
    items.map((item) => `<option value="${item.value}">${item.label}</option>`).join("");
}

fillSelect(typeSelect, TASK_TYPES.map((t) => ({ value: t.value, label: t.name })), "작업 유형 선택");
fillSelect(farmSelect, FARMS, "농지 선택");
fillSelect(droneSelect, DRONES, "드론 선택");
fillSelect(chemSelect, CHEMICALS, "약제 선택");
fillSelect(pilotSelect, PILOTS, "담당자 선택");

// 처음 선택되어 있을 값 (피그마 화면과 같게)
typeSelect.value = "spray";
farmSelect.value = "F05";
droneSelect.value = "DR-003";
chemSelect.value = "A";
pilotSelect.value = "P01";

// 오늘 날짜를 "2026-10-22" 형식으로
function todayString() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
dateInput.value = todayString();
document.querySelector("#task-top-date").value = todayString();

/* ===== 4. 왼쪽 작업 유형 목록 ===== */
const totalCount = TASK_TYPES.reduce((sum, t) => sum + t.count, 0);
const typeItems = [{ value: "", icon: "🗂️", name: "전체", count: totalCount }, ...TASK_TYPES];

typeList.innerHTML = typeItems.map((t) => `
  <li>
    <button type="button" data-type="${t.value}">
      <span>${t.icon}</span>
      <span class="task-type-name">${t.name}</span>
      <span class="task-type-count">${t.count}</span>
    </button>
  </li>
`).join("");

function setActiveType(value) {
  typeList.querySelectorAll("button").forEach((button) => {
    button.classList.toggle("active", button.dataset.type === value);
  });
}
setActiveType(""); // 처음엔 "전체"가 선택된 모양

// 왼쪽 목록을 누르면 → 오른쪽 "작업 유형"도 같이 바뀜
typeList.addEventListener("click", (event) => {
  const button = event.target.closest("button");
  if (!button) return;
  setActiveType(button.dataset.type);
  typeSelect.value = button.dataset.type; // "전체"면 빈 값 → "작업 유형 선택"
  updateForm();
});

/* ===== 5. 작업 구역 체크박스 ===== */
// "전체 구역"을 체크하면 → A~D는 해제
zoneAll.addEventListener("change", () => {
  const anyZone = [...zoneBoxes].some((box) => box.checked);
  if (zoneAll.checked) {
    zoneBoxes.forEach((box) => (box.checked = false));
  } else if (!anyZone) {
    zoneAll.checked = true; // 아무 구역도 없으면 안 되니까 다시 체크
  }
});

// A~D 중 하나라도 체크하면 → "전체 구역"은 해제, 다 풀면 → "전체 구역" 다시 체크
zoneBoxes.forEach((box) => {
  box.addEventListener("change", () => {
    const anyZone = [...zoneBoxes].some((b) => b.checked);
    zoneAll.checked = !anyZone;
  });
});

function getZones() {
  if (zoneAll.checked) return ["A", "B", "C", "D"];
  return [...zoneBoxes].filter((box) => box.checked).map((box) => box.value);
}

/* ===== 6. 요약 + 예상 소요 시간 자동 계산 ===== */
function selectedText(select) {
  const option = select.selectedOptions[0];
  return option && option.value ? option.textContent : "-";
}
// "DR-003 (방제 드론)" → "DR-003"
function shortText(text) {
  return text.split(" (")[0];
}

function updateForm() {
  // 방제 작업일 때만 약제·살포량 입력 가능
  const isSpray = typeSelect.value === "spray";
  chemSelect.disabled = !isSpray;
  amountInput.disabled = !isSpray;

  // ⚠️ 임시 계산식: 준비 5분 + 구역 1개당 10분 (실제 계산식은 팀에서 정하기)
  const minutes = 5 + getZones().length * 10;
  durationInput.value = `약 ${minutes}분`;

  document.querySelector("#sum-farm").textContent = selectedText(farmSelect);
  document.querySelector("#sum-type").textContent = selectedText(typeSelect);
  document.querySelector("#sum-drone").textContent = shortText(selectedText(droneSelect));
  document.querySelector("#sum-chem").textContent = isSpray ? shortText(selectedText(chemSelect)) : "해당 없음";
  document.querySelector("#sum-time").textContent = durationInput.value;
}

// 폼 안에서 무엇이든 바뀌면 요약 다시 계산
form.addEventListener("change", (event) => {
  if (event.target === typeSelect) setActiveType(typeSelect.value);
  updateForm();
});
form.addEventListener("input", updateForm);

updateForm(); // 처음 한 번 실행

/* ===== 7. 작업 배정하기 버튼 ===== */
form.addEventListener("submit", (event) => {
  event.preventDefault();

  const data = {
    taskType: typeSelect.value,
    farmId: farmSelect.value,
    zones: getZones(),
    startDate: dateInput.value,
    startTime: timeInput.value,
    droneId: droneSelect.value,
    chemical: typeSelect.value === "spray" ? chemSelect.value : null,
    amount: typeSelect.value === "spray" ? Number(amountInput.value) : null,
    pilotId: pilotSelect.value,
  };

  console.log("배정 요청 데이터:", data);

  // 나중에 백엔드 연결할 때 (주소는 백엔드에 맞게 변경):
  // await fetch("/api/tasks", {
  //   method: "POST",
  //   headers: { "Content-Type": "application/json" },
  //   body: JSON.stringify(data),
  // });

  openModal();
});

/* ===== 8. 완료 팝업 ===== */
function openModal() {
  modal.hidden = false;
  document.querySelector("#task-modal-ok").focus();
}
function closeModal() {
  modal.hidden = true;
}

document.querySelector("#task-modal-ok").addEventListener("click", closeModal);
document.querySelector("#task-modal-close").addEventListener("click", closeModal);
modal.addEventListener("click", (event) => {
  if (event.target === modal) closeModal(); // 팝업 바깥 어두운 부분 클릭
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && !modal.hidden) closeModal();
});

/* ===== 9. 위쪽 탭 전환 ===== */
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