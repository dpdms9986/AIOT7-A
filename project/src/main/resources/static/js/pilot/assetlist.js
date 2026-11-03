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

    // 자산 종류에 따른 입력 항목 설정
    const updateForm = (type) => {
        statusSelect.innerHTML = "";

        if (type === "drone") {
            nameLabel.textContent = "이름 / 모델명";
            nameInput.placeholder = "드론 모델명을 입력하세요";

            subtitleLabel.textContent = "설명";
            subtitleInput.placeholder = "드론 설명을 입력하세요";
            subtitleInput.value = "";
            subtitleInput.required = false;

            statusSelect.add(new Option("정상", "AVAILABLE"));
            statusSelect.add(new Option("정비 필요", "MAINTENANCE"));
            statusSelect.add(new Option("운용 중지", "INACTIVE"));

        } else if (type === "chemical") {
            nameLabel.textContent = "약제명";
            nameInput.placeholder = "약제명을 입력하세요";

            subtitleLabel.textContent = "약제 분류";
            subtitleInput.placeholder = "예: 살충제, 살균제";
            subtitleInput.required = true;

            statusSelect.add(new Option("정상", "normal"));
            statusSelect.add(new Option("재고 부족", "low"));
            statusSelect.add(new Option("품절", "empty"));

        } else {
            nameLabel.textContent = "이름 / 모델명";
            nameInput.placeholder = "자산 이름을 입력하세요";

            subtitleLabel.textContent = "설명";
            subtitleInput.placeholder = "자산 설명을 입력하세요";
            subtitleInput.value = "";
            subtitleInput.required = false;

            statusSelect.add(new Option("자산 종류를 먼저 선택하세요", ""));
        }
    };

    // 모달 열기
    const openModal = () => {
        modal.hidden = false;
        document.body.style.overflow = "hidden";

        typeSelect.value = "";
        nameInput.value = "";
        subtitleInput.value = "";

        updateForm("");
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
        subtitleInput.value = "";
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
});