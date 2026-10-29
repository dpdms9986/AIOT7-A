/* ===== 회원가입 (signup.js) ===== */
document.addEventListener("DOMContentLoaded", () => {
  const signupForm = document.querySelector("#signup-form");
  if (!signupForm) return;

  const emailInput = document.querySelector("#signup-userId");
  const checkBtn = document.querySelector("#check-userId-btn");
  const pwInput = document.querySelector("#signup-pw");
  const pw2Input = document.querySelector("#signup-pw2");
  const pwMsg = document.querySelector("#pw-msg");
  const teamField = document.querySelector("#field-team");
  const inviteField = document.querySelector("#field-invite");
  const teamInput = document.querySelector("#signup-team");
  const inviteInput = document.querySelector("#signup-invite");
  const phoneInput = document.querySelector("#signup-phone");
  let checkedUserName = ""; // 중복확인을 통과한 이메일 저장 변수

  const isEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

  // 이메일 입력값을 수정하면 중복확인 다시 하도록 초기화
  emailInput.addEventListener("input", () => {
    checkedUserName = "";
  });

  // 1. 이메일 중복확인 (비동기 유지)
  checkBtn.addEventListener("click", async () => {
    const userName = emailInput.value.trim();

    if (!isEmail(userName)) {
      alert("올바른 이메일 형식으로 입력하세요. (예: user@example.com)");
      return;
    }

    try {
      // 주소를 /check-username 으로 일치시킵니다!
      const response = await fetch(`/check-username?userName=${encodeURIComponent(userName)}`);

      if (!response.ok) {
        alert(`중복확인을 할 수 없습니다. (서버 응답: ${response.status})`);
        return;
      }

      const result = await response.json();

      // 서버 응답: true(중복), false(사용 가능)
      if (result === true) {
        alert("이미 사용 중인 이메일입니다.");
        checkedUserName = "";
      } else if (result === false) {
        alert("사용 가능한 이메일입니다.");
        checkedUserName = userName; // 성공한 이메일 기록
      } else {
        alert("서버 응답 형식이 예상과 다릅니다.");
      }
    } catch (error) {
      console.error("중복확인 에러:", error);
      alert("서버와 연결할 수 없습니다.");
    }
  });

  // 2. 비밀번호 실시간 일치 검사
  const checkPassword = () => {
    if (!pw2Input.value) {
      pwMsg.textContent = "";
      pwMsg.className = "signup-hint";
    } else if (pwInput.value === pw2Input.value) {
      pwMsg.textContent = "비밀번호가 일치합니다.";
      pwMsg.className = "signup-hint ok";
    } else {
      pwMsg.textContent = "비밀번호가 일치하지 않습니다.";
      pwMsg.className = "signup-hint warn";
    }
  };
  pwInput.addEventListener("input", checkPassword);
  pw2Input.addEventListener("input", checkPassword);

  // 3. 역할 선택(단장/조종사)에 따라 입력창 토글
  signupForm.querySelectorAll('input[name="role"]').forEach((radio) => {
    radio.addEventListener("change", () => {
      const isLeader = radio.value === "LEADER";
      teamField.hidden = !isLeader;
      teamInput.required = isLeader;
      inviteField.hidden = isLeader;
      inviteInput.required = !isLeader;
      phoneInput.required = !isLeader;
    });
  });

  // URL 파라미터(?role=LEADER 등)가 있으면 라디오 자동 체크
  const roleParam = new URLSearchParams(location.search).get("role");
  if (["LEADER", "PILOT"].includes(roleParam)) {
    const target = signupForm.querySelector(`input[name="role"][value="${roleParam}"]`);
    if (target) {
      target.checked = true;
      target.dispatchEvent(new Event("change"));
    }
  }

  // 4. [타임리프 연동 핵심] 폼 제출 이벤트
  // 비동기 fetch 호출이 전혀 없습니다! 검사만 통과하면 브라우저가 form의 th:action으로 바로 전송합니다.
  signupForm.addEventListener("submit", (event) => {
    const userNameVal = emailInput.value.trim();
    const pwVal = pwInput.value;
    const pw2Val = pw2Input.value;
    const selectedRole = signupForm.querySelector('input[name="role"]:checked')?.value;

    // 검증 1: 중복확인 여부
    if (userNameVal !== checkedUserName) {
      alert("이메일 중복확인을 해주세요.");
      event.preventDefault(); // 전송 중단
      return;
    }

    // 검증 2: 비밀번호 일치 여부
    if (pwVal !== pw2Val) {
      alert("비밀번호가 일치하지 않습니다.");
      event.preventDefault(); // 전송 중단
      return;
    }

    // 검증 3: 조종사 선택 시 초대코드 6자리 검증
    if (selectedRole === "PILOT") {
      const code = inviteInput.value.trim();
      if (!/^[A-Za-z0-9]{6}$/.test(code)) {
        alert("초대 코드는 영문·숫자 6자리입니다.");
        event.preventDefault(); // 전송 중단
        return;
      }
    }

    // 모든 검증을 통과하면 event.preventDefault()를 부르지 않으므로,
    // 스프링 컨트롤러의 @PostMapping("/signup")으로 자동 폼 전송됩니다!
  });
});