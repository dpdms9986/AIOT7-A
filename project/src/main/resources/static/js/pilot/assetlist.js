
document.addEventListener("DOMContentLoaded", () => {
    const modal = document.querySelector("#asset-register-modal");

    const openBtn = document.querySelector("#open-asset-register");
    const closeBtn = document.querySelector("#close-asset-register");
    const cancelBtn = document.querySelector("#cancel-asset-register");

    const typeSelect = document.querySelector("#asset-type");
    const nameLabel = document.querySelector("#asset-name-label");
    const nameInput = document.querySelector("#asset-name");

    const subtitleLabel = document.querySelector("#asset-subtitle-label");
    const subtitleInput = document.querySelector("#asset-subtitle");

    const statusSelect = document.querySelector("#asset-status");

    if (
        !modal || !openBtn || !closeBtn || !cancelBtn ||
        !typeSelect || !nameLabel || !nameInput ||
        !subtitleLabel || !subtitleInput || !statusSelect
    ) {
        console.error("자산 등록 모달 요소를 찾을 수 없습니다.");
        return;
    }

    // 자산 종류에 따른 입력 항목 및 상태 선택지 설정
    const updateForm = (type) => {
        statusSelect.innerHTML = "";

        if (type === "drone") {
            nameLabel.textContent = "이름 / 모델명";
            nameInput.placeholder = "드론 모델명을 입력하세요";

            subtitleLabel.textContent = "설명";
            subtitleInput.placeholder = "드론 설명을 입력하세요";
            subtitleInput.value = "";
            subtitleInput.required = false;

            statusSelect.add(new Option("정상", "normal"));
            statusSelect.add(new Option("운용 중", "operating"));
            statusSelect.add(new Option("정비 중", "maintenance"));
            statusSelect.add(new Option("정비 필요", "in-maintenance"));
            statusSelect.add(new Option("고장", "fault"));

        } else if (type === "chemical") {
            nameLabel.textContent = "약제명";
            nameInput.placeholder = "약제명을 입력하세요";

            subtitleLabel.textContent = "약제 분류";
            subtitleInput.placeholder = "예: 살충제, 살균제";
            subtitleInput.value = "";
            subtitleInput.required = true;

            statusSelect.add(new Option("정상", "normal"));
            statusSelect.add(new Option("재고 부족", "low"));
        }

        // 리더와 동일한 방식으로 설명란 숨김
    // 리더와 동일하게 설명 입력 영역 전체 숨김
     const subtitleField = subtitleInput.closest("label");

      if (subtitleField) {
    subtitleField.style.display = type === "drone" ? "none" : "";
       }
    };
    // 모달 열기
    const openModal = () => {
        modal.hidden = false;
        document.body.style.overflow = "hidden";

        typeSelect.value = "drone";
        nameInput.value = "";
        subtitleInput.value = "";

        updateForm("drone");
    };

    // 모달 닫기
    const closeModal = () => {
        modal.hidden = true;
        document.body.style.overflow = "";
    };

    openBtn.addEventListener("click", openModal);
    closeBtn.addEventListener("click", closeModal);
    cancelBtn.addEventListener("click", closeModal);

    // 자산 종류 변경
    typeSelect.addEventListener("change", () => {
        updateForm(typeSelect.value);
    });

    // 바깥 영역 클릭
    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            closeModal();
        }
    });

    // ESC 키
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.hidden) {
            closeModal();
        }
    });

    // 전체 자산 탭 - 자산 유형에 따른 상태 검색 옵션 변경
        const filterForm = document.querySelector(".asset-pilot-filters");
        const filterTypeSelect = filterForm?.querySelector('select[name="type"]');
        const searchStatusSelect = document.querySelector("#asset-search-status");

    // 전체 탭이 아니면 자산 유형 선택창이 아니므로 종료
    if (filterTypeSelect && searchStatusSelect) {

    // 서버가 전달한 상태 옵션을 원본으로 보관
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
