
const hazardSearchForm = document.getElementById("hazard-search-form");
const hazardRegion = document.getElementById("hazard-region");
const hazardType = document.getElementById("hazard-type");
const hazardSearch = document.getElementById("hazard-search");
const hazardResetButton = document.getElementById("hazard-reset-button");
const hazardTbody = document.getElementById("hazard-tbody");
const hazardCount = document.getElementById("hazard-count");
const hazardResultCount = document.getElementById("hazard-result-count");
const hazardEmpty = document.getElementById("hazard-empty");

function filterHazards() {
    const region = hazardRegion.value;
    const type = hazardType.value;
    const keyword = hazardSearch.value.trim().toLowerCase();

    let visibleCount = 0;

    hazardTbody.querySelectorAll("tr").forEach(row => {
        const matchesRegion =
            !region || row.dataset.region === region;

        const matchesType =
            !type || row.dataset.type === type;

        const searchableText = [
            row.dataset.title,
            row.dataset.content,
            row.dataset.writer,
            row.dataset.region,
            row.dataset.type
        ].join(" ").toLowerCase();

        const matchesKeyword =
            !keyword || searchableText.includes(keyword);

        const visible =
            matchesRegion && matchesType && matchesKeyword;

        row.hidden = !visible;

        if (visible) {
            visibleCount++;
        }
    });

    hazardCount.textContent = visibleCount;
    hazardResultCount.textContent = visibleCount;
    hazardEmpty.hidden = visibleCount !== 0;
}

hazardSearchForm.addEventListener("submit", event => {
    event.preventDefault();
    filterHazards();
});

hazardResetButton.addEventListener("click", () => {
    hazardRegion.value = "";
    hazardType.value = "";
    hazardSearch.value = "";
    filterHazards();
});

hazardTbody.addEventListener("click", event => {
    const row = event.target.closest("tr[data-hazard-id]");

    if (!row) {
        return;
    }

    window.location.href =
        `hazarddetail.html?id=${encodeURIComponent(row.dataset.hazardId)}`;
});

hazardTbody.addEventListener("keydown", event => {
    if (event.key !== "Enter" && event.key !== " ") {
        return;
    }

    const row = event.target.closest("tr[data-hazard-id]");

    if (!row) {
        return;
    }

    event.preventDefault();

    window.location.href =
        `hazarddetail.html?id=${encodeURIComponent(row.dataset.hazardId)}`;
});

document.getElementById("write-hazard")
    .addEventListener("click", () => {
        alert("위험 요소 신고 화면은 추후 연결합니다.");
    });

filterHazards();
