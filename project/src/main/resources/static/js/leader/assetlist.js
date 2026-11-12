
// 드론·약제 관리 - 등록 팝업 열기/닫기 및 상태 선택지 전환
document.addEventListener("DOMContentLoaded", () => {
    const modal = document.querySelector("#asset-register-modal");
    const openBtn = document.querySelector("#asset-register-btn");

    if (modal && openBtn) {
    // 기존 자산 등록 팝업 코드 전체

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
  // 배경에서 누르고 배경에서 뗐을 때만 닫기
  let downOnBackdrop = false;
  modal.addEventListener("pointerdown", (e) => { downOnBackdrop = (e.target === modal); });
  modal.addEventListener("click", (e) => { if (e.target === modal && downOnBackdrop) close(); });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !modal.hidden) close(); });

  // ===== 자산 유형에 따라 상태 목록 바꾸기 =====
  // 드론: 정상 / 운용 중 / 정비 필요 / 정비 중 / 고장,  약제: 정상 / 부족
  const typeSel = modal.querySelector('select[name="type"]');
  const statusSel = modal.querySelector('select[name="status"]');

  if (typeSel && statusSel) {
    const ALLOWED = {
      drone: ["normal", "maintenance", "in-maintenance", "fault"],
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
}
// ==============================
// 전체 자산 탭 - 상태 검색 필터
// ==============================
const filterForm = document.querySelector(".asset-pilot-filters");
const filterTypeSelect = filterForm?.querySelector('select[name="type"]');
const searchStatusSelect = document.querySelector("#asset-search-status");

// 전체 자산 탭에서만 실행
if (filterTypeSelect && searchStatusSelect) {

    // 서버가 전달한 상태 옵션 원본 보관
    const allOptions = Array.from(searchStatusSelect.options)
        .filter(option => option.value !== "")
        .map(option => option.cloneNode(true));

    const applyStatusOptions = () => {
        const selectedType = filterTypeSelect.value;
        const previousStatus = searchStatusSelect.value;

        // 기존 옵션 초기화
        searchStatusSelect.innerHTML = '<option value="">전체</option>';

        // 선택한 자산 유형에 맞는 상태만 표시
        allOptions
            .filter(option => {
                const assetType = option.dataset.assetType;

                return selectedType === ""
                    || assetType === "both"
                    || assetType === selectedType;
            })
            .forEach(option => {
                searchStatusSelect.add(option.cloneNode(true));
            });

        // 이전 상태가 현재 유형에서도 유효하면 유지
        const isValidStatus = Array.from(searchStatusSelect.options)
            .some(option => option.value === previousStatus);

        searchStatusSelect.value = isValidStatus ? previousStatus : "";
    };

    filterTypeSelect.addEventListener("change", applyStatusOptions);

    // 페이지 최초 진입 시에도 적용
    applyStatusOptions();
}
});
