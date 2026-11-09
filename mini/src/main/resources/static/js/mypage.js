// 마이페이지 (mypage.html) - GNB_007, USER_011

const DRONE_MODELS = ["DJI Agras T40", "DJI Agras T30", "DJI Agras T25", "XAG P100", "기타"];

// 실적 제목 (조종사는 내 실적, 단장은 방제단 전체 실적)
const RECORD_TITLE = { PILOT: "📊 내 방제 실적", LEADER: "📊 방제단 실적", ADMIN: "📊 방제단 실적" };

// 실적 API (백엔드 주소에 맞게 수정)
// 응답 예: { month: 6, total: 42, area: 38400, used: 41.5 }
const RECORDS_API = "/api/users/me/records";

// 실적 불러오기
async function loadRecords() {
  try {
    const res = await fetch(RECORDS_API);
    if (!res.ok) throw new Error(`서버 응답 ${res.status}`);
    const rec = await res.json();
    $("#rec-month").textContent = rec.month ?? 0;
    $("#rec-total").textContent = rec.total ?? 0;
    $("#rec-area").textContent = Number(rec.area || 0).toLocaleString();
    $("#rec-used").textContent = rec.used ?? 0;
  } catch (error) {
    console.error("실적 조회 실패:", error);
    ["#rec-month", "#rec-total", "#rec-area", "#rec-used"].forEach((s) => ($(s).textContent = "-"));
  }
}

const $ = (selector) => document.querySelector(selector);
let currentRole = "";
let initialized = false;

/* ===== 1. 화면 채우기 ===== */
function initMyPage(user) {
  if (initialized) return;
  initialized = true;

  currentRole = String(user.role || "").toUpperCase();

  // 프로필
  $("#my-name").textContent = user.userName || "-";
  $("#my-role").textContent = ROLE_NAME[currentRole] || currentRole; // common.js에 있는 이름표
  $("#my-email").textContent = user.userId || "-";
  $("#my-team").textContent = user.teamName || "소속 없음";

  // 단장만 초대 코드
  if (currentRole === "LEADER" && user.inviteCode) {
    $("#my-invite-code").textContent = user.inviteCode;
    document.querySelectorAll(".my-invite-row").forEach((el) => (el.hidden = false));
  }

  // 실적 (GNB_007)
  $("#record-title").textContent = RECORD_TITLE[currentRole] || RECORD_TITLE.PILOT;
  loadRecords();

  // 정보 수정 기본값
  $("#info-email").value = user.userId || "";
  $("#info-name").value = user.userName || "";
  $("#info-phone").value = user.phone || "";

  // 조종사만 기체 정보
  if (currentRole === "PILOT") {
    $("#drone-fields").hidden = false;
    $("#info-drone").innerHTML = DRONE_MODELS.map((m) => `<option value="${m}">${m}</option>`).join("");
    // 기체 정보는 로그인 사용자 정보에서 (백엔드 필드 이름에 맞게 수정)
    $("#info-drone").value = user.droneModel || DRONE_MODELS[0];
    $("#info-tank").value = user.tankCapacity || "";
    $("#info-tank").required = true;
  }
}

// common.js가 사용자 정보를 준비하면 실행
if (window.currentUser) initMyPage(window.currentUser);
document.addEventListener("user-ready", (event) => initMyPage(event.detail));

/* ===== 2. 초대 코드 복사 ===== */
$("#my-invite").addEventListener("click", async () => {
  const code = $("#my-invite-code").textContent;
  try {
    await navigator.clipboard.writeText(code);
    alert(`초대 코드 ${code}가 복사되었습니다.`);
  } catch (error) {
    alert(`초대 코드: ${code}`);
  }
});

/* ===== 3. 정보 저장 (USER_011) ===== */
$("#info-form").addEventListener("submit", (event) => {
  event.preventDefault();

  const phone = $("#info-phone").value.trim();
  if (!/^01[016789]-?\d{3,4}-?\d{4}$/.test(phone)) {
    alert("연락처 형식을 확인하세요. (예: 010-1234-5678)");
    return;
  }

  const body = { phone };

  if (currentRole === "PILOT") {
    const tank = Number($("#info-tank").value);
    if (!tank || tank < 1 || tank > 100) {
      alert("약제 탱크 용량은 1~100L 사이로 입력하세요.");
      return;
    }
    body.droneModel = $("#info-drone").value;
    body.tankCapacity = tank;
  }

  console.log("정보 수정 요청:", body);

  // 나중에 백엔드 연결 (주소·이름은 백엔드에 맞게)
  // const res = await fetch("/api/users/me", {
  //   method: "PUT",
  //   headers: { "Content-Type": "application/json" },
  //   body: JSON.stringify(body),
  // });
  // if (!res.ok) { alert("저장에 실패했습니다."); return; }
  // ※ await를 쓰려면 위의 (event) => 를 async (event) => 로 바꿔야 합니다.

  alert("정보가 저장되었습니다.");
});

/* ===== 4. 비밀번호 변경 (USER_011) ===== */
const pwNew = $("#pw-new");
const pwNew2 = $("#pw-new2");
const pwCheck = $("#pw-check");

// 새 비밀번호 확인 실시간 검사
function checkNewPassword() {
  if (!pwNew2.value) {
    pwCheck.textContent = "";
    pwCheck.className = "signup-hint";
  } else if (pwNew.value === pwNew2.value) {
    pwCheck.textContent = "새 비밀번호가 일치합니다.";
    pwCheck.className = "signup-hint ok";
  } else {
    pwCheck.textContent = "새 비밀번호가 일치하지 않습니다.";
    pwCheck.className = "signup-hint warn";
  }
}
pwNew.addEventListener("input", checkNewPassword);
pwNew2.addEventListener("input", checkNewPassword);

$("#pw-form").addEventListener("submit", (event) => {
  event.preventDefault();

  const currentPassword = $("#pw-current").value;
  const newPassword = pwNew.value;

  if (newPassword !== pwNew2.value) {
    alert("새 비밀번호가 일치하지 않습니다.");
    return;
  }
  if (newPassword === currentPassword) {
    alert("새 비밀번호가 현재 비밀번호와 같습니다.");
    return;
  }

  console.log("비밀번호 변경 요청 (값은 표시 안 함)");

  // 나중에 백엔드 연결: 서버가 현재 비밀번호를 확인한 뒤 BCrypt로 저장
  // const res = await fetch("/api/users/me/password", {
  //   method: "PUT",
  //   headers: { "Content-Type": "application/json" },
  //   body: JSON.stringify({ currentPassword, newPassword }),
  // });
  // if (!res.ok) { alert("현재 비밀번호가 맞지 않거나 변경에 실패했습니다."); return; }

  alert("비밀번호가 변경되었습니다.");
  $("#pw-form").reset();
  checkNewPassword();
});