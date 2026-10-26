
(() => {
    const tbody = document.getElementById("pilot-asset-tbody");
    if (!tbody) return;

    const rows = Array.from(tbody.querySelectorAll("tr"));
    const tabs = Array.from(document.querySelectorAll("[data-pilot-tab]"));
    const typeFilter = document.getElementById("pilot-asset-type");
    const statusFilter = document.getElementById("pilot-asset-status");
    const searchInput = document.getElementById("pilot-asset-search");
    const form = document.getElementById("pilot-asset-search-form");
    const count = document.getElementById("pilot-visible-count");
    const empty = document.getElementById("pilot-asset-empty");
    const title = document.getElementById("pilot-list-title");
    const detail = document.getElementById("pilot-asset-detail");
    const detailType = document.getElementById("pilot-detail-type");

    let currentTab = "all";

    const tabTitles = {
        all: "전체 자산",
        drone: "내 드론 기체",
        chemical: "내 농업용 약제",
        maintenance: "정비 현황"
    };

    function renderRows() {
        const keyword = searchInput.value.trim().toLocaleLowerCase("ko-KR");
        const selectedType = typeFilter.value;
        const selectedStatus = statusFilter.value;
        let visibleCount = 0;

        rows.forEach((row) => {
            const type = row.dataset.assetType;
            const status = row.dataset.status;
            const isMaintenance = row.dataset.maintenance === "true";

            let show = true;

            if (currentTab === "drone" && type !== "drone") show = false;
            if (currentTab === "chemical" && type !== "chemical") show = false;
            if (currentTab === "maintenance" && !isMaintenance) show = false;

            if (selectedType && type !== selectedType) show = false;
            if (selectedStatus && status !== selectedStatus) show = false;

            if (keyword &&
                !row.textContent.toLocaleLowerCase("ko-KR").includes(keyword)) {
                show = false;
            }

            row.hidden = !show;
            if (show) visibleCount++;
        });

        title.textContent = tabTitles[currentTab];
        count.textContent = String(visibleCount);
        empty.hidden = visibleCount !== 0;
    }

    function selectTab(tab) {
        currentTab = tab;

        tabs.forEach((button) => {
            const active = button.dataset.pilotTab === tab;
            button.classList.toggle("active", active);
            button.setAttribute("aria-pressed", String(active));
        });

        typeFilter.value = "";
        statusFilter.value = "";
        searchInput.value = "";

        renderRows();
    }

    function selectAsset(row) {
        rows.forEach((item) => item.classList.toggle("selected", item === row));

        const type = row.dataset.assetType;
        const cells = row.querySelectorAll("td");
        const name = cells[1].querySelector("strong")?.textContent.trim() || "";
        const subtitle = cells[1].querySelector("small")?.textContent.trim() || "";
        const spec = cells[2].innerText.trim();
        const status = cells[3].innerText.trim();
        const date = cells[4].innerText.trim();
        const assetId = row.dataset.assetId;

        detailType.textContent = type === "drone" ? "드론 기체" : "농업용 약제";

        detail.innerHTML = `
            <div class="asset-pilot-detail-grid">
                <div class="asset-pilot-detail-item">
                    <span>자산명</span>
                    <strong>${escapeHTML(name)}</strong>
                </div>
                <div class="asset-pilot-detail-item">
                    <span>자산 코드</span>
                    <strong>${escapeHTML(assetId)}</strong>
                </div>
                <div class="asset-pilot-detail-item">
                    <span>${type === "drone" ? "기체 정보" : "약제 분류"}</span>
                    <strong>${escapeHTML(subtitle)}</strong>
                </div>
                <div class="asset-pilot-detail-item">
                    <span>${type === "drone" ? "규격" : "재고 정보"}</span>
                    <strong>${escapeHTML(spec)}</strong>
                </div>
                <div class="asset-pilot-detail-item">
                    <span>현재 상태</span>
                    <strong>${escapeHTML(status)}</strong>
                </div>
                <div class="asset-pilot-detail-item">
                    <span>최근 관리일</span>
                    <strong>${escapeHTML(date)}</strong>
                </div>
            </div>
            ${type === "drone"
                ? `<button type="button" class="asset-pilot-maintenance-button"
                    data-maintenance-for="${escapeHTML(assetId)}">
                    정비 이력 조회
                   </button>`
                : ""}
        `;

        const maintenanceButton = detail.querySelector("[data-maintenance-for]");

        if (maintenanceButton) {
            maintenanceButton.addEventListener("click", () => {
                alert("정비 이력 API가 연결되면 해당 기체의 정비 기록을 표시합니다.");
            });
        }
    }

    function escapeHTML(value) {
        return String(value).replace(/[&<>"']/g, (char) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[char]);
    }

    tabs.forEach((button) => {
        button.addEventListener("click", () => {
            selectTab(button.dataset.pilotTab);
        });
    });

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        renderRows();
    });

    form.addEventListener("reset", () => {
        setTimeout(renderRows, 0);
    });

    searchInput.addEventListener("input", renderRows);
    typeFilter.addEventListener("change", renderRows);
    statusFilter.addEventListener("change", renderRows);

    tbody.addEventListener("click", (event) => {
        const button = event.target.closest(".asset-pilot-detail-button");
        if (!button) return;

        const row = button.closest("tr");
        if (row) selectAsset(row);
    });

    function updateSummary() {
        const droneRows = rows.filter(row => row.dataset.assetType === "drone");
        const chemicalRows = rows.filter(row => row.dataset.assetType === "chemical");
        const maintenanceRows = droneRows.filter(row =>
            row.dataset.maintenance === "true"
        );

        document.getElementById("pilot-drone-count").textContent =
            `${droneRows.length}대`;

        document.getElementById("pilot-chemical-count").textContent =
            `${chemicalRows.length}종`;

        document.getElementById("pilot-maintenance-count").textContent =
            `${maintenanceRows.length}건`;
    }

    updateSummary();
    selectTab("all");
})();
