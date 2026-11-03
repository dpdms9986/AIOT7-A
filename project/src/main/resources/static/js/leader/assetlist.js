
// 드론·약제 관리 - 등록 팝업 열기/닫기 및 상태 선택지 전환
document.addEventListener("DOMContentLoaded", () => {
    const modal = document.querySelector("#asset-register-modal");
    const openBtn = document.querySelector("#asset-register-btn");

    if (!modal || !openBtn) return;

    const typeSelect = modal.querySelector("#asset-type");
    const statusSelect = modal.querySelector('select[name="status"]');

    const open = () => {
        modal.hidden = false;
        document.body.style.overflow = "hidden";
    };

    const close = () => {
        modal.hidden = true;
        document.body.style.overflow = "";
    };

    // 기존 팝업 열기/닫기
    openBtn.addEventListener("click", open);
    modal.querySelectorAll("[data-close-modal]").forEach((b) => {
        b.addEventListener("click", close);
    });

    modal.addEventListener("click", (e) => {
        if (e.target === modal) close();
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && !modal.hidden) close();
    });

    // 자산 유형에 따른 상태 선택지 전환
    if (typeSelect && statusSelect) {
        const allOptions = Array.from(statusSelect.options)
            .map(option => option.cloneNode(true));

        function updateAssetStatus() {
            const type = typeSelect.value;

            statusSelect.innerHTML = "";

            allOptions.forEach(option => {
                const optionType = option.dataset.assetType;

                if (optionType === type || optionType === "both") {
                    statusSelect.add(option.cloneNode(true));
                }
            });

            statusSelect.value = "normal";
        }

        typeSelect.addEventListener("change", updateAssetStatus);

        updateAssetStatus();
    }
});
