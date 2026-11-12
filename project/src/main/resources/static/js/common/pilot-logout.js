// 조종사가 작업 중(IN_PROGRESS)에 로그아웃하려 하면 확인 팝업을 띄운다.
(function () {
  document.addEventListener("submit", async function (e) {
    const form = e.target;
    if (!(form instanceof HTMLFormElement)) return;

    // 로그아웃 폼만 대상
    if (!/\/logout$/.test(form.getAttribute("action") || "")) return;

    // 확인이 끝난 제출은 그대로 통과
    if (form.dataset.logoutChecked === "1") return;

    e.preventDefault();
    if (form.dataset.logoutChecking === "1") return;   // 확인 중 중복 클릭 방지
    form.dataset.logoutChecking = "1";

    let working = false;
    try {
      const res = await fetch("/api/pilot/assignments", {
        headers: { Accept: "application/json" },
      });
      if (res.ok) {
        const list = await res.json();
        working = Array.isArray(list) && list.some((a) => a.status === "IN_PROGRESS");
      }
    } catch (err) {
      // 조회에 실패하면 막지 않고 그대로 로그아웃 진행
    }

    if (working) {
      const msg = "작업 중 로그아웃하면 작업이 중단됩니다.\n그래도 로그아웃 하시겠습니까?";
      const ok = typeof window.showAskPopup === "function"
        ? await window.showAskPopup(msg, { yesText: "로그아웃", noText: "취소" })
        : window.confirm(msg);   // 커스텀 팝업이 없는 페이지용 예비
      if (!ok) {
        delete form.dataset.logoutChecking;
        return;
      }
    }

    form.dataset.logoutChecked = "1";
    if (form.requestSubmit) form.requestSubmit(); else form.submit();
  });
})();