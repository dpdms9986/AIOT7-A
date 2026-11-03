
// 드론·약제 관리 - 등록 팝업 열기/닫기 및 상태 선택지 전환
document.addEventListener("DOMContentLoaded", () => {
    const modal = document.querySelector("#asset-register-modal");
    const openBtn = document.querySelector("#asset-register-btn");

    if (!modal || !openBtn) return;

// 모달 열기
const open = () => {
    modal.hidden = false;
    document.body.style.overflow = "hidden";
};

// 모달 닫기
const close = () => {
    modal.hidden = true;
    document.body.style.overflow = "";
};

  openBtn.addEventListener("click", open);
  modal.querySelectorAll("[data-close-modal]").forEach((b) => b.addEventListener("click", close));
  modal.addEventListener("click", (e) => { if (e.target === modal) close(); });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !modal.hidden) close(); });

  // ===== 자산 유형에 따라 상태 목록 바꾸기 =====
  // 드론: 정상 / 운용 중 / 정비 필요 / 정비 중 / 고장,  약제: 정상 / 부족
  const typeSel = modal.querySelector('select[name="type"]');
  const statusSel = modal.querySelector('select[name="status"]');

  if (typeSel && statusSel) {
    const ALLOWED = {
      drone: ["normal", "operating", "maintenance", "in-maintenance", "fault"],
      chemical: ["normal", "low"],
    };
    // 서버가 그려준 전체 상태 목록을 처음에 저장해 둠
    const allOptions = Array.from(statusSel.options).map((o) => ({
      value: o.value,
      text: o.textContent,
    }));

    const applyType = () => {
      const keys = ALLOWED[typeSel.value] || allOptions.map((o) => o.value);
      const prev = statusSel.value;
      statusSel.innerHTML = "";
      allOptions
        .filter((o) => keys.includes(o.value))
        .forEach((o) => statusSel.add(new Option(o.text, o.value)));
      if (keys.includes(prev)) statusSel.value = prev;   // 이전 선택이 가능하면 유지
          const subtitleLabel = modal.querySelector('input[name="subtitle"]')?.closest("label");
    // (applyType 함수 안, 맨 끝에 추가)
    if (subtitleLabel) subtitleLabel.style.display = typeSel.value === "drone" ? "none" : "";
    };

    typeSel.addEventListener("change", applyType);
    applyType();   // 팝업을 처음 열 때도 맞춰 둠
    
  }
});
