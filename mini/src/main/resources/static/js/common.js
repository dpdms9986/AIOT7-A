// 로그인 후 화면 공통 (관리자·단장·조종사 화면 모두 사용)
// - 권한 검사 (USER_010): <body data-role="LEADER ADMIN"> 처럼 쓰면 그 역할만 접근 가능, 없으면 로그인한 누구나
// - 헤더에 이름·역할·소속 방제단·초대 코드 표시 (GNB_006)
// - 로고 → 권한별 메인 (GNB_001), 내 이름 → 마이페이지 (GNB_007)
// - 로그아웃 (USER_012)
// ⚠️ 각 페이지에서 페이지 전용 JS보다 "먼저" 불러오세요.

// 로그인 검사 켜기/끄기 (로그인이 안 될 때 화면만 보려면 false, 완성 후에는 꼭 true)
const LOGIN_GUARD = false;

// LOGIN_GUARD = false 일 때 쓰는 가짜 사용자 (role을 "LEADER"로 바꾸면 단장으로 확인 가능)
const DEV_USER = {
  userId: "pilot@test.com",
  userName: "김조종",
  role: "LEADER",
  teamName: "대전 유성 방제단",
  inviteCode: "DR789A",
  phone: "010-1234-5678",
};

// 권한별 메인 화면 (대시보드가 없는 동안 단장을 작업 배정으로 보내려면 "task-assignment.html")
const MAIN_PAGE = { ADMIN: "task-assignment.html", LEADER: "task-assignment.html", PILOT: "pilot-dashboard.html" };
const ROLE_NAME = { ADMIN: "관리자", LEADER: "방제단장", PILOT: "조종사" };

// 권한별 메뉴 (GNB_004) - <nav id="gnb-menu"></nav> 가 있는 화면에서 자동으로 그림
const MENUS = {
  LEADER: [
    ["#", "관제 대시보드", "🏠"],
    ["task-assignment.html", "작업 배정", "📋"],
    ["farmlist.html", "농지 관리", "🌱"],
    ["#", "방제단 관리", "👥"],
    ["#", "드론 · 자산", "🚁"],
    ["#", "통계/리포트", "📊"],
    ["hazardlist.html", "게시판", "⚠️"],
  ],
  PILOT: [
    ["pilot-dashboard.html", "내 할당 필지", "📍"],
    ["#", "작업 이력", "🕘"],
    ["hazardlist.html", "게시판", "⚠️"],
  ],
};
MENUS.ADMIN = MENUS.LEADER;

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

  // 내 이름 → 마이페이지 (GNB_007)
  const userLink = document.querySelector(".gnb-user");
  if (userLink) userLink.href = "mypage.html";

  // 단장만 초대 코드 표시
  const inviteBtn = document.querySelector("#invite-btn");
  if (inviteBtn && role === "LEADER" && user.inviteCode) {
    setText("#invite-code", user.inviteCode);
    inviteBtn.hidden = false;
  }

  // 지금 보고 있는 화면 (상세 화면은 목록 메뉴를 켜줌)
  const alias = { "hazarddetail.html": "hazardlist.html" };
  const current = location.pathname.split("/").pop();
  const here = alias[current] || current;
  const items = MENUS[role] || [];

  // 상단 메뉴
  const menu = document.querySelector("#gnb-menu");
  if (menu) {
    menu.innerHTML = items
      .map(([href, text]) => `<a href="${href}"${href === here ? ' class="active"' : ""}>${text}</a>`)
      .join("");
  }

  // 왼쪽 사이드 메뉴 (있는 화면만)
  const side = document.querySelector("#gnb-side");
  if (side) {
    side.innerHTML = items
      .map(([href, text, icon]) => `<a href="${href}"${href === here ? ' class="active"' : ""}><span>${icon}</span>${text}</a>`)
      .join("");
  }
}

// 사용자 정보 준비 완료 → 헤더 채우고, 페이지 JS에게 알려줌
function userReady(user) {
  window.currentUser = user;
  fillHeader(user);
  document.dispatchEvent(new CustomEvent("user-ready", { detail: user }));
}

// 로그인·권한 검사
async function checkAccess() {
  if (!LOGIN_GUARD) {
    setTimeout(() => userReady(DEV_USER)); // 페이지 JS가 다 준비된 뒤 실행
    return;
  }

  const required = document.body.dataset.role; // 이 화면에 필요한 역할

  try {
    const res = await fetch("/api/auth/status");

    if (!res.ok) {
      location.href = "login.html";
      return;
    }

    const user = await res.json();
    const role = String(user.role || "").toUpperCase();

    if (required && !required.split(" ").includes(role)) {
      alert("접근 권한이 없는 화면입니다.");
      location.href = MAIN_PAGE[role] || "login.html";
      return;
    }

    userReady(user);
  } catch (error) {
    console.error("로그인 확인 실패:", error);
    location.href = "login.html";
  }
}

// 헤더 초대 코드 복사, 로그아웃
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