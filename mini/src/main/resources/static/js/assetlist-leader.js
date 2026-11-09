console.log("LEADER JS NEW VERSION LOADED");

(() => {
    "use strict";

    const init = () => {
        const tbody = document.getElementById("pilot-asset-tbody");

        if (!tbody) {
            console.error("자산 목록 테이블을 찾을 수 없습니다.");
            return;
        }

        const tabs = [...document.querySelectorAll("[data-pilot-tab]")];
        const searchForm = document.getElementById("pilot-asset-search-form");
        const typeFilter = document.getElementById("pilot-asset-type");
        const ownerFilter = document.getElementById("pilot-asset-owner");
        const statusFilter = document.getElementById("pilot-asset-status");
        const searchInput = document.getElementById("pilot-asset-search");

        const listTitle = document.getElementById("pilot-list-title");
        const visibleCount = document.getElementById("pilot-visible-count");
        const emptyMessage = document.getElementById("pilot-asset-empty");

        const droneCount = document.getElementById("pilot-drone-count");
        const chemicalCount = document.getElementById("pilot-chemical-count");
        const maintenanceCount = document.getElementById("pilot-maintenance-count");

        const registerButton = document.getElementById("pilot-asset-register");
        const detailCard = document.getElementById("pilot-asset-detail-card");
        const detail = document.getElementById("pilot-asset-detail");
        const detailType = document.getElementById("pilot-detail-type");

        let currentTab = "all";

        const tabTitles = {
            all: "전체 자산",
            drone: "드론 기체",
            chemical: "농업용 약제",
            maintenance: "정비 현황"
        };

        const statusLabels = {
            normal: "정상",
            low: "부족",
            maintenance: "정비 필요",
            fault: "고장",
            "in-maintenance": "정비 중",
            operating: "운용 중"
        };

        // ==============================
        // 공통 함수
        // ==============================

        function getRows() {
            return [...tbody.querySelectorAll("tr[data-asset-type]")];
        }

        function getType(row) {
            return (row.dataset.assetType || "").toLowerCase();
        }

        function getStatus(row) {
            return (row.dataset.status || "").toLowerCase();
        }

        function getOwner(row) {
            return row.dataset.owner || row.cells[2]?.textContent.trim() || "";
        }

        function getCellText(row, index) {
            return row.cells[index]?.innerText.trim() || "";
        }

        function isMaintenanceAsset(row) {
            const status = getStatus(row);
            const badge = row.querySelector(".asset-pilot-badge")?.textContent || "";

            return (
                row.dataset.maintenance === "true" ||
                ["maintenance", "fault", "broken", "repair", "in-maintenance"]
                    .includes(status) ||
                /정비|고장|점검/.test(badge)
            );
        }

        function escapeHTML(value) {
            return String(value ?? "").replace(/[&<>"']/g, char => ({
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#39;"
            })[char]);
        }

        function clearSelection() {
            getRows().forEach(row => {
                row.classList.remove("selected");
            });
        }

        // ==============================
        // 목록 표시
        // ==============================

        function renderRows() {
            const keyword = (searchInput?.value || "")
                .trim()
                .toLocaleLowerCase("ko-KR");

            const selectedType = typeFilter?.value || "";
            const selectedOwner = ownerFilter?.value || "";
            const selectedStatus = statusFilter?.value || "";

            let count = 0;

            getRows().forEach(row => {
                const type = getType(row);
                const status = getStatus(row);
                const owner = getOwner(row);

                const matchesTab =
                    currentTab === "all" ||
                    (currentTab === "drone" && type === "drone") ||
                    (currentTab === "chemical" && type === "chemical") ||
                    (currentTab === "maintenance" && isMaintenanceAsset(row));

                const matchesFilters =
                    (!selectedType || type === selectedType) &&
                    (!selectedOwner || owner === selectedOwner) &&
                    (!selectedStatus || status === selectedStatus) &&
                    (!keyword ||
                        row.textContent.toLocaleLowerCase("ko-KR").includes(keyword));

                const show = matchesTab && matchesFilters;

                row.hidden = !show;
                row.style.display = show ? "table-row" : "none";

                if (show) {
                    count++;
                } else {
                    row.classList.remove("selected");
                }
            });

            tabs.forEach(tab => {
                const active = tab.dataset.pilotTab === currentTab;

                tab.classList.toggle("active", active);
                tab.setAttribute("aria-pressed", String(active));
            });

            if (listTitle) {
                listTitle.textContent = tabTitles[currentTab];
            }

            if (visibleCount) {
                visibleCount.textContent = String(count);
            }

            if (emptyMessage) {
                emptyMessage.hidden = count !== 0;
            }
        }

        // ==============================
        // 탭 전환
        // ==============================

        function selectTab(tabName) {
            if (!Object.prototype.hasOwnProperty.call(tabTitles, tabName)) {
                return;
            }

            currentTab = tabName;

            if (typeFilter) typeFilter.value = "";
            if (ownerFilter) ownerFilter.value = "";
            if (statusFilter) statusFilter.value = "";
            if (searchInput) searchInput.value = "";

            clearSelection();

            if (detailCard) {
                detailCard.hidden = true;
            }

            renderRows();
        }

        // HTML의 onclick="switchAssetTab('drone')"과 연결
        window.switchAssetTab = selectTab;

        // 이벤트 위임: 탭 버튼 이벤트를 한 곳에서 처리
        const tabContainer = document.querySelector(".asset-pilot-tabs");

        if (tabContainer) {
            tabContainer.addEventListener("click", event => {
                const tab = event.target.closest("[data-pilot-tab]");

                if (!tab || !tabContainer.contains(tab)) {
                    return;
                }

                event.preventDefault();
                selectTab(tab.dataset.pilotTab);
            });
        }

        // ==============================
        // 요약 숫자
        // ==============================

        function updateSummary() {
            const rows = getRows();

            if (droneCount) {
                droneCount.textContent =
                    `${rows.filter(row => getType(row) === "drone").length}대`;
            }

            if (chemicalCount) {
                chemicalCount.textContent =
                    `${rows.filter(row => getType(row) === "chemical").length}종`;
            }

            if (maintenanceCount) {
                maintenanceCount.textContent =
                    `${rows.filter(row =>
                        getType(row) === "drone" && isMaintenanceAsset(row)
                    ).length}건`;
            }
        }

        // ==============================
        // 검색 및 필터
        // ==============================

        if (searchForm) {
            searchForm.addEventListener("submit", event => {
                event.preventDefault();
                renderRows();
            });

            searchForm.addEventListener("reset", () => {
                currentTab = "all";

                setTimeout(() => {
                    clearSelection();

                    if (detailCard) {
                        detailCard.hidden = true;
                    }

                    renderRows();
                }, 0);
            });
        }

        [typeFilter, ownerFilter, statusFilter].forEach(control => {
            if (control) {
                control.addEventListener("change", renderRows);
            }
        });

        if (searchInput) {
            searchInput.addEventListener("input", renderRows);
        }

        // ==============================
        // 자산 상세보기
        // ==============================

        function selectAsset(row) {
            if (!detailCard || !detail || !detailType) {
                return;
            }

            clearSelection();
            row.classList.add("selected");

            const type = getType(row);
            const assetId = row.dataset.assetId || "";
            const nameCell = row.cells[1];

            const name =
                nameCell?.querySelector("strong")?.textContent.trim() || "";

            const subtitle =
                nameCell?.querySelector("small")?.textContent.trim() || "";

            const owner = getCellText(row, 2);
            const status = getCellText(row, 3);
            const recentDate = getCellText(row, 4);

            detailType.textContent =
                type === "drone" ? "드론 기체" :
                type === "chemical" ? "농업용 약제" : "자산";

            if (type === "drone") {
                detail.innerHTML = `
                    <div class="asset-detail-title">
                        <h3>${escapeHTML(name)}</h3>
                        <p>기체 코드 ${escapeHTML(assetId)} · 드론 관리 정보</p>
                    </div>

                    <div class="asset-current-status unknown">
                        <span class="asset-current-status-label">현재 가동 상태</span>
                        <strong>${escapeHTML(status || "상태 확인 필요")}</strong>
                    </div>

                    <div class="asset-detail-grid">
                        <div class="asset-detail-info">
                            <span>기체 코드</span>
                            <strong>${escapeHTML(assetId || "정보 확인 필요")}</strong>
                        </div>
                        <div class="asset-detail-info">
                            <span>담당 조종사</span>
                            <strong>${escapeHTML(owner || "정보 확인 필요")}</strong>
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
                            <strong>${escapeHTML(recentDate || "기록 확인 필요")}</strong>
                        </div>
                    </div>

                    <div class="asset-detail-section">
                        <div class="asset-detail-section-heading">
                            <h4>정비 및 점검 이력</h4>
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
                                        <td colspan="4" class="empty-row">
                                            정비 이력 데이터 연동 필요
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                `;
            } else if (type === "chemical") {
                detail.innerHTML = `
                    <div class="asset-detail-title">
                        <h3>${escapeHTML(name)}</h3>
                        <p>약제 코드 ${escapeHTML(assetId)} · 농업용 약제 관리</p>
                    </div>

                    <div class="asset-detail-grid">
                        <div class="asset-detail-info">
                            <span>약제 코드</span>
                            <strong>${escapeHTML(assetId || "정보 확인 필요")}</strong>
                        </div>
                        <div class="asset-detail-info">
                            <span>약제명</span>
                            <strong>${escapeHTML(name)}</strong>
                        </div>
                        <div class="asset-detail-info">
                            <span>담당 조종사</span>
                            <strong>${escapeHTML(owner || "정보 확인 필요")}</strong>
                        </div>
                        <div class="asset-detail-info">
                            <span>약제 분류</span>
                            <strong>${escapeHTML(subtitle || "정보 확인 필요")}</strong>
                        </div>
                        <div class="asset-detail-info">
                            <span>재고 상태</span>
                            <strong>${escapeHTML(status || "정보 확인 필요")}</strong>
                        </div>
                        <div class="asset-detail-info">
                            <span>최근 재고 갱신일</span>
                            <strong>${escapeHTML(recentDate || "기록 확인 필요")}</strong>
                        </div>
                        <div class="asset-detail-info">
                            <span>현재 재고량</span>
                            <strong>수량 데이터 연동 필요</strong>
                        </div>
                    </div>

                    <div class="asset-detail-section">
                        <div class="asset-detail-section-heading">
                            <h4>입고 이력</h4>
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
                                        <td colspan="3" class="empty-row">
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
                                        <td colspan="3" class="empty-row">
                                            약제 사용 이력 데이터 연동 필요
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                `;
            } else {
                detail.innerHTML = "<p>자산 유형을 확인할 수 없습니다.</p>";
            }

            detailCard.hidden = false;
            detailCard.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }

        tbody.addEventListener("click", event => {
            const button = event.target.closest(".asset-pilot-detail-button");

            if (!button || !tbody.contains(button)) {
                return;
            }

            const row = button.closest("tr");

            if (row) {
                selectAsset(row);
            }
        });

        // ==============================
        // 자산 등록
        // ==============================

        if (registerButton) {
            registerButton.addEventListener("click", () => {
                const type = prompt(
                    "자산 유형을 입력하세요: drone 또는 chemical",
                    "drone"
                );

                if (type === null) return;

                const normalizedType = type.trim().toLowerCase();

                if (!["drone", "chemical"].includes(normalizedType)) {
                    alert("drone 또는 chemical로 입력하세요.");
                    return;
                }

                const name = prompt("자산명 또는 모델명을 입력하세요.");
                if (!name?.trim()) return;

                const owner = prompt("담당 조종사 이름을 입력하세요.");
                if (!owner?.trim()) return;

                const status = prompt(
                    "상태를 입력하세요: normal, low, maintenance, fault, in-maintenance, operating",
                    "normal"
                );

                if (status === null) return;

                const normalizedStatus = status.trim().toLowerCase();

                if (!Object.prototype.hasOwnProperty.call(statusLabels, normalizedStatus)) {
                    alert("올바르지 않은 상태입니다.");
                    return;
                }

                const prefix = normalizedType === "drone" ? "DR" : "CH";
                const ids = getRows().map(row => row.dataset.assetId);

                let number = 1;
                let assetId;

                do {
                    assetId = `${prefix}-${String(number).padStart(3, "0")}`;
                    number++;
                } while (ids.includes(assetId));

                const row = document.createElement("tr");

                row.dataset.assetType = normalizedType;
                row.dataset.assetId = assetId;
                row.dataset.status = normalizedStatus;
                row.dataset.owner = owner.trim();

                row.dataset.maintenance = String(
                    ["maintenance", "fault", "in-maintenance"].includes(normalizedStatus)
                );

                const values = [
                    normalizedType === "drone" ? "🚁 드론" : "🧪 약제",
                    "",
                    owner.trim(),
                    "",
                    "기록 확인 필요",
                    ""
                ];

                values.forEach((value, index) => {
                    const cell = document.createElement("td");

                    if (index === 1) {
                        const strong = document.createElement("strong");
                        strong.textContent = name.trim();

                        const small = document.createElement("small");
                        small.textContent = `자산 코드 ${assetId}`;

                        cell.append(strong, small);
                    } else if (index === 3) {
                        const badge = document.createElement("span");
                        badge.className = `asset-pilot-badge ${normalizedStatus}`;
                        badge.textContent = statusLabels[normalizedStatus];

                        cell.appendChild(badge);
                    } else if (index === 5) {
                        const button = document.createElement("button");

                        button.type = "button";
                        button.className = "asset-pilot-detail-button";
                        button.dataset.assetId = assetId;
                        button.textContent = "상세보기";

                        cell.appendChild(button);
                    } else {
                        cell.textContent = value;
                    }

                    row.appendChild(cell);
                });

                tbody.appendChild(row);

                updateSummary();
                selectTab("all");
            });
        }

        // ==============================
        // 최초 초기화
        // ==============================

        if (detailCard) {
            detailCard.hidden = true;
        }

        updateSummary();
        renderRows();
    };

    // DOM 로딩 상태에 따라 초기화
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
})();