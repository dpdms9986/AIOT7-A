// 작업 배정 (배정 탭)
// - 목록 / 검색 / 저장 / 현황 : 서버 (타임리프 + 컨트롤러)
// - JS : 선택 합계, 전체 선택, 균등 분배, 합계 검사, 팝업
document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector("#assign-form");
  if (!form) return;   // 현황 탭에서는 배정 폼이 없으므로 아무것도 안 함

  const $ = (sel) => document.querySelector(sel);
  const landChecks = () => [...form.querySelectorAll(".land-check")];
  const pilotChecks = () => [...form.querySelectorAll(".task-pilot-check")];
  const modal = $("#assign-modal");
  const quotaMsg = $("#quota-msg");
  const checkAll = $("#check-all");

  /* ===== 1. 선택한 농지 수 / 면적 ===== */
  const selectedLands = () => landChecks().filter((c) => c.checked);

  const renderSelection = () => {
    const list = selectedLands();
    const count = list.length;
    const area = list
      .reduce((sum, c) => sum + Number(c.dataset.area || 0), 0)
      .toLocaleString();

    $("#sel-count").textContent = count;
    $("#sel-area").textContent = area;
    $("#modal-count").textContent = count;
    $("#modal-area").textContent = area;

    // 선택된 줄 색칠 + 전체 선택 체크박스 상태
    landChecks().forEach((c) => c.closest("tr").classList.toggle("selected", c.checked));
    const all = landChecks();
    checkAll.disabled = all.length === 0;
    checkAll.checked = all.length > 0 && all.every((c) => c.checked);
  };

  landChecks().forEach((c) => c.addEventListener("change", () => {
    renderSelection();
    evenDistribute();
  }));

  checkAll.addEventListener("change", () => {
    landChecks().forEach((c) => (c.checked = checkAll.checked));
    renderSelection();
    evenDistribute();
  });

  /* ===== 2. 조종사 선택 → 숫자칸 켜기/끄기 ===== */
  const quotaInputOf = (pilotCheck) =>
    pilotCheck.closest(".task-pilot").querySelector(".task-quota-input");

  const checkedPilots = () => pilotChecks().filter((c) => c.checked);

  pilotChecks().forEach((c) => c.addEventListener("change", () => {
    const input = quotaInputOf(c);
    input.disabled = !c.checked;          // disabled 면 서버로 안 보내짐
    if (!c.checked) input.value = 0;
    c.closest(".task-pilot").classList.toggle("checked", c.checked);
    evenDistribute();
  }));

  /* ===== 3. 균등 분배  예) 7필지, 3명 → 3, 2, 2 ===== */
  const evenDistribute = () => {
    const pilots = checkedPilots();
    const total = selectedLands().length;
    if (pilots.length > 0) {
      const base = Math.floor(total / pilots.length);
      const rest = total % pilots.length;
      pilots.forEach((p, i) => {
        quotaInputOf(p).value = base + (i < rest ? 1 : 0);
      });
    }
    renderQuotaMsg();
  };

  $("#even-btn").addEventListener("click", evenDistribute);

  /* ===== 4. 합계 검사 ===== */
  const quotaSum = () =>
    checkedPilots().reduce((sum, p) => sum + Math.max(0, Number(quotaInputOf(p).value) || 0), 0);

  const renderQuotaMsg = () => {
    const total = selectedLands().length;
    const sum = quotaSum();
    if (checkedPilots().length === 0) {
      quotaMsg.className = "task-quota";
      quotaMsg.textContent = "조종사를 선택하면 자동으로 균등 분배됩니다.";
    } else if (sum === total) {
      quotaMsg.className = "task-quota ok";
      quotaMsg.textContent = `배정 합계 ${sum} / ${total}필지 ✔`;
    } else {
      quotaMsg.className = "task-quota warn";
      quotaMsg.textContent = `배정 합계 ${sum} / ${total}필지 — 선택한 농지 수와 맞지 않습니다.`;
    }
  };

  // 숫자를 직접 고칠 때마다 합계 다시 검사
  form.addEventListener("input", (e) => {
    if (e.target.classList.contains("task-quota-input")) renderQuotaMsg();
  });

  /* ===== 5. 팝업 열기 / 닫기 ===== */
  $("#open-assign").addEventListener("click", () => {
    if (selectedLands().length === 0) {
      alert("배정할 농지를 먼저 선택하세요.");
      return;
    }
    evenDistribute();
    modal.hidden = false;
  });

  modal.addEventListener("click", (e) => {
    if (e.target === modal || e.target.closest("[data-close]")) modal.hidden = true;
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") modal.hidden = true;
  });

  /* ===== 6. 배정 확정 전 검사 → 통과하면 브라우저가 폼 전송 ===== */
  form.addEventListener("submit", (e) => {
    const total = selectedLands().length;
    if (checkedPilots().length === 0) {
      e.preventDefault();
      alert("투입할 조종사를 선택하세요.");
      return;
    }
    if (quotaSum() !== total) {
      e.preventDefault();
      alert(`조종사별 배정 수 합계(${quotaSum()})가 선택한 농지 수(${total})와 다릅니다.`);
      return;
    }
    // 통과: 아무것도 안 함 → landNos, quotas[번호] 가 POST 로 전송
  });

  /* ===== 처음 열렸을 때 (농지 관리에서 ?landNo= 로 넘어온 경우 미리 체크돼 있음) ===== */
  renderSelection();
});