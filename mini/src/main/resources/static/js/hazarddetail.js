
/* ===== 위험 상세 (서버에서 조회) ===== */
// 백엔드 주소에 맞게 수정: GET /api/hazards/{id}
// 응답 예: { title, type, severity, region, author, date, description, action }

const hazardApi = (id) => `/api/hazards/${encodeURIComponent(id)}`;

// 위험도: 서버가 HIGH/MEDIUM/LOW 로 줘도, 고위험/중위험/저위험 으로 줘도 처리
const SEVERITY_TEXT = { HIGH: "고위험", MEDIUM: "중위험", LOW: "저위험" };
const SEVERITY_CLASS = { "고위험": "high", "중위험": "medium", "저위험": "low" };

function setDetailText(id, value) {
    const element = document.getElementById(id);
    if (element) element.textContent = value ?? "-";
}

async function renderHazardDetail() {
    // 공지사항 상세 화면이면 위험 상세는 건너뜀
    if (document.body.classList.contains("notice-detail-page")) return;

    const hazardId = new URLSearchParams(location.search).get("id");
    if (!hazardId) return;

    try {
        const res = await fetch(hazardApi(hazardId));
        if (!res.ok) throw new Error(`서버 응답 ${res.status}`);
        const hazard = await res.json();

        const severity = SEVERITY_TEXT[hazard.severity] || hazard.severity || "-";

        setDetailText("hazard-title", hazard.title);
        setDetailText("hazard-type", hazard.type);
        setDetailText("hazard-severity", severity);
        setDetailText("hazard-region", hazard.region);
        setDetailText("hazard-author", hazard.author);
        setDetailText("hazard-date", hazard.date);
        setDetailText("hazard-description", hazard.description);
        setDetailText("hazard-action", hazard.action);

        const severityElement = document.getElementById("hazard-severity");
        if (severityElement) {
            severityElement.classList.remove("high", "medium", "low");
            if (SEVERITY_CLASS[severity]) severityElement.classList.add(SEVERITY_CLASS[severity]);
        }
    } catch (error) {
        console.error("위험 상세 조회 실패:", error);
        setDetailText("hazard-title", "게시글을 불러오지 못했습니다.");
    }
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

/* ===== 공지사항 상세 (서버에서 조회) ===== */
// 백엔드 주소에 맞게 수정: GET /api/notices/{id}
// 응답 예: { title, type, pinned, author, date, content, alerts: [], attachment, attachmentSize }

const noticeApi = (id) => `/api/notices/${encodeURIComponent(id)}`;

async function renderNoticeDetail() {
    // 공지사항 상세 화면에서만 실행
    if (!document.body.classList.contains("notice-detail-page")) return;

    const noticeId = new URLSearchParams(location.search).get("id");
    if (!noticeId) return;

    let notice;
    try {
        const res = await fetch(noticeApi(noticeId));
        if (!res.ok) throw new Error(`서버 응답 ${res.status}`);
        notice = await res.json();
    } catch (error) {
        console.error("공지사항 조회 실패:", error);
        setDetailText("notice-title", "공지사항을 불러오지 못했습니다.");
        return;
    }

    setDetailText("notice-title", notice.title);
    setDetailText("notice-type", notice.type);
    setDetailText("notice-author", notice.author);
    setDetailText("notice-date", notice.date);
    setDetailText("notice-content", notice.content);

    const pin = document.getElementById("notice-pin");
    if (pin) pin.hidden = !notice.pinned;

    const alertList = document.getElementById("notice-alert-list");
    if (alertList) {
        alertList.replaceChildren();
        (notice.alerts || []).forEach((message) => {
            const li = document.createElement("li");
            li.textContent = message;
            alertList.appendChild(li);
        });
    }

    const attachmentList = document.getElementById("notice-attachment-list");
    if (attachmentList) {
        attachmentList.replaceChildren();
        if (notice.attachment) {
            const item = document.createElement("div");
            item.className = "notice-attachment-item";

            const name = document.createElement("span");
            name.className = "notice-attachment-name";
            name.textContent = notice.attachment;

            const size = document.createElement("span");
            size.className = "notice-attachment-size";
            size.textContent = notice.attachmentSize ? `(${notice.attachmentSize})` : "";

            item.append(name, size);
            attachmentList.appendChild(item);
        }
    }
}

renderNoticeDetail();