//   로그인 후 화면 공통 (관리자·조종사 화면 모두 사용)
// - 헤더에 이름·역할·소속 방제단·초대 코드 표시 - GNB_006
// - 로고 클릭 시 권한별 메인으로 이동 - GNB_001
// - 로그아웃 - USER_012

// 로그인 검사 on/off  화면만 보려면 false, 완성 후에는 꼭 true
const LOGIN_GUARD = false;

const MAIN_PAGE = { ADMIN: "dashboard.html", PILOT: "pilot-dashboard.html" };
const ROLE_NAME = { ADMIN: "방제단장", PILOT: "조종사" };

// 헤더 채우기
function fillHeader(user) {
  const role = String(user.role || "").toUpperCase();
  const setText = (selector, text) => {
    const el = document.querySelector(selector);
    if (el && text) el.textContent = text;
  };

  setText("#user-name", user.userName || user.userId);
  setText("#user-role", ROLE_NAME[role] || role);
  setText("#team-name", user.teamName || "소속 없음");

  // 로고 → 권한별 메인 (GNB_001)
  const logo = document.querySelector(".dash-logo");
  if (logo && MAIN_PAGE[role]) logo.href = MAIN_PAGE[role];

  // 단장만 초대 코드 표시
  const inviteBtn = document.querySelector("#invite-btn");
  if (inviteBtn && role === "ADMIN" && user.inviteCode) {
    setText("#invite-code", user.inviteCode);
    inviteBtn.hidden = false;
  }
}

// 로그인·권한 검사
async function checkAccess() {
  if (!LOGIN_GUARD) return;

  const required = document.body.dataset.role; // 이 화면에 필요한 역할

  try {
    const res = await fetch("/api/auth/status");

    if (!res.ok) {                       // 로그인 안 함
      location.href = "login.html";
      return;
    }

    const user = await res.json();
    const role = String(user.role || "").toUpperCase();

    if (required && role !== required) { // 다른 역할의 화면에 들어옴
      alert("접근 권한이 없는 화면입니다.");
      location.href = MAIN_PAGE[role] || "login.html";
      return;
    }

    fillHeader(user);
  } catch (error) {
    console.error("로그인 확인 실패:", error);
    location.href = "login.html";
  }
}

// 초대 코드 복사, 로그아웃
document.addEventListener("click", async (event) => {
  if (event.target.closest("#invite-btn")) {
    const code = document.querySelector("#invite-code").textContent;
    try {
      await navigator.clipboard.writeText(code);
      alert(`초대 코드 ${code}가 복사되었습니다.`);
    } catch (error) {
      alert(`초대 코드: ${code}`);
    }
  }

  if (event.target.closest("#logout-btn")) {
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } catch (error) {
      console.error("로그아웃 요청 실패:", error);
    }
    location.href = "login.html";
  }
});

checkAccess();