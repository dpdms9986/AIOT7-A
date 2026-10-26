/* ===== 접이식 사이드바 ===== */

const sidebarToggle = document.getElementById("sidebar-toggle");

if (sidebarToggle) {
    const storageKey = "hazard-sidebar-collapsed";

    function setSidebarState(collapsed) {
        document.body.classList.toggle("sidebar-collapsed", collapsed);

        sidebarToggle.setAttribute("aria-expanded", String(!collapsed));
        sidebarToggle.setAttribute(
            "aria-label",
            collapsed ? "사이드바 펼치기" : "사이드바 접기"
        );
        sidebarToggle.title = collapsed
            ? "메뉴 펼치기"
            : "메뉴 접기";
    }

    try {
        setSidebarState(localStorage.getItem(storageKey) === "true");
    } catch {
        setSidebarState(false);
    }

    sidebarToggle.addEventListener("click", () => {
        const collapsed =
            !document.body.classList.contains("sidebar-collapsed");

        setSidebarState(collapsed);

        try {
            localStorage.setItem(storageKey, String(collapsed));
        } catch {
            // 저장이 불가능해도 현재 화면의 접기/펼치기는 동작
        }
    });
}