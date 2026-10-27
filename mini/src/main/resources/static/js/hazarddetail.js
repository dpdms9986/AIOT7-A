
/* ===== 위험 상세 데이터 ===== */

const hazardData = {
    "3": {
        title: "논 주변 고압선 주의",
        type: "고압선",
        severity: "고위험",
        region: "대전 유성구 신성동",
        author: "김조종",
        date: "2026-10-22",
        description: "농지 주변에 고압선이 설치되어 있습니다.\n드론 비행 및 방제 작업 시 전선과의 접촉에 주의해야 합니다.",
        action: "작업 전 주변 전선의 위치를 확인하고 안전거리를 확보해 주세요."
    },
    "2": {
        title: "통신탑 근처 비행 주의",
        type: "통신탑",
        severity: "중위험",
        region: "대전 대덕구 송촌동",
        author: "이조종",
        date: "2026-10-21",
        description: "통신탑 주변에서는 전파 간섭 및 구조물 충돌 위험이 있습니다.",
        action: "비행 전 통신탑 위치와 주변 장애물을 확인하고 충분한 안전거리를 유지해 주세요."
    },
    "1": {
        title: "농지 진입로 벼락 주의",
        type: "진입로",
        severity: "저위험",
        region: "대전 동구 어남동",
        author: "박조종",
        date: "2026-10-20",
        description: "농지 진입로 주변의 차량과 작업자 이동에 주의해야 합니다.",
        action: "작업 전 진입로의 차량 통행과 작업자 이동 동선을 확인해 주세요."
    }
};

function renderHazardDetail() {
    const hazardId = new URLSearchParams(window.location.search).get("id");
    const hazard = hazardData[hazardId];

    if (!hazard) {
        console.warn("해당 ID의 위험 데이터가 없습니다:", hazardId);
        return;
    }

    document.getElementById("hazard-title").textContent = hazard.title;
    document.getElementById("hazard-type").textContent = hazard.type;
    document.getElementById("hazard-severity").textContent = hazard.severity;
    document.getElementById("hazard-region").textContent = hazard.region;
    document.getElementById("hazard-author").textContent = hazard.author;
    document.getElementById("hazard-date").textContent = hazard.date;
    document.getElementById("hazard-description").textContent = hazard.description;
    document.getElementById("hazard-action").textContent = hazard.action;

    const severityElement = document.getElementById("hazard-severity");

    severityElement.classList.remove("high", "medium", "low");

    const severityClasses = {
        "고위험": "high",
        "중위험": "medium",
        "저위험": "low"
    };

    severityElement.classList.add(severityClasses[hazard.severity]);
}

renderHazardDetail();


/* ===== 댓글 이미지 첨부 및 미리보기 ===== */

const commentImageInput = document.getElementById("hazard-comment-images");
const commentImagePreview = document.getElementById("hazard-comment-preview");
const commentImageCount = document.getElementById("hazard-image-count");

let commentImages = [];
const MAX_COMMENT_IMAGES = 5;

if (commentImageInput && commentImagePreview && commentImageCount) {
    commentImageInput.addEventListener("change", () => {
        const selectedFiles = Array.from(commentImageInput.files || []);
        const imageFiles = selectedFiles.filter(file =>
            file.type.startsWith("image/")
        );

        const remaining = MAX_COMMENT_IMAGES - commentImages.length;

        if (imageFiles.length > remaining) {
            alert("이미지는 최대 5개까지 첨부할 수 있습니다.");
        }

        commentImages.push(...imageFiles.slice(0, remaining));

        commentImageInput.value = "";
        renderCommentImagePreview();
    });
}

function renderCommentImagePreview() {
    if (!commentImagePreview || !commentImageCount) return;

    commentImagePreview.querySelectorAll("img").forEach(image => {
        if (image.dataset.previewUrl) {
            URL.revokeObjectURL(image.dataset.previewUrl);
        }
    });

    commentImagePreview.replaceChildren();

    commentImages.forEach((file, index) => {
        const item = document.createElement("div");
        item.className = "hazard-comment-preview-item";

        const image = document.createElement("img");
        const previewUrl = URL.createObjectURL(file);

        image.src = previewUrl;
        image.alt = file.name;
        image.dataset.previewUrl = previewUrl;

        const removeButton = document.createElement("button");
        removeButton.type = "button";
        removeButton.textContent = "첨부 취소";

        removeButton.addEventListener("click", () => {
            commentImages.splice(index, 1);
            renderCommentImagePreview();
        });

        item.append(image, removeButton);
        commentImagePreview.appendChild(item);
    });

    commentImageCount.textContent =
        `첨부 이미지 ${commentImages.length}개`;
}
