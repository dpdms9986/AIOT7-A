
const hazardSearchForm = document.getElementById("hazard-search-form");
const hazardCategory = document.getElementById("hazard-category");
const hazardRegion = document.getElementById("hazard-region");
const hazardType = document.getElementById("hazard-type");
const hazardSeverityFilter = document.getElementById("hazard-severity-filter");
const hazardSearch = document.getElementById("hazard-search");
const hazardResetButton = document.getElementById("hazard-reset-button");
const hazardTbody = document.getElementById("hazard-tbody");
const hazardCount = document.getElementById("hazard-count");
const hazardResultCount = document.getElementById("hazard-result-count");
const hazardEmpty = document.getElementById("hazard-empty");

function filterHazards() {
    const category = hazardCategory?.value || "";
    const region = hazardRegion?.value || "";
    const type = hazardType?.value || "";
    const severity = hazardSeverityFilter?.value || "";
    const keyword = hazardSearch?.value.trim().toLowerCase() || "";

    let visibleCount = 0;

    hazardTbody.querySelectorAll("tr[data-hazard-id]").forEach(row => {
        const rowCategory = row.dataset.category || "hazard";
        const rowRegion = row.dataset.region || "";
        const rowType = row.dataset.type || "";
        const rowSeverity = row.dataset.severity || "";

        const matchesCategory =
            !category || rowCategory === category;

        const matchesRegion =
            !region || rowRegion.trim() === region;

        const matchesType =
            !type || rowType === type;

        const matchesSeverity =
            !severity || rowSeverity === severity;

        const searchableText = [
            row.dataset.title,
            row.dataset.content,
            row.dataset.writer,
            rowRegion,
            rowType
        ].join(" ").toLowerCase();

        const matchesKeyword =
            !keyword || searchableText.includes(keyword);

        const visible =
            matchesCategory &&
            matchesRegion &&
            matchesType &&
            matchesSeverity &&
            matchesKeyword;

        row.hidden = !visible;

        if (visible) {
            visibleCount++;
        }
    });

    if (hazardCount) {
        hazardCount.textContent = visibleCount;
    }

    if (hazardResultCount) {
        hazardResultCount.textContent = visibleCount;
    }

    if (hazardEmpty) {
        hazardEmpty.hidden = visibleCount !== 0;
    }
}

hazardSearchForm?.addEventListener("submit", event => {
    event.preventDefault();
    filterHazards();
});

hazardResetButton?.addEventListener("click", () => {
    if (hazardCategory) hazardCategory.value = "";
    if (hazardRegion) hazardRegion.value = "";
    if (hazardType) hazardType.value = "";
    if (hazardSeverityFilter) hazardSeverityFilter.value = "";
    if (hazardSearch) hazardSearch.value = "";


    filterHazards();
});

// 필터 변경 시 즉시 적용
[
    hazardCategory,
    hazardRegion,
    hazardType,
    hazardSeverityFilter
].forEach(element => {
    element?.addEventListener("change", filterHazards);
});

// 검색어 입력 후 Enter로 검색
hazardSearch?.addEventListener("input", filterHazards);

hazardTbody?.addEventListener("click", event => {
    const button = event.target.closest("[data-detail-id]");
    const row = event.target.closest("tr[data-hazard-id]");

    // 상세보기 버튼
    if (button) {
        openPostDetail(button.dataset.detailId);
        return;
    }

    // 표의 행 클릭
    if (row) {
        openPostDetail(row.dataset.hazardId);
    }
});

hazardTbody?.addEventListener("keydown", event => {
    if (event.key !== "Enter" && event.key !== " ") {
        return;
    }

    // 버튼 자체의 키보드 동작은 브라우저에 맡김
    if (event.target.closest("button")) {
        return;
    }

    const row = event.target.closest("tr[data-hazard-id]");

    if (!row) return;

    event.preventDefault();
    openPostDetail(row.dataset.hazardId);
});

function openPostDetail(id) {
    if (!id) return;

    const row = Array.from(
        hazardTbody.querySelectorAll("tr[data-hazard-id]")
    ).find(item => item.dataset.hazardId === id);

    if (!row) return;

    if (row.dataset.category === "notice") {
        window.location.href =
            `noticelistdetail.html?id=${encodeURIComponent(id.replace("notice-", ""))}`;
    } else {
        window.location.href =
            `hazarddetail.html?id=${encodeURIComponent(id)}`;
    }
}

document.getElementById("write-hazard")?.addEventListener("click", () => {
    alert("위험 요소 신고 화면은 추후 연결합니다.");
});

document.getElementById("notice-create-button")?.addEventListener("click", () => {
    alert("공지사항 작성 화면은 추후 연결합니다.");
});

filterHazards();
