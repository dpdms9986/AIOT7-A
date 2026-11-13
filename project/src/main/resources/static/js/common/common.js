function logout() {
    // /logout?redirect={현재경로}
    var currentPath = window.location.pathname;
    window.location.href = '/logout?redirect=' + encodeURIComponent(currentPath);
}


// ========================================
// 공통 오버레이 스크롤바
// 페이지 / 모달 / 팝업 / 표 / 가로 메뉴
// ========================================
(() => {
    const records = new Map();
    const hideTimers = new WeakMap();
    const root = document.scrollingElement || document.documentElement;

    // 스크롤바 DOM 생성
    function createBar(element, direction) {
        const track = document.createElement('div');
        track.className = `overlay-scrollbar ${direction}`;

        // 헤더 메뉴 전용 스크롤바 구분
        if (element.matches('.dash-topnav')) {
            track.classList.add('dash-topnav-scrollbar');
        }

        const thumb = document.createElement('div');
        thumb.className = 'overlay-scrollbar-thumb';

        track.appendChild(thumb);
        document.body.appendChild(track);

        const record = records.get(element);
        
        // 가로 스크롤바 트랙 클릭 시 해당 위치로 이동
        if (direction === 'horizontal') {
            track.addEventListener('pointerdown', (event) => {
                if (event.target === thumb) return;

                const rect = track.getBoundingClientRect();
                const clickPosition = event.clientX - rect.left;

                const maxScroll = element.scrollWidth - element.clientWidth;
                const thumbSize = thumb.offsetWidth;
                const movableSize = track.clientWidth - thumbSize;

                if (movableSize <= 0) return;

                const position = Math.max(
                    0,
                    Math.min(clickPosition - thumbSize / 2, movableSize)
                );

                element.scrollLeft = position / movableSize * maxScroll;
                updateElement(element);
            });
        }

        thumb.addEventListener('pointerdown', (event) => {
            event.preventDefault();
            event.stopPropagation();

            const vertical = direction === 'vertical';

            record.drag = {
                direction,
                startPosition: vertical ? event.clientY : event.clientX,
                startScroll: vertical ? element.scrollTop : element.scrollLeft
            };

            thumb.classList.add('dragging');
            showBars(element);
        });

        window.addEventListener('pointermove', (event) => {
            if (!record.drag || record.drag.direction !== direction) return;

            const vertical = direction === 'vertical';
            const currentPosition = vertical ? event.clientY : event.clientX;
            const delta = currentPosition - record.drag.startPosition;

            const scrollSize = vertical
                ? element.scrollHeight - element.clientHeight
                : element.scrollWidth - element.clientWidth;

            const trackSize = vertical
                ? track.clientHeight
                : track.clientWidth;

            const thumbSize = vertical
                ? thumb.offsetHeight
                : thumb.offsetWidth;

            const movableSize = trackSize - thumbSize;

            if (movableSize <= 0) return;

            const scrollDelta = delta * scrollSize / movableSize;

            if (vertical) {
                element.scrollTop = record.drag.startScroll + scrollDelta;
            } else {
                element.scrollLeft = record.drag.startScroll + scrollDelta;
            }
        });

        window.addEventListener('pointerup', () => {
            if (!record.drag || record.drag.direction !== direction) return;

            record.drag = null;
            thumb.classList.remove('dragging');
        });

        return { track, thumb };
    }

    // 스크롤바 표시
    function showBars(element) {
        const record = records.get(element);
        if (!record) return;

        [record.vertical, record.horizontal].forEach(bar => {
            if (bar) bar.track.classList.add('is-visible');
        });

        clearTimeout(hideTimers.get(element));

        const timer = setTimeout(() => {
            [record.vertical, record.horizontal].forEach(bar => {
                if (bar) bar.track.classList.remove('is-visible');
            });
            hideTimers.delete(element);
        }, 800);

        hideTimers.set(element, timer);
    }

    // 스크롤바 위치 및 크기 갱신
    function updateBar(element, direction) {
        const record = records.get(element);
        if (!record) return;

        const bar = direction === 'vertical'
            ? record.vertical
            : record.horizontal;

        if (!bar) return;

        const isVertical = direction === 'vertical';
        const rect = element === root
            ? {
                top: 0,
                left: 0,
                right: window.innerWidth,
                bottom: window.innerHeight,
                width: window.innerWidth,
                height: window.innerHeight
            }
            : element.getBoundingClientRect();

        const viewportSize = isVertical ? rect.height : rect.width;
        const contentSize = isVertical
            ? element.scrollHeight
            : element.scrollWidth;

        const clientSize = isVertical
            ? element.clientHeight
            : element.clientWidth;

        const scrollPosition = isVertical
            ? element.scrollTop
            : element.scrollLeft;

        const trackSize = Math.max(0, viewportSize - 6);

        const thumbSize = Math.max(
            24,
            trackSize * clientSize / contentSize
        );

        const movableSize = Math.max(0, trackSize - thumbSize);
        const maxScroll = Math.max(1, contentSize - clientSize);

        const thumbPosition = movableSize * scrollPosition / maxScroll;

        if (isVertical) {
            bar.track.style.left = `${rect.right - 8}px`;
            bar.track.style.top = `${rect.top + 3}px`;
            bar.track.style.width = '8px';
            bar.track.style.height = `${trackSize}px`;

            bar.thumb.style.width = '5px';
            bar.thumb.style.height = `${Math.min(thumbSize, trackSize)}px`;
            bar.thumb.style.transform =
                `translate(1.5px, ${thumbPosition}px)`;
        } else {
            bar.track.style.left = `${rect.left + 3}px`;
            bar.track.style.top = `${rect.bottom - 8}px`;
            bar.track.style.width = `${trackSize}px`;
            bar.track.style.height = '8px';

            bar.thumb.style.width = `${Math.min(thumbSize, trackSize)}px`;
            bar.thumb.style.height = '5px';
            bar.thumb.style.transform =
                `translate(${thumbPosition}px, 1.5px)`;
        }

        const visible = rect.bottom > 0 &&
            rect.top < window.innerHeight &&
            rect.right > 0 &&
            rect.left < window.innerWidth;

        bar.track.style.display = visible ? 'block' : 'none';
    }

    function updateElement(element) {
        const record = records.get(element);
        if (!record) return;

        const vertical = element.scrollHeight > element.clientHeight + 1;
        const horizontal = element.scrollWidth > element.clientWidth + 1;

        if (vertical && !record.vertical) {
            record.vertical = createBar(element, 'vertical');
        } else if (!vertical && record.vertical) {
            record.vertical.track.remove();
            record.vertical = null;
        }

        if (horizontal && !record.horizontal) {
            record.horizontal = createBar(element, 'horizontal');
        } else if (!horizontal && record.horizontal) {
            record.horizontal.track.remove();
            record.horizontal = null;
        }

        updateBar(element, 'vertical');
        updateBar(element, 'horizontal');
    }

    // 스크롤 가능한 요소인지 확인
    function isScrollable(element) {
        if (element === root) {
            return element.scrollHeight > element.clientHeight ||
                   element.scrollWidth > element.clientWidth;
        }

        const style = getComputedStyle(element);
        const allowsVertical = ['auto', 'scroll', 'overlay'].includes(style.overflowY);
        const allowsHorizontal = ['auto', 'scroll', 'overlay'].includes(style.overflowX);

        return (
            (allowsVertical && element.scrollHeight > element.clientHeight + 1) ||
            (allowsHorizontal && element.scrollWidth > element.clientWidth + 1)
        );
    }

    // 스크롤 가능한 요소 등록
    function registerElement(element) {
        if (!element || records.has(element)) return;

        const record = {
            vertical: null,
            horizontal: null,
            drag: null
        };

        records.set(element, record);

        element.addEventListener('scroll', () => {
            updateElement(element);
            showBars(element);
        }, { passive: true });

        resizeObserver.observe(element);
        updateElement(element);
    }

    // 전체 요소 검색
    function scanElements() {
        registerElement(root);

        document.querySelectorAll('*').forEach(element => {
            if (isScrollable(element)) {
                registerElement(element);
            }
        });

        // DOM에서 제거된 요소 정리
        for (const [element, record] of records) {
            if (!element.isConnected && element !== root) {
                record.vertical?.track.remove();
                record.horizontal?.track.remove();
                resizeObserver.unobserve(element);
                records.delete(element);
            }
        }
    }

    // 크기 변경 감지
    const resizeObserver = new ResizeObserver(() => {
        scanElements();
        records.forEach((record, element) => updateElement(element));
    });

    // 모달 생성, 팝업 생성 등 DOM 변경 감지
    const mutationObserver = new MutationObserver(() => {
        scanElements();
    });

    mutationObserver.observe(document.documentElement, {
        childList: true,
        subtree: true
    });

    // 브라우저 창 크기 및 페이지 스크롤 감지
    window.addEventListener('resize', () => {
        scanElements();
        records.forEach((record, element) => updateElement(element));
    });

    window.addEventListener('scroll', () => {
        records.forEach((record, element) => updateElement(element));
        showBars(root);
    }, { passive: true });

    // 최초 실행
    scanElements();
})();