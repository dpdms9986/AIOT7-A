
/* =========================================
   조종사 작업 전·후 사진 관리
   pilot-work-images.js
========================================= */

document.addEventListener("DOMContentLoaded", () => {

    const section = document.getElementById("pilot-work-image-section");
    const modal = document.getElementById("pilot-image-modal");

    const $ = (id) => document.getElementById(id);

    const MAX_IMAGES = 4;

    // 목록의 작업별 사진 등록 여부 조회
    const imageButtons = document.querySelectorAll(
        ".pilot-image-register-btn"
    );

    // API URL 생성
    function getApiUrl(no) {
        return `/api/pilot/assignments/${no}/images`;
    }

    // CSRF 정보
    function getCsrfHeaders() {

        const token = document
            .querySelector('meta[name="_csrf"]')?.content;

        const header = document
            .querySelector('meta[name="_csrf_header"]')?.content;

        if (!token || !header) return {};

        return {
            [header]: token
        };
    }

    // 서버 이미지 데이터 확인
    function hasImages(data) {

        const before = data.beforeImages || [];
        const after = data.afterImages || [];

        return before.length + after.length > 0;
    }

    // 목록의 사진 등록 / 수정 문구 갱신
    async function updateListImageButtons() {

        for (const button of imageButtons) {

            const no = button.dataset.assignmentNo;

            if (!no) continue;

            try {

                const response = await fetch(getApiUrl(no), {
                    method: "GET",
                    headers: {
                        "Accept": "application/json"
                    },
                    credentials: "same-origin"
                });

                if (!response.ok) {
                    continue;
                }

                const data = await response.json();

                // 사진이 하나라도 등록되어 있으면 목록의 등록 버튼 숨김
                button.hidden = hasImages(data);

            } catch (error) {
                console.error(
                    `작업 ${no} 사진 여부 조회 오류:`,
                    error
                );
            }
        }
    }

    // 선택된 작업이 없거나 사진 모달이 없는 페이지라면
    // 목록의 버튼 문구만 갱신하고 종료
    if (!section || !modal) {
        updateListImageButtons();
        return;
    }

    const assignmentNo = section.dataset.assignmentNo;
    const API_URL = getApiUrl(assignmentNo);

    const beforeInput = $("pilot-before-images");
    const afterInput = $("pilot-after-images");

    const beforePreview = $("pilot-before-preview");
    const afterPreview = $("pilot-after-preview");

    // 상세 화면 전용 요소
    const beforeGallery = $("detail-before-image-gallery");
    const afterGallery = $("detail-after-image-gallery");

    const beforeGroup = $("detail-before-image-group");
    const afterGroup = $("detail-after-image-group");

    const emptyMessage = $("pilot-work-image-empty");
    const actions = $("pilot-work-image-actions");

    // 모달 전용 개수 표시
    const modalBeforeCount = $("modal-before-image-count");
    const modalAfterCount = $("modal-after-image-count");

    // 상세 화면 전용 개수 표시
    const detailBeforeCount = $("detail-before-image-count");
    const detailAfterCount = $("detail-after-image-count");

    const submitButton = $("pilot-image-submit-btn");
    const modalTitle = $("pilot-image-modal-title");

    let beforeImages = [];
    let afterImages = [];

    let hasRegisteredImages = false;

    // 이미지 형식 및 크기 관련 제한은 서버 검증을 따름
    // 브라우저별 JFIF MIME 타입 차이를 고려해 image/* 허용

    // 서버에 등록된 이미지인지 확인
    function isExisting(image) {
        return image.kind === "existing";
    }

    // 삭제되지 않은 이미지인지 확인
    function isActive(image) {
        return !image.deleted;
    }

    // 실제 사용 중인 이미지 개수
    function getActiveCount(images) {
        return images.filter(isActive).length;
    }

    // 이미지 파일명
    function getFileName(image) {

        if (isExisting(image)) {
            return image.data.originName || "작업 사진";
        }

        return image.file?.name || "작업 사진";
    }

    // 이미지 URL
    function getImageUrl(image) {

        if (isExisting(image)) {
            return image.data.filePath;
        }

        return image.previewUrl;
    }

    // 사진 개수 갱신
    function updateCounts() {

        const beforeCount = getActiveCount(beforeImages);
        const afterCount = getActiveCount(afterImages);

        if (modalBeforeCount) {
            modalBeforeCount.textContent =
                `${beforeCount} / ${MAX_IMAGES}`;
        }

        if (modalAfterCount) {
            modalAfterCount.textContent =
                `${afterCount} / ${MAX_IMAGES}`;
        }

        if (detailBeforeCount) {
            detailBeforeCount.textContent =
                `${beforeCount} / ${MAX_IMAGES}`;
        }

        if (detailAfterCount) {
            detailAfterCount.textContent =
                `${afterCount} / ${MAX_IMAGES}`;
        }
    }

    // 이미지 개수 초과 안내
    function showImageLimitNotice(type) {

        const message = type === "BEFORE"
            ? "작업 전 사진은 최대 4장까지 등록할 수 있습니다."
            : "작업 후 사진은 최대 4장까지 등록할 수 있습니다.";

        // 프로젝트 공통 안내 팝업 사용
        showPopup(message, "warn");
    }

    // 사진 미리보기 생성
    function createPreviewItem(image, type, index) {

        const item = document.createElement("div");
        item.className = "pilot-image-preview-item";

        // 삭제 선택된 기존 사진 표시
        if (image.deleted) {
            item.classList.add("is-delete");
        }

        const img = document.createElement("img");
        img.src = getImageUrl(image);
        img.alt = getFileName(image);

        const removeButton = document.createElement("button");
        removeButton.type = "button";
        removeButton.className = "pilot-image-preview-remove";

        if (isExisting(image)) {

            removeButton.textContent = image.deleted ? "↩" : "×";

            removeButton.setAttribute(
                "aria-label",
                image.deleted ? "삭제 취소" : "사진 삭제"
            );

        } else {

            removeButton.textContent = "×";
            removeButton.setAttribute("aria-label", "사진 삭제");
        }

        removeButton.addEventListener("click", () => {

            const images =
                type === "BEFORE" ? beforeImages : afterImages;

            const target = images[index];

            if (!target) return;

            if (isExisting(target)) {

                if (target.deleted) {

                    // 삭제 취소 후 개수 검사
                    const restoredCount = getActiveCount(images) + 1;

                    if (restoredCount > MAX_IMAGES) {
                        showImageLimitNotice(type);
                        return;
                    }

                    target.deleted = false;

                } else {

                    // 기존 사진 삭제 선택
                    target.deleted = true;
                }

            } else {

                // 새로 선택한 사진 제거
                URL.revokeObjectURL(target.previewUrl);
                images.splice(index, 1);
            }

            renderModalPreviews();
        });

        item.appendChild(img);
        item.appendChild(removeButton);

        return item;
    }

    // 모달 사진 미리보기 갱신
    function renderModalPreviews() {

        beforePreview.innerHTML = "";
        afterPreview.innerHTML = "";

        beforeImages.forEach((image, index) => {

            beforePreview.appendChild(
                createPreviewItem(image, "BEFORE", index)
            );
        });

        afterImages.forEach((image, index) => {

            afterPreview.appendChild(
                createPreviewItem(image, "AFTER", index)
            );
        });

        updateCounts();
    }

    // 상세 화면 사진 갤러리 생성
    function renderGallery(gallery, images) {

        gallery.innerHTML = "";

        images.forEach(image => {

            const item = document.createElement("div");
            item.className = "pilot-work-image-item";

            const img = document.createElement("img");
            img.src = image.filePath;
            img.alt = image.originName || "작업 사진";
            img.loading = "lazy";

            item.appendChild(img);

            item.addEventListener("click", () => {
                openLightbox(image.filePath);
            });

            gallery.appendChild(item);
        });
    }

    // 상세 화면 사진 영역 갱신
    function renderGallerySection() {

        const activeBefore = beforeImages
            .filter(isActive)
            .map(image => image.data);

        const activeAfter = afterImages
            .filter(isActive)
            .map(image => image.data);

        renderGallery(beforeGallery, activeBefore);
        renderGallery(afterGallery, activeAfter);

        const beforeCount = activeBefore.length;
        const afterCount = activeAfter.length;

        const totalCount = beforeCount + afterCount;

        // 사진이 없는 그룹은 숨김
        beforeGroup.hidden = beforeCount === 0;
        afterGroup.hidden = afterCount === 0;

        // 사진이 하나도 없으면 안내 문구만 표시
        emptyMessage.hidden = totalCount > 0;

        // 사진이 있을 때만 수정 버튼 표시
        actions.hidden = totalCount === 0;

        updateCounts();

        // 사진이 등록되어 있으면 목록의 사진 등록 버튼 숨김
        imageButtons.forEach(button => {

            if (button.dataset.assignmentNo !== assignmentNo) {
                return;
            }

            button.hidden = totalCount > 0;
        });
    }

    // API에서 사진 조회
    async function loadImages() {

        try {

            const response = await fetch(API_URL, {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                },
                credentials: "same-origin"
            });

            if (!response.ok) {
                throw new Error("작업 사진 조회에 실패했습니다.");
            }

            const data = await response.json();

            beforeImages = (data.beforeImages || []).map(image => ({
                kind: "existing",
                data: image,
                deleted: false
            }));

            afterImages = (data.afterImages || []).map(image => ({
                kind: "existing",
                data: image,
                deleted: false
            }));

            hasRegisteredImages =
                beforeImages.length + afterImages.length > 0;

            renderGallerySection();

            return true;

        } catch (error) {

            console.error("작업 사진 조회 오류:", error);

            showPopup(
                error.message || "사진을 불러오지 못했습니다.",
                "warn"
            );

            return false;
        }
    }

    // 새 이미지 선택
    function handleFileSelect(event, images, type) {

        const selectedFiles = Array.from(
            event.target.files || []
        );

        // 같은 파일을 다시 선택할 수 있도록 초기화
        event.target.value = "";

        if (selectedFiles.length === 0) return;

        let exceeded = false;

        for (const file of selectedFiles) {

            // 이미지 파일인지 확인
            if (!file.type.startsWith("image/")) {
                continue;
            }

            // 이미 선택된 동일 파일 중복 방지
            const alreadyExists = images.some(image => {

                if (!isActive(image) || isExisting(image)) {
                    return false;
                }

                return image.file.name === file.name
                    && image.file.size === file.size
                    && image.file.lastModified === file.lastModified;
            });

            if (alreadyExists) {
                continue;
            }

            // 기존 사진 + 새 사진 합산 개수 검사
            if (getActiveCount(images) >= MAX_IMAGES) {
                exceeded = true;
                continue;
            }

            const previewUrl = URL.createObjectURL(file);

            images.push({
                kind: "new",
                file: file,
                previewUrl: previewUrl,
                deleted: false
            });
        }

        if (exceeded) {
            showImageLimitNotice(type);
        }

        renderModalPreviews();
    }

    beforeInput.addEventListener("change", (event) => {
        handleFileSelect(event, beforeImages, "BEFORE");
    });

    afterInput.addEventListener("change", (event) => {
        handleFileSelect(event, afterImages, "AFTER");
    });

    // 모달 열기
    async function openModal() {

        const loaded = await loadImages();

        if (!loaded) return;

        const hasImages =
            beforeImages.length + afterImages.length > 0;

        modalTitle.textContent =
            hasImages ? "작업 사진 수정" : "작업 사진 등록";

        submitButton.textContent =
            hasImages ? "사진 수정" : "사진 등록";

        renderModalPreviews();

        modal.hidden = false;
        document.body.style.overflow = "hidden";
    }

    // 새로 선택한 이미지 미리보기 URL 정리
    function releaseNewImageUrls(images) {

        images.forEach(image => {

            if (!isExisting(image) && image.previewUrl) {
                URL.revokeObjectURL(image.previewUrl);
            }
        });
    }

    // 모달 닫기
    function closeModal() {

        modal.hidden = true;
        document.body.style.overflow = "";

        releaseNewImageUrls(beforeImages);
        releaseNewImageUrls(afterImages);

        beforeImages = [];
        afterImages = [];

        beforeInput.value = "";
        afterInput.value = "";
    }

    $("pilot-image-modal-close").addEventListener(
        "click",
        closeModal
    );

    $("pilot-image-cancel-btn").addEventListener(
        "click",
        closeModal
    );

    modal.addEventListener("click", (event) => {

        if (event.target === modal) {
            closeModal();
        }
    });

    // 목록의 사진 등록 / 수정 버튼
    imageButtons.forEach(button => {

        button.addEventListener("click", async () => {

            const no = button.dataset.assignmentNo;

            if (no !== assignmentNo) {

                const url = new URL(window.location.href);

                url.searchParams.set("filter", "done");
                url.searchParams.set("no", no);
                url.searchParams.set("openImages", "1");

                window.location.href = url.toString();
                return;
            }

            await openModal();
        });
    });

    // 상세 영역의 사진 수정 버튼
    $("pilot-image-edit-btn")?.addEventListener(
        "click",
        async () => {
            await openModal();
        }
    );

    // 기존 서버 사진을 File로 변환
    async function convertExistingToFile(image) {

        const response = await fetch(image.data.filePath, {
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error("기존 사진을 불러오지 못했습니다.");
        }

        const blob = await response.blob();

        return new File(
            [blob],
            image.data.originName || "existing-image.jpg",
            {
                type: blob.type || "image/jpeg"
            }
        );
    }

    // FormData 생성
    async function createFormData() {

        const formData = new FormData();

        // 작업 전 사진
        for (const image of beforeImages) {

            if (image.deleted) continue;

            const file = isExisting(image)
                ? await convertExistingToFile(image)
                : image.file;

            formData.append("beforeImages", file);
        }

        // 작업 후 사진
        for (const image of afterImages) {

            if (image.deleted) continue;

            const file = isExisting(image)
                ? await convertExistingToFile(image)
                : image.file;

            formData.append("afterImages", file);
        }

        return formData;
    }

    // 업로드 및 수정
    $("pilot-image-form").addEventListener(
        "submit",
        async (event) => {

            event.preventDefault();

            const beforeCount = getActiveCount(beforeImages);
            const afterCount = getActiveCount(afterImages);

            if (beforeCount > MAX_IMAGES || afterCount > MAX_IMAGES) {
                showImageLimitNotice(
                    beforeCount > MAX_IMAGES ? "BEFORE" : "AFTER"
                );
                return;
            }

            const totalCount = beforeCount + afterCount;

            if (totalCount === 0 && !hasRegisteredImages) {
                showPopup("등록할 사진을 선택해 주세요.", "warn");
                return;
            }

            const editMode = hasRegisteredImages;

            submitButton.disabled = true;
            submitButton.textContent = "저장 중...";

            try {

                const formData = await createFormData();

                const response = await fetch(API_URL, {
                    method: editMode ? "PUT" : "POST",
                    headers: getCsrfHeaders(),
                    body: formData,
                    credentials: "same-origin"
                });

                const data = await response.json().catch(() => ({}));

                if (!response.ok) {
                    throw new Error(
                        data.message || "사진 저장에 실패했습니다."
                    );
                }

                showPopup(
                    editMode
                        ? "작업 사진이 수정되었습니다."
                        : "작업 사진이 등록되었습니다.",
                    "ok"
                );

                closeModal();

                await loadImages();

                // 목록의 등록 / 수정 문구도 다시 확인
                updateListImageButtons();

            } catch (error) {

                console.error("작업 사진 저장 오류:", error);

                showPopup(
                    error.message || "사진 저장에 실패했습니다.",
                    "warn"
                );

            } finally {

                submitButton.disabled = false;

                submitButton.textContent =
                    hasRegisteredImages ? "사진 수정" : "사진 등록";
            }
        }
    );

    // 이미지 확대
    const lightbox = $("pilot-image-lightbox");
    const lightboxImg = $("pilot-image-lightbox-img");

    function openLightbox(src) {

        lightboxImg.src = src;
        lightbox.hidden = false;
        document.body.style.overflow = "hidden";
    }

    function closeLightbox() {

        lightbox.hidden = true;
        lightboxImg.src = "";

        if (modal.hidden) {
            document.body.style.overflow = "";
        }
    }

    $("pilot-image-lightbox-close").addEventListener(
        "click",
        closeLightbox
    );

    lightbox.addEventListener("click", (event) => {

        if (event.target === lightbox) {
            closeLightbox();
        }
    });

    // ESC 키
    document.addEventListener("keydown", (event) => {

        if (event.key === "Escape") {

            if (!lightbox.hidden) {
                closeLightbox();
            } else if (!modal.hidden) {
                closeModal();
            }
        }
    });

    // 페이지 최초 진입 시 기존 사진 조회
    loadImages().then(() => {

        const params = new URLSearchParams(window.location.search);

        if (params.get("openImages") === "1") {

            params.delete("openImages");

            const url = new URL(window.location.href);
            url.search = params.toString();

            window.history.replaceState({}, "", url);

            openModal();
        }
    });

    // 목록 전체의 사진 등록 / 수정 버튼 문구 갱신
    updateListImageButtons();

});
