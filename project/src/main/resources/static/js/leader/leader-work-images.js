
/* =========================================
   단장 작업 이력 - 방제 작업 전·후 사진 조회
   leader-work-images.js
========================================= */

document.addEventListener("DOMContentLoaded", () => {

    const buttons = document.querySelectorAll(
        ".leader-work-image-detail-btn"
    );

    // 작업 사진 조회 API
    function getApiUrl(no) {
        return `/api/leader/task-assignment/${no}/images`;
    }

    // 모달 생성
    const modal = document.createElement("div");

    modal.id = "leader-work-image-modal";
    modal.className = "leader-work-image-modal";
    modal.hidden = true;

    modal.innerHTML = `
        <section class="leader-work-image-modal-box"
                 role="dialog"
                 aria-modal="true"
                 aria-labelledby="leader-work-image-title">

            <header class="leader-work-image-modal-header">

                <div>
                    <h3 id="leader-work-image-title">
                        방제 작업 사진
                    </h3>

                    <p>
                        조종사가 등록한 작업 전·후 사진입니다.
                    </p>
                </div>

                <button type="button"
                        class="leader-work-image-close"
                        aria-label="닫기">
                    ✕
                </button>

            </header>

            <div class="leader-work-image-modal-content"
                 id="leader-work-image-content">

                <p class="leader-work-image-loading">
                    사진을 불러오는 중입니다.
                </p>

            </div>

            <footer class="leader-work-image-modal-footer">

                <button type="button"
                        class="dash-btn-outline"
                        id="leader-work-image-confirm">
                    닫기
                </button>

            </footer>

        </section>
    `;

    document.body.appendChild(modal);

    const content = document.getElementById(
        "leader-work-image-content"
    );

    const closeButton = modal.querySelector(
        ".leader-work-image-close"
    );

    const confirmButton = document.getElementById(
        "leader-work-image-confirm"
    );

    // 이미지 확대 모달
    const lightbox = document.createElement("div");

    lightbox.className = "leader-work-image-lightbox";
    lightbox.hidden = true;

    lightbox.innerHTML = `
        <button type="button"
                class="leader-work-image-lightbox-close"
                aria-label="닫기">
            ✕
        </button>

        <img src="" alt="방제 작업 사진 확대">
    `;

    document.body.appendChild(lightbox);

    const lightboxImage = lightbox.querySelector("img");

    // 오류 안내
    function showError(message) {

        if (typeof window.showPopup === "function") {
            window.showPopup(message, "warn");
        } else {
            alert(message);
        }
    }

    // 모달 닫기
    function closeModal() {

        modal.hidden = true;
        document.body.style.overflow = "";

        content.innerHTML = "";
    }

    // 이미지 확대
    function openLightbox(src) {

        lightboxImage.src = src;
        lightbox.hidden = false;
    }

    // 이미지 확대 닫기
    function closeLightbox() {

        lightbox.hidden = true;
        lightboxImage.src = "";
    }

    // 이미지 그룹 생성
    function createImageGroup(title, images) {

        const group = document.createElement("section");

        group.className = "leader-work-image-group";

        const header = document.createElement("div");

        header.className = "leader-work-image-group-header";

        const heading = document.createElement("strong");
        heading.textContent = title;

        const count = document.createElement("span");
        count.textContent = `${images.length}장`;

        header.append(heading, count);

        const gallery = document.createElement("div");

        gallery.className = "leader-work-image-gallery";

        images.forEach((image, index) => {

            const item = document.createElement("button");

            item.type = "button";
            item.className = "leader-work-image-item";

            const img = document.createElement("img");

            img.src = image.filePath;
            img.alt = `${title} ${index + 1}`;
            img.loading = "lazy";

            item.appendChild(img);

            item.addEventListener("click", () => {
                openLightbox(image.filePath);
            });

            gallery.appendChild(item);
        });

        group.append(header, gallery);

        return group;
    }

    // 서버에서 가져온 사진 표시
    function renderImages(data) {

        content.innerHTML = "";

        const beforeImages = data.beforeImages || [];
        const afterImages = data.afterImages || [];

        const totalCount =
            beforeImages.length + afterImages.length;

        // 등록된 사진이 하나도 없는 경우
        if (totalCount === 0) {

            const empty = document.createElement("p");

            empty.className = "leader-work-image-empty";
            empty.textContent =
                "현재 등록된 사진이 없습니다.";

            content.appendChild(empty);

            return;
        }

        // 작업 전 사진
        if (beforeImages.length > 0) {

            content.appendChild(
                createImageGroup(
                    "작업 전 사진",
                    beforeImages
                )
            );
        }

        // 작업 후 사진
        if (afterImages.length > 0) {

            content.appendChild(
                createImageGroup(
                    "작업 후 사진",
                    afterImages
                )
            );
        }
    }

    // 사진 조회
    async function openModal(assignmentNo) {

        if (!assignmentNo) {
            showError("작업 번호를 확인할 수 없습니다.");
            return;
        }

        modal.hidden = false;
        document.body.style.overflow = "hidden";

        content.innerHTML = `
            <p class="leader-work-image-loading">
                사진을 불러오는 중입니다.
            </p>
        `;

        try {

            const response = await fetch(
                getApiUrl(assignmentNo),
                {
                    method: "GET",
                    headers: {
                        "Accept": "application/json"
                    },
                    credentials: "same-origin"
                }
            );

            const data = await response.json().catch(() => ({}));

            if (!response.ok) {

                throw new Error(
                    data.message || "작업 사진을 불러오지 못했습니다."
                );
            }

            renderImages(data);

        } catch (error) {

            console.error(
                "단장 작업 사진 조회 오류:",
                error
            );

            closeModal();

            showError(
                error.message || "작업 사진 조회에 실패했습니다."
            );
        }
    }

    // 각 작업의 상세 버튼
    buttons.forEach(button => {

        button.addEventListener("click", () => {

            const assignmentNo =
                button.dataset.assignmentNo;

            openModal(assignmentNo);
        });
    });

    // 모달 닫기
    closeButton.addEventListener("click", closeModal);

    confirmButton.addEventListener("click", closeModal);

    // 모달 바깥 클릭 시 닫기
    modal.addEventListener("click", event => {

        if (event.target === modal) {
            closeModal();
        }
    });

    // 이미지 확대 닫기
    lightbox.querySelector(
        ".leader-work-image-lightbox-close"
    ).addEventListener("click", closeLightbox);

    lightbox.addEventListener("click", event => {

        if (event.target === lightbox) {
            closeLightbox();
        }
    });

    // ESC 키
    document.addEventListener("keydown", event => {

        if (event.key === "Escape") {

            if (!lightbox.hidden) {
                closeLightbox();
            } else if (!modal.hidden) {
                closeModal();
            }
        }
    });

});
