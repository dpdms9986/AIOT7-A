
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
    // 공지사항 상세 페이지에서는 위험 데이터가 없어도 정상
    if (document.body.classList.contains("notice-detail-page")) {
        return;
    }

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

/* ===== 공지사항 상세 처리 ===== */

const noticeData = {
    "56": {
        title: "[기상특보] 10월 23일 강풍 주의보 발령",
        type: "긴급",
        pinned: true,
        author: "관리자",
        date: "2026-10-22 14:32",
        content:
            "기상청 예보에 따라 10월 23일 해당 지역에 강풍 주의보가 발령되었습니다. 안전한 방제 작업을 위해 아래 내용을 준수해 주시기 바랍니다.",
        alerts: [
            "10월 23일 전 지역 드론 비행 금지",
            "최대 풍속 5m/s 이상 예상",
            "현장 풍속 3m/s 이상 점검",
            "추가 변동 사항은 추후 공지 예정"
        ],
        attachment: "기상특보_강풍주의보.pdf",
        attachmentSize: "245KB"
    }
};

function renderNoticeDetail() {
    const noticeId = new URLSearchParams(location.search).get("id");

    // 공지 ID가 아니면 기존 위험 상세 로직을 사용
    if (!noticeId || !noticeData[noticeId]) {
        return;
    }

    const notice = noticeData[noticeId];

    const setText = (id, value) => {
        const element = document.getElementById(id);
        if (element) element.textContent = value;
    };

    setText("notice-title", notice.title);
    setText("notice-type", notice.type);
    setText("notice-author", notice.author);
    setText("notice-date", notice.date);
    setText("notice-content", notice.content);

    const pin = document.getElementById("notice-pin");
    if (pin) pin.hidden = !notice.pinned;

    const alertList = document.getElementById("notice-alert-list");

    if (alertList) {
        alertList.replaceChildren();

        notice.alerts.forEach(message => {
            const li = document.createElement("li");
            li.textContent = message;
            alertList.appendChild(li);
        });
    }

    const attachmentList =
        document.getElementById("notice-attachment-list");

    if (attachmentList) {
        attachmentList.replaceChildren();

        const item = document.createElement("div");
        item.className = "notice-attachment-item";

        const name = document.createElement("span");
        name.className = "notice-attachment-name";
        name.textContent = notice.attachment;

        const size = document.createElement("span");
        size.className = "notice-attachment-size";
        size.textContent = `(${notice.attachmentSize})`;

        item.append(name, size);
        attachmentList.appendChild(item);
    }
}

renderNoticeDetail();