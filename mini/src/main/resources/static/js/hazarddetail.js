// 댓글 이미지 첨부 및 미리보기
const commentImageInput = document.getElementById("hazard-comment-images");
const commentImagePreview = document.getElementById("hazard-comment-preview");
const commentImageCount = document.getElementById("hazard-image-count");

let commentImages = [];

if (commentImageInput && commentImagePreview && commentImageCount) {
    commentImageInput.addEventListener("change", () => {
        const selectedFiles = Array.from(commentImageInput.files || []);
        const remaining = Math.max(0, 5 - commentImages.length);

        const imageFiles = selectedFiles.filter(file =>
            file.type.startsWith("image/")
        );

        commentImages = [
            ...commentImages,
            ...imageFiles.slice(0, remaining)
        ];

        if (imageFiles.length > remaining) {
            alert("이미지는 최대 5개까지 첨부할 수 있습니다.");
        }

        commentImageInput.value = "";
        renderCommentImagePreview();
    });
}

function renderCommentImagePreview() {
    if (!commentImagePreview || !commentImageCount) return;

    commentImagePreview.replaceChildren();

    commentImages.forEach((file, index) => {
        const item = document.createElement("div");
        item.className = "hazard-comment-preview-item";

        const image = document.createElement("img");
        image.src = URL.createObjectURL(file);
        image.alt = file.name;

        const removeButton = document.createElement("button");
        removeButton.type = "button";
        removeButton.textContent = "첨부 취소";

        removeButton.addEventListener("click", () => {
            URL.revokeObjectURL(image.src);
            commentImages.splice(index, 1);
            renderCommentImagePreview();
        });

        item.append(image, removeButton);
        commentImagePreview.appendChild(item);
    });

    commentImageCount.textContent = `첨부 이미지 ${commentImages.length}개`;
}