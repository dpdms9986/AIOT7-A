
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
    rows.forEach((item) => {
        item.classList.toggle("selected", item === row);
    });

    document.getElementById("pilot-asset-detail-card").hidden = false;

    const type = row.dataset.assetType;
    const cells = row.querySelectorAll("td");

    const name =
        cells[1].querySelector("strong")?.textContent.trim() || "";

    const subtitle =
        cells[1].querySelector("small")?.textContent.trim() || "";

    // 현재 HTML의 실제 열 순서에 맞춤
    const status = cells[2].innerText.trim();
    const date = cells[3].innerText.trim();
    const assetId = row.dataset.assetId;

    detailType.textContent =
        type === "drone" ? "드론 기체" : "농업용 약제";

    const escape = escapeHTML;

    if (type === "drone") {
        // 드론 상세 정보
        // 실제 DB에 없는 값은 임의로 만들지 않고 확인 필요로 표시
        const droneStatus = {
    AVAILABLE: {
        label: "AVAILABLE · 가용",
        className: "available"
    },
    IN_USE: {
        label: "IN_USE · 운용 중",
        className: "in-use"
    },
    MAINTENANCE: {
        label: "MAINTENANCE · 정비 중",
        className: "maintenance"
    }
};

const currentStatus = droneStatus[row.dataset.status] || {
    label: "상태 확인 필요",
    className: "unknown"
};

        detail.innerHTML = `
    <div class="asset-detail-title">
        <h3>${escape(name)}</h3>
        <p>기체 코드 ${escape(assetId)} · 드론 관리 정보</p>
    </div>

    <div class="asset-current-status ${currentStatus.className}">
        <span class="asset-current-status-label">현재 가동 상태</span>
        <strong>
            <span class="status-dot"></span>
            ${escape(currentStatus.label)}
        </strong>
    </div>

    <div class="asset-detail-grid">
        <div class="asset-detail-info">
            <span>기체 코드</span>
            <strong>${escape(assetId)}</strong>
        </div>
        <div class="asset-detail-info">
            <span>담당 조종사</span>
            <strong>정보 확인 필요</strong>
        </div>
        <div class="asset-detail-info">
            <span>약제 탱크 용량</span>
            <strong>정보 확인 필요</strong>
        </div>
        <div class="asset-detail-info">
            <span>배터리 사이클</span>
            <strong>정보 확인 필요</strong>
        </div>
        <div class="asset-detail-info">
            <span>최근 관리일</span>
            <strong>${escape(date)}</strong>
        </div>
    </div>

    <div class="asset-detail-status-guide">
        <span><i class="guide-available"></i> AVAILABLE · 가용</span>
        <span><i class="guide-in-use"></i> IN_USE · 사용 중</span>
        <span><i class="guide-maintenance"></i> MAINTENANCE · 정비 중</span>
    </div>

    <div class="asset-detail-section">
        <div class="asset-detail-section-heading">
            <h4>정비 및 점검 이력</h4>
            <span>최신 기록부터 표시</span>
        </div>

        <div class="asset-detail-table-wrap">
            <table class="asset-detail-table">
                <thead>
                    <tr>
                        <th>관리 일자</th>
                        <th>관리 유형</th>
                        <th>관리 내용</th>
                        <th>정비 비용</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td class="empty-row" colspan="4">
                            정비 이력 데이터 연동 필요
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
`;

    } else {
        // 약제 상세 정보
        const isLow = row.dataset.status === "low";

        detail.innerHTML = `
            <div class="asset-detail-top">
                <div class="asset-detail-title">
                    <h3>${escape(name)}</h3>
                    <p>약제 코드 ${escape(assetId)} · 농업용 약제 관리</p>
                </div>

                <div class="asset-detail-status ${isLow ? "low" : "normal"}">
                    <span class="status-dot"></span>
                    ${isLow ? "재고 부족" : "정상"}
                </div>
            </div>

            <div class="asset-detail-grid">
                <div class="asset-detail-info">
                    <span>약제 코드</span>
                    <strong>${escape(assetId)}</strong>
                </div>

                <div class="asset-detail-info">
                    <span>약제명</span>
                    <strong>${escape(name)}</strong>
                </div>

                <div class="asset-detail-info">
                    <span>약제 분류</span>
                    <strong>${escape(subtitle)}</strong>
                </div>

                <div class="asset-detail-info">
                    <span>현재 재고량</span>
                    <strong>수량 데이터 확인 필요</strong>
                    <small>${isLow ? "재고 부족 상태로 표시 중" : "재고 수량 연동 필요"}</small>
                </div>

                <div class="asset-detail-info">
                    <span>권장 살포량</span>
                    <strong>정보 확인 필요</strong>
                </div>

                <div class="asset-detail-info">
                    <span>최근 재고 갱신일</span>
                    <strong>${escape(date)}</strong>
                </div>
            </div>

            <div class="asset-detail-section">
                <div class="asset-detail-section-heading">
                    <h4>입고 이력</h4>
                    <span>약제 입고 내역</span>
                </div>

                <div class="asset-detail-table-wrap">
                    <table class="asset-detail-table">
                        <thead>
                            <tr>
                                <th>입고 일자</th>
                                <th>입고 수량</th>
                                <th>비고</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="empty-row" colspan="3">
                                    입고 이력 데이터 연동 필요
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="asset-detail-section">
                <div class="asset-detail-section-heading">
                    <h4>사용 이력</h4>
                    <span>방제 작업 사용 내역</span>
                </div>

                <div class="asset-detail-table-wrap">
                    <table class="asset-detail-table">
                        <thead>
                            <tr>
                                <th>사용 일자</th>
                                <th>사용 농지</th>
                                <th>사용량</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="empty-row" colspan="3">
                                    약제 사용 이력 데이터 연동 필요
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    }

    // 드론 정비 이력 버튼은 기존 동작을 유지
    const maintenanceButton = detail.querySelector(
        "[data-maintenance-for]"
    );

    if (maintenanceButton) {
        maintenanceButton.addEventListener("click", () => {
            alert(
                "정비 이력 API가 연결되면 해당 기체의 정비 기록을 표시합니다."
            );
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

    document.getElementById("pilot-asset-detail-card").hidden = true;

    selectTab("all");
})();
