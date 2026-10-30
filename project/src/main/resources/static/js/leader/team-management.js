// 방제단 관리
// - 목록/검색/요약/재배정 : 서버(타임리프)가 처리
// - JS : 초대코드 복사 + 상세보기 모달만
document.addEventListener("DOMContentLoaded", () => {

  /* ===== 1. 초대 코드 복사 ===== */
  document.querySelector("#invite-pilot-btn")?.addEventListener("click", async () => {
    const code = document.querySelector("#invite-code").textContent.trim();
    try {
      await navigator.clipboard.writeText(code);
      alert(`초대 코드 ${code} 가 복사되었습니다.`);
    } catch (e) {
      alert(`초대 코드: ${code}`);
    }
  });

  /* ===== 2. 상세보기 모달 ===== */
  const modal = document.querySelector("#team-detail-modal");
  const content = document.querySelector("#team-detail-content");

  const fields = [
    ["조종사 이름", "name"],
    ["아이디(이메일)", "username"],
    ["연락처", "phone"],
    ["담당 기체", "drone"],
    ["약제 탱크 용량", "tank"],
    ["숙련도", "skill"],
    ["현재 작업", "task"],
    ["금일 완료 작업", "completed"],
  ];

  const openModal = () => { modal.hidden = false; document.body.style.overflow = "hidden"; };
  const closeModal = () => { modal.hidden = true; document.body.style.overflow = ""; };

  document.querySelectorAll(".btn-detail").forEach((btn) => {
    btn.addEventListener("click", () => {
      content.innerHTML = "";
      fields.forEach(([label, key]) => {
        const item = document.createElement("div");
        item.className = "team-detail-item";
        const span = document.createElement("span");
        span.textContent = label;
        const strong = document.createElement("strong");
        strong.textContent = btn.dataset[key];   // th:data-* 로 심어둔 값
        item.append(span, strong);
        content.append(item);
      });
      openModal();
    });
  });

  modal?.querySelector("[data-close-modal]")?.addEventListener("click", closeModal);
  modal?.addEventListener("click", (e) => { if (e.target === modal) closeModal(); });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && modal && !modal.hidden) closeModal(); });
});