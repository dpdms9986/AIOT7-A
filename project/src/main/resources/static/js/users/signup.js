/* ===== 회원가입 (signup.js) ===== */
document.addEventListener("DOMContentLoaded", () => {
  const signupForm = document.querySelector("#signup-form");
  if (!signupForm) return;

  // 공통 팝업(toast.html 의 showPopup) 사용. 혹시 없으면 기본 alert 로
  const popup = (msg, type = "ok", onClose) => {
    if (window.showPopup) {
      window.showPopup(msg, type, onClose);
    } else {
      alert(msg);
      if (onClose) onClose();
    }
  };

  const emailInput = document.querySelector("#signup-userId");
  const sendBtn = document.querySelector("#btn-send-code");
  const authBox = document.querySelector("#auth-box");
  const authCodeInput = document.querySelector("#signup-auth-code");
  const authTimer = document.querySelector("#auth-timer");
  const verifyBtn = document.querySelector("#btn-verify-code");
  const authMsg = document.querySelector("#auth-msg");
  const pwInput = document.querySelector("#signup-pw");
  const pw2Input = document.querySelector("#signup-pw2");
  const pwMsg = document.querySelector("#pw-msg");
  const teamField = document.querySelector("#field-team");
  const inviteField = document.querySelector("#field-invite");
  const teamInput = document.querySelector("#signup-team");
  const inviteInput = document.querySelector("#signup-invite");
  const phoneInput = document.querySelector("#signup-phone");

  // th:action 폼이 자동으로 넣어주는 CSRF 토큰 (없으면 POST 가 403)
  const csrfToken = signupForm.querySelector('input[name="_csrf"]')?.value;

  // ===== 이메일 인증 상태 =====
  let isEmailVerified = false; // 이메일 인증 성공 여부
  let verifiedEmail = "";      // 인증을 마친 이메일
  let timerInterval = null;    // 3분 카운트다운 핸들러

  const isEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

  // 상태 메시지 (type: "ok" 녹색 / "warn" 붉은색 / "" 기본)
  const setMsg = (text, type = "") => {
    authMsg.textContent = text;
    authMsg.className = `signup-hint ${type}`.trim();
  };

  // POST 공통 (파라미터는 쿼리스트링, CSRF 토큰은 헤더)
  const postAuth = async (url, params) => {
    const res = await fetch(`${url}?${new URLSearchParams(params)}`, {
      method: "POST",
      headers: csrfToken ? { "X-CSRF-TOKEN": csrfToken } : {},
    });
    const text = (await res.text()).trim();
    return { ok: res.ok, status: res.status, text };
  };

  // 타이머
  const stopTimer = () => {
    clearInterval(timerInterval);
    timerInterval = null;
  };

  const startTimer = (seconds) => {
    stopTimer();
    let left = seconds;
    authTimer.hidden = false;
    authTimer.classList.remove("expired");

    const tick = () => {
      const m = String(Math.floor(left / 60)).padStart(2, "0");
      const s = String(left % 60).padStart(2, "0");
      authTimer.textContent = `${m}:${s}`;

      if (left <= 0) {
        stopTimer();
        authTimer.classList.add("expired");
        verifyBtn.disabled = true;
        setMsg("인증 시간이 만료되었습니다. [재발송]을 눌러 주세요.", "warn");
        return;
      }
      left--;
    };
    tick();
    timerInterval = setInterval(tick, 1000);
  };

  // 인증 상태 전체 초기화
  const resetAuth = () => {
    stopTimer();
    isEmailVerified = false;
    verifiedEmail = "";
    authBox.hidden = true;
    authCodeInput.value = "";
    authCodeInput.readOnly = false;
    authTimer.hidden = false;
    authTimer.textContent = "03:00";
    authTimer.classList.remove("expired");
    verifyBtn.disabled = false;
    sendBtn.disabled = false;
    sendBtn.textContent = "인증번호 받기";
    setMsg("");
  };

  // 이메일을 수정하면 인증 다시 하도록 초기화
  emailInput.addEventListener("input", resetAuth);

  // 인증번호 입력칸은 숫자만
  authCodeInput.addEventListener("input", () => {
    authCodeInput.value = authCodeInput.value.replace(/\D/g, "");
  });

  // 1. [인증번호 받기 / 재발송]
  sendBtn.addEventListener("click", async () => {
    const email = emailInput.value.trim();

    if (!isEmail(email)) {
      popup("올바른 이메일 형식으로 입력하세요. (예: user@example.com)", "warn", () => emailInput.focus());
      return;
    }

    sendBtn.disabled = true; // 중복 클릭 방지
    try {
      const { ok, status, text } = await postAuth("/api/auth/email/send-code", { email });

      if (!ok) {
        const msg = text || "인증번호 발송에 실패했습니다.";
        setMsg(msg, "warn");
        popup(msg, "warn");

        // ▼ 테스트용 임시
        if (status === 500) {
          authBox.hidden = false;
          authCodeInput.readOnly = false;
          verifyBtn.disabled = false;
          sendBtn.textContent = "재발송";
          startTimer(180);
        }
        // ▲ 테스트용 임시
        return;
      }

      // 성공: 인증번호 입력 영역 표시 + 3분 타이머
      authBox.hidden = false;
      authCodeInput.value = "";
      authCodeInput.readOnly = false;
      verifyBtn.disabled = false;
      sendBtn.textContent = "재발송";
      setMsg(text || "인증번호가 발송되었습니다. 메일함을 확인해 주세요.", "ok");
      startTimer(180);
      authCodeInput.focus();
    } catch (error) {
      console.error("인증번호 발송 에러:", error);
      popup("서버와 연결할 수 없습니다.", "warn");
    } finally {
      if (!isEmailVerified) sendBtn.disabled = false;
    }
  });

  // 2. [인증 확인]
  verifyBtn.addEventListener("click", async () => {
    const email = emailInput.value.trim();
    const code = authCodeInput.value.trim();

    if (!/^\d{6}$/.test(code)) {
      setMsg("인증번호 6자리를 입력하세요.", "warn");
      authCodeInput.focus();
      return;
    }

    try {
      const { ok, text } = await postAuth("/api/auth/email/verify-code", { email, code });

      // 서버 성공 응답: 200 + true
      if (ok && text === "true") {
        stopTimer();
        authTimer.hidden = true;
        emailInput.readOnly = true;
        authCodeInput.readOnly = true;
        verifyBtn.disabled = true;
        sendBtn.disabled = true;
        isEmailVerified = true;
        verifiedEmail = email;
        setMsg("✔ 이메일 인증이 완료되었습니다.", "ok");
        return;
      }

      // 실패: 서버가 준 사유(불일치/만료) 표시
      setMsg(text || "인증번호가 일치하지 않거나 만료되었습니다.", "warn");
    } catch (error) {
      console.error("인증번호 확인 에러:", error);
      popup("서버와 연결할 수 없습니다.", "warn");
    }
  });

  // 3. 비밀번호 실시간 일치 검사
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

  // 4. 역할 선택(단장/조종사)에 따라 입력창 토글
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

  // 5. 폼 제출 전 검사 → 통과하면 th:action 으로 그대로 전송
  signupForm.addEventListener("submit", (event) => {
    const emailVal = emailInput.value.trim();
    const pwVal = pwInput.value;
    const pw2Val = pw2Input.value;
    const selectedRole = signupForm.querySelector('input[name="role"]:checked')?.value;

    // 검증 1: 이메일 인증 여부
    if (!isEmailVerified || emailVal !== verifiedEmail) {
      event.preventDefault();
      popup("이메일 인증을 완료해 주세요.", "warn", () => emailInput.focus());
      return;
    }

    // 검증 2: 비밀번호 일치 여부
    if (pwVal !== pw2Val) {
      event.preventDefault();
      popup("비밀번호가 일치하지 않습니다.", "warn", () => pw2Input.focus());
      return;
    }

    // 검증 3: 조종사 선택 시 초대코드 6자리 검증
    if (selectedRole === "PILOT") {
      const code = inviteInput.value.trim();
      if (!/^[A-Za-z0-9]{6}$/.test(code)) {
        event.preventDefault();
        popup("초대 코드는 영문·숫자 6자리입니다.", "warn", () => inviteInput.focus());
        return;
      }
    }
  });
});