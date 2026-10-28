
(() => {
    "use strict";

    const $ = (selector) => document.querySelector(selector);

    const tbody = $("#farm-list .farm-table tbody");
    const count = $("#farm-count");
    const regionFilter = $("#region-filter");
    const searchInput = $("#farm-search");
    const searchButton = $("#search-button");
    const registerButton = $("#register-button");

    const drawer = $("#farm-detail-drawer");
    const backdrop = $("#farm-detail-backdrop");
    const closeButton = $("#close-farm-detail");

    if (!tbody || !drawer || !backdrop || !closeButton) return;

    let selectedFarm = null;
    let previousFocus = null;

    // 지도 관련 변수
    let farmDetailMap = null;
    let farmDetailMarker = null;

    const DEFAULT_MAP_POSITION = [36.3504, 127.3845];

    const rows = () =>
        [...tbody.querySelectorAll("tr[data-farm-id]")];

    function getFarm(id) {
        return rows().find(
            row => row.dataset.farmId === String(id)
        );
    }

    function getFarmData(row) {
        return {
            id: row.dataset.farmId || "",
            address: row.dataset.address || "",
            city: row.dataset.city || "",
            region: row.dataset.region || "",
            dong: row.dataset.dong || "",
            status: row.dataset.status || "대기",
            latitude: row.dataset.latitude || "",
            longitude: row.dataset.longitude || "",
            pilot: row.dataset.pilot || "미배정",
            assignedAt: row.dataset.assignedAt || "-"
        };
    }

    // 표의 5개 열 갱신
    function renderRow(row) {
        const data = getFarmData(row);
        const cells = row.querySelectorAll("td");

        if (cells.length < 5) return;

        cells[1].textContent = data.address;
        cells[2].textContent =
            `${data.region} ${data.dong}`.trim();

        const badge = cells[3].querySelector(".status-badge");

        if (badge) {
            badge.textContent = data.status;
        }
    }

    // 검색 및 행정구역 필터
    function applyFilters() {
        const dong = regionFilter?.value || "";
        const keyword = (searchInput?.value || "")
            .trim()
            .toLowerCase();

        let visible = 0;

        rows().forEach(row => {
            const data = getFarmData(row);

            const searchable = [
                data.address,
                data.city,
                data.region,
                data.dong,
                `${data.city} ${data.region} ${data.dong}`
            ].join(" ").toLowerCase();

            const matches =
                (!dong || data.dong === dong) &&
                (!keyword || searchable.includes(keyword));

            row.hidden = !matches;

            if (matches) visible++;
        });

        if (count) count.textContent = visible;
    }

    // Leaflet 지도 생성 및 선택한 농지 위치 표시
    function initializeFarmDetailMap(data) {
        const mapElement = $("#farm-detail-map");
        const statusElement = $("#farm-detail-map-status");

        if (!mapElement) return;

        if (typeof L === "undefined") {
            if (statusElement) {
                statusElement.textContent = "Leaflet 지도를 불러오지 못했습니다.";
            }
            return;
        }

        if (!farmDetailMap) {
            farmDetailMap = L.map(mapElement).setView(
                DEFAULT_MAP_POSITION,
                13
            );

            L.tileLayer(
                "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
                {
                    maxZoom: 19,
                    attribution: "&copy; OpenStreetMap contributors"
                }
            ).addTo(farmDetailMap);
        }

        const lat = Number(data.latitude);
        const lng = Number(data.longitude);

        const hasCoordinates =
            data.latitude.trim() !== "" &&
            data.longitude.trim() !== "" &&
            Number.isFinite(lat) &&
            Number.isFinite(lng) &&
            Math.abs(lat) <= 90 &&
            Math.abs(lng) <= 180;

        requestAnimationFrame(() => {
            if (!farmDetailMap) return;

            farmDetailMap.invalidateSize();

            if (farmDetailMarker) {
                farmDetailMap.removeLayer(farmDetailMarker);
                farmDetailMarker = null;
            }

            if (hasCoordinates) {
                farmDetailMap.setView([lat, lng], 16);

                farmDetailMarker = L.marker([lat, lng])
                    .addTo(farmDetailMap)
                    .bindPopup(data.address);

                if (statusElement) {
                    statusElement.textContent = "농지 위치 표시 중";
                }
            } else {
                farmDetailMap.setView(DEFAULT_MAP_POSITION, 13);

                if (statusElement) {
                    statusElement.textContent = "농지 좌표 미등록";
                }
            }
        });
    }

    // 오른쪽 상세 패널 열기
    function openDrawer(id, trigger) {
        const row = getFarm(id);
        if (!row) return;

        selectedFarm = row;
        previousFocus = trigger || document.activeElement;

        const data = getFarmData(row);

        $("#drawer-address").textContent = data.address;

        $("#drawer-region").textContent =
            `${data.city} ${data.region} ${data.dong}`.trim();

        $("#drawer-status").textContent = data.status;

        $("#drawer-id").textContent =
            `LAND-${data.id.padStart(4, "0")}`;

        const mapAddress = $("#drawer-map-address");

        if (mapAddress) {
            mapAddress.textContent =
                `${data.city} ${data.region} ${data.dong} ${data.address}`.trim();
        }

        $("#drawer-coordinates").textContent =
            data.latitude && data.longitude
                ? `${data.latitude}, ${data.longitude}`
                : "미등록";

        $("#drawer-pilot").textContent = data.pilot;
        $("#drawer-assigned-at").textContent = data.assignedAt;
        $("#drawer-work-status").textContent = data.status;

        drawer.hidden = false;
        backdrop.hidden = false;
        drawer.inert = false;
        drawer.setAttribute("aria-hidden", "false");

        document.body.classList.add("farm-drawer-open");

        requestAnimationFrame(() => {
            drawer.classList.add("is-open");
            closeButton.focus();

            // 패널이 열린 다음 지도 표시
            initializeFarmDetailMap(data);
        });
    }

    // 상세 패널 닫기
    function closeDrawer() {
        drawer.classList.remove("is-open");
        drawer.inert = true;
        drawer.setAttribute("aria-hidden", "true");
        backdrop.hidden = true;

        document.body.classList.remove("farm-drawer-open");

        const focusTarget = previousFocus;
        selectedFarm = null;

        window.setTimeout(() => {
            drawer.hidden = true;

            if (focusTarget?.isConnected) {
                focusTarget.focus();
            }
        }, 260);
    }

    function refreshSelectedFarm() {
        if (!selectedFarm) return;

        openDrawer(selectedFarm.dataset.farmId, previousFocus);
    }

    // 상세 버튼
    tbody.addEventListener("click", event => {
        const button = event.target.closest(".detail-button");
        if (!button) return;

        openDrawer(button.dataset.farmId, button);
    });

    closeButton.addEventListener("click", closeDrawer);
    backdrop.addEventListener("click", closeDrawer);

    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !drawer.hidden) {
            closeDrawer();
        }
    });

    // 검색
    searchButton?.addEventListener("click", applyFilters);

    searchInput?.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            applyFilters();
        }
    });

    regionFilter?.addEventListener("change", applyFilters);

    // 농지 주소 수정: 시연용
    $("#edit-farm-button")?.addEventListener("click", () => {
        if (!selectedFarm) return;

        const current = selectedFarm.dataset.address || "";
        const address = window.prompt(
            "농지 주소를 수정하세요.",
            current
        );

        if (address === null || !address.trim()) return;

        selectedFarm.dataset.address = address.trim();

        renderRow(selectedFarm);
        applyFilters();
        refreshSelectedFarm();
    });

    // 농지 삭제: 시연용
    $("#delete-farm-button")?.addEventListener("click", () => {
        if (!selectedFarm) return;

        if (selectedFarm.dataset.status === "작업 중") {
            window.alert("작업 중인 농지는 삭제할 수 없습니다.");
            return;
        }

        const address = selectedFarm.dataset.address;

        if (!window.confirm(`${address} 농지를 삭제하시겠습니까?`)) {
            return;
        }

        selectedFarm.remove();
        closeDrawer();
        applyFilters();
    });

    // 작업 배정: 시연용
    $("#assign-farm-button")?.addEventListener("click", () => {
        if (!selectedFarm) return;

        if (selectedFarm.dataset.status !== "대기") {
            window.alert("대기 상태인 농지만 배정할 수 있습니다.");
            return;
        }

        const pilot = window.prompt(
            "배정할 조종사 이름을 입력하세요."
        );

        if (!pilot || !pilot.trim()) return;

        selectedFarm.dataset.pilot = pilot.trim();
        selectedFarm.dataset.assignedAt =
            new Date().toLocaleString("ko-KR");
        selectedFarm.dataset.status = "배정됨";

        renderRow(selectedFarm);
        applyFilters();
        refreshSelectedFarm();

        window.alert(
            "시연용 목록에 배정 결과를 반영했습니다. 서버에는 저장되지 않았습니다."
        );
    });

    // 농지 등록: 미구현
    registerButton?.addEventListener("click", () => {
        window.alert("농지 등록 화면은 추후 연결이 필요합니다.");
    });

    // 초기 목록 표시
    if (searchInput) searchInput.value = "";
    if (regionFilter) regionFilter.value = "";

    applyFilters();
})();
