// 드론·자산 관리 - 등록 팝업 열고 닫기만 (목록/검색/상세/저장은 서버)
document.addEventListener("DOMContentLoaded", () => {
  const modal = document.querySelector("#asset-register-modal");
  const openBtn = document.querySelector("#asset-register-btn");
  if (!modal || !openBtn) return;

  const open = () => { modal.hidden = false; document.body.style.overflow = "hidden"; };
  const close = () => { modal.hidden = true; document.body.style.overflow = ""; };

  openBtn.addEventListener("click", open);
  modal.querySelectorAll("[data-close-modal]").forEach((b) => b.addEventListener("click", close));
  modal.addEventListener("click", (e) => { if (e.target === modal) close(); });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !modal.hidden) close(); });
});