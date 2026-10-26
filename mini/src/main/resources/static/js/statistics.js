
(() => {
    const body = document.getElementById("stats-history-body");
    if (!body) return;

    const $ = selector => document.querySelector(selector);
    const $$ = selector => Array.from(document.querySelectorAll(selector));

    // 화면 확인용 예시 데이터. 실제 DB 데이터가 아님.
    const history = [
        { id: 1, address: "봉명동 123-4", region: "유성구", pilot: "김조종", assigned: "2026-10-20 09:10", completed: "2026-10-20 10:05", area: 1200, status: "COMPLETED" },
        { id: 2, address: "구암동 55-3", region: "유성구", pilot: "이민수", assigned: "2026-10-20 10:20", completed: "2026-10-20 11:00", area: 850, status: "COMPLETED" },
        { id: 3, address: "신탄진동 88-1", region: "대덕구", pilot: "박지훈", assigned: "2026-10-21 08:30", completed: "2026-10-21 09:15", area: 1600, status: "COMPLETED" },
        { id: 4, address: "관평동 17-2", region: "유성구", pilot: "김조종", assigned: "2026-10-22 13:00", completed: "2026-10-22 13:50", area: 950, status: "COMPLETED" },
        { id: 5, address: "법동 201-1", region: "대덕구", pilot: "이민수", assigned: "2026-10-23 09:00", completed: "2026-10-23 09:45", area: 1100, status: "COMPLETED" },
        { id: 6, address: "둔산동 32-8", region: "서구", pilot: "김조종", assigned: "2026-10-24 10:00", completed: "2026-10-24 10:40", area: 700, status: "COMPLETED" },
        { id: 7, address: "송강동 61-2", region: "유성구", pilot: "박지훈", assigned: "2026-10-24 13:20", completed: "2026-10-24 14:10", area: 1400, status: "COMPLETED" },
        { id: 8, address: "월평동 45-1", region: "서구", pilot: "이민수", assigned: "2026-10-25 08:40", completed: "2026-10-25 09:30", area: 900, status: "COMPLETED" },
        { id: 9, address: "전민동 15-6", region: "유성구", pilot: "김조종", assigned: "2026-10-26 09:10", completed: "", area: 1300, status: "IN_PROGRESS" },
        { id: 10, address: "오정동 90-3", region: "대덕구", pilot: "박지훈", assigned: "2026-10-26 09:40", completed: "", area: 750, status: "ASSIGNED" }
    ];

    let currentTab = "history";

    const number = value => Number(value).toLocaleString("ko-KR");

    function escapeHTML(value) {
        return String(value ?? "").replace(/[&<>"']/g, char => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[char]);
    }

    function getFilteredHistory() {
        const start = $("#stats-start-date").value;
        const end = $("#stats-end-date").value;
        const region = $("#stats-region").value;
        const pilot = $("#stats-pilot").value;

        return history.filter(item => {
            const date = (item.completed || item.assigned).slice(0, 10);

            return (!start || date >= start) &&
                (!end || date <= end) &&
                (!region || item.region === region) &&
                (!pilot || item.pilot === pilot);
        });
    }

    function renderSummary(items) {
        const completed = items.filter(item => item.status === "COMPLETED");
        const totalArea = completed.reduce((sum, item) => sum + item.area, 0);
        const total = items.length;
        const rate = total ? Math.round(completed.length / total * 100) : 0;

        $("#stats-total-count").textContent = `${completed.length}건`;
        $("#stats-total-area").textContent = `${number(totalArea)}㎡`;
        $("#stats-total-pyeong").textContent =
            `약 ${number(Math.round(totalArea / 3.3058))}평`;

        // 기존 HTML의 ID와 이전 JS의 ID 모두 지원
        const pilotCount = $("#stats-pilot-count") || $("#stats-pilot-area");
        if (pilotCount) {
            pilotCount.textContent =
                `${new Set(completed.map(item => item.pilot)).size}명`;
        }

        $("#stats-completion-rate").textContent = `${rate}%`;
    }

    function renderTable(items) {
        let displayed = [...items];

        if (currentTab === "pilot") {
            displayed = displayed.filter(item => item.status === "COMPLETED");
            displayed.sort((a, b) => a.pilot.localeCompare(b.pilot, "ko"));
        } else if (currentTab === "area") {
            displayed = displayed.filter(item => item.status === "COMPLETED");
            displayed.sort((a, b) => a.address.localeCompare(b.address, "ko"));
        }

        $("#stats-table-title").textContent = {
            history: "방제 작업 이력",
            pilot: "조종사별 방제 실적",
            area: "농지별 방제 실적"
        }[currentTab];

        $("#stats-result-count").textContent = `${displayed.length}건`;

        body.innerHTML = displayed.map(item => `
            <tr>
                <td>${escapeHTML(item.address)}</td>
                <td>${escapeHTML(item.region)}</td>
                <td>${escapeHTML(item.pilot)}</td>
                <td>${escapeHTML(item.assigned)}</td>
                <td>${escapeHTML(item.completed || "-")}</td>
                <td>${number(item.area)}</td>
                <td>
                    <span class="stats-status"
                        style="${item.status !== "COMPLETED"
                            ? "background:#fff2e3;color:#b66a12"
                            : ""}">
                        ${item.status === "COMPLETED"
                            ? "완료"
                            : item.status === "IN_PROGRESS" ? "작업 중" : "배정"}
                    </span>
                </td>
            </tr>
        `).join("");

        $("#stats-empty").hidden = displayed.length > 0;
    }

    // 왼쪽: 기간별 방제 완료 막대 차트
    function renderTrend(items) {
        const completed = items.filter(item => item.status === "COMPLETED");
        const grouped = {};

        completed.forEach(item => {
            const date = item.completed.slice(5, 10);
            grouped[date] = (grouped[date] || 0) + 1;
        });

        const entries = Object.entries(grouped).sort((a, b) =>
            a[0].localeCompare(b[0])
        );

        const chart = $("#stats-trend-chart");

        if (!entries.length) {
            chart.innerHTML =
                '<p class="stats-empty">표시할 완료 이력이 없습니다.</p>';
            return;
        }

        const max = Math.max(...entries.map(entry => entry[1]), 1);

        chart.innerHTML = entries.map(([date, count]) => {
            const height = Math.max(3, count / max * 135);

            return `
                <div class="stats-bar-item"
                     title="${escapeHTML(date)}: ${count}건">
                    <span class="stats-bar-value">${count}</span>
                    <div class="stats-bar-track">
                        <div class="stats-bar" style="height:${height}px"></div>
                    </div>
                    <span class="stats-bar-label">${escapeHTML(date)}</span>
                </div>
            `;
        }).join("");
    }

    // 오른쪽: 지역별 방제 실적 도넛 차트
    function renderRegions(items) {
        const completed = items.filter(item => item.status === "COMPLETED");
        const grouped = {};

        completed.forEach(item => {
            grouped[item.region] = (grouped[item.region] || 0) + 1;
        });

        const entries = Object.entries(grouped).sort((a, b) => b[1] - a[1]);
        const total = completed.length;
        const chart = $("#stats-region-chart");

        if (!total || !entries.length) {
            chart.innerHTML =
                '<p class="stats-empty">표시할 지역 통계가 없습니다.</p>';
            return;
        }

        const colors = ["#269b66", "#4c91f5", "#f59e42", "#94a3b8", "#8b5cf6"];
        const radius = 54;
        const circumference = 2 * Math.PI * radius;
        let offset = 0;

        const segments = entries.map(([region, count], index) => {
            const fraction = count / total;
            const dash = fraction * circumference;
            const segment = `
                <circle
                    cx="70" cy="70" r="${radius}"
                    fill="none"
                    stroke="${colors[index % colors.length]}"
                    stroke-width="20"
                    stroke-dasharray="${dash} ${circumference - dash}"
                    stroke-dashoffset="${-offset}"
                    transform="rotate(-90 70 70)"
                />
            `;
            offset += dash;
            return segment;
        }).join("");

        const legend = entries.map(([region, count], index) => {
            const percent = Math.round(count / total * 100);

            return `
                <div class="stats-donut-legend-row">
                    <span class="stats-donut-dot"
                          style="background:${colors[index % colors.length]}"></span>
                    <span class="stats-donut-region">${escapeHTML(region)}</span>
                    <span class="stats-donut-value">
                        ${count}건 <small>(${percent}%)</small>
                    </span>
                </div>
            `;
        }).join("");

        chart.innerHTML = `
            <div class="stats-donut-wrap">
                <div class="stats-donut-visual">
                    <svg viewBox="0 0 140 140"
                         role="img"
                         aria-label="지역별 방제 완료 필지 비율">
                        <circle cx="70" cy="70" r="${radius}"
                                fill="none" stroke="#edf1f5" stroke-width="20"/>
                        ${segments}
                    </svg>
                    <div class="stats-donut-center">
                        <strong>${total}</strong>
                        <span>완료 필지</span>
                    </div>
                </div>
                <div class="stats-donut-legend">
                    ${legend}
                </div>
            </div>
        `;
    }

    function render() {
        const items = getFilteredHistory();

        renderSummary(items);
        renderTable(items);
        renderTrend(items);
        renderRegions(items);
    }

    $$(".stats-tab").forEach(button => {
        button.addEventListener("click", () => {
            currentTab = button.dataset.statsTab;

            $$(".stats-tab").forEach(tab => {
                const active = tab === button;
                tab.classList.toggle("active", active);
                tab.setAttribute("aria-pressed", String(active));
            });

            render();
        });
    });

    $("#stats-filter-form").addEventListener("submit", event => {
        event.preventDefault();

        const start = $("#stats-start-date").value;
        const end = $("#stats-end-date").value;

        if (start && end && start > end) {
            alert("종료일은 시작일보다 빠를 수 없습니다.");
            return;
        }

        render();
    });

    $("#stats-filter-form").addEventListener("reset", () => {
        setTimeout(render, 0);
    });

    ["#stats-region", "#stats-pilot", "#stats-start-date", "#stats-end-date"]
        .forEach(selector => $(selector).addEventListener("change", render));

    // UTF-8 BOM을 넣어 Excel에서 한글 CSV가 깨지는 문제를 줄임.
    $("#stats-export").addEventListener("click", () => {
        const items = getFilteredHistory();

        if (!items.length) {
            alert("다운로드할 이력이 없습니다.");
            return;
        }

        const headers = [
            "농지 주소", "지역", "담당 조종사",
            "배정 일시", "완료 일시", "방제 면적(㎡)", "상태"
        ];

        const rows = items.map(item => [
            item.address,
            item.region,
            item.pilot,
            item.assigned,
            item.completed,
            item.area,
            item.status
        ]);

        const csvCell = value =>
            `"${String(value ?? "").replace(/"/g, '""')}"`;

        const csv = "\uFEFF" +
            [headers, ...rows].map(row => row.map(csvCell).join(",")).join("\r\n");

        const blob = new Blob([csv], {
            type: "text/csv;charset=utf-8;"
        });

        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "방제_작업_이력_예시.csv";
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    });

    // 작업 이력 접기 / 펼치기
    const historyToggle = document.getElementById("stats-history-toggle");
    const historyContent = document.getElementById("stats-history-content");

    if (historyToggle && historyContent) {
        historyToggle.addEventListener("click", () => {
            const isExpanded = historyToggle.getAttribute("aria-expanded") === "true";

            historyContent.hidden = isExpanded;
            historyToggle.setAttribute("aria-expanded", String(!isExpanded));
            historyToggle.textContent = isExpanded ? "이력 조회" : "이력 접기";
        });
    }

    render();
})();
