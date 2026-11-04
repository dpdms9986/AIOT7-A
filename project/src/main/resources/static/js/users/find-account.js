// 계정 찾기: 탭 전환, 연락처 자동 하이픈, 메일 발송 중 중복 제출 방지
document.addEventListener("DOMContentLoaded", () => {
  const tabs = document.querySelectorAll(".findaccount-tabs button");
  const panels = document.querySelectorAll(".findaccount-panel");

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      tabs.forEach((t) => t.classList.toggle("active", t === tab));
      panels.forEach((p) => { p.hidden = p.dataset.panel !== tab.dataset.tab; });
    });
  });

  const phone = document.querySelector("#phoneForId");
  if (phone) {
    phone.addEventListener("input", () => {
      const d = phone.value.replace(/\D/g, "").slice(0, 11);
      if (d.length < 4) phone.value = d;
      else if (d.length < 8) phone.value = `${d.slice(0, 3)}-${d.slice(3)}`;
      else phone.value = `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`;
    });
  }

  const pwForm = document.querySelector('.findaccount-panel[data-panel="password"]');
  if (pwForm) {
    pwForm.addEventListener("submit", () => {
      const btn = pwForm.querySelector(".login-submit");
      btn.disabled = true;
      btn.textContent = "발송 중...";
    });
  }
});