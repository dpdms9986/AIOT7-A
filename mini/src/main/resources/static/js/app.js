// 로그인 전 화면 공통 (로그인, 계정 찾기, 회원가입)
document.addEventListener("DOMContentLoaded", () => {

  /* ===== 로그인 (login.html) ===== */
  const loginForm = document.querySelector("#login-form");

  if (loginForm) {
    const userIdInput = document.querySelector("#userId");
    const passwordInput = document.querySelector("#password");
    const rememberCheck = document.querySelector("#remember-id");
    const autoLoginCheck = document.querySelector("#remember-me")

    // 아이디 기억하기
    const savedId = localStorage.getItem("savedUserId");

    if (savedId) {
      userIdInput.value = savedId;
      rememberCheck.checked = true;
    }

    loginForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const userId = userIdInput.value.trim();
      const password = passwordInput.value;

      if (rememberCheck.checked) {
        localStorage.setItem("savedUserId", userId);
      } else {
        localStorage.removeItem("savedUserId");
      }

      const isAutoLogin = autoLoginCheck ? autoLoginCheck.checked : false;
      try {
        const response = await fetch("/api/auth/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ 
            userId, 
            password, 
            rememberMe: isAutoLogin
          }),
        });

        if (response.ok) {
          const text = await response.text();
          console.log("로그인 응답 원본:", text);

          let user = {};
          try {
            user = JSON.parse(text);
          } catch (e) {
            console.warn("응답이 JSON이 아닙니다.");
          }

          alert("로그인 환영합니다!");

          // 권한별 메인으로 이동
          const role = String(user.role || "").toUpperCase();
          // location.href = role === "PILOT" ? "pilot-dashboard.html" : "dashboard.html";
          location.href = role === "PILOT" ? "pilot-dashboard.html" : "task-assignment.html";
        } else {
          alert("이메일이나 비밀번호가 틀렸습니다.");
        }
      } catch (error) {
        console.error("서버 통신 에러 발생:", error);
        alert("서버와 연결할 수 없습니다.");
      }
    });
  }

  /* ===== 계정 찾기 (findaccount.html) ===== */
  const findForm = document.querySelector("#findaccount-form");
  if (findForm) {
    const tabs = [...document.querySelectorAll("[data-tab]")];
    const nameField = document.querySelector('[data-field="name"]');
    const userIdField = document.querySelector('[data-field="userid"]');
    const submitButton = document.querySelector("#findaccount-submit");
    let activeTab = "id";

    const setTab = (tab) => {
      activeTab = tab;
      tabs.forEach((button) => {
        const selected = button.dataset.tab === tab;
        button.classList.toggle("active", selected);
        button.setAttribute("aria-selected", String(selected));
      });
      nameField.hidden = tab !== "id";
      nameField.querySelector("input").required = tab === "id";
      userIdField.hidden = tab !== "password";
      userIdField.querySelector("input").required = tab === "password";
      submitButton.textContent = tab === "id" ? "아이디 찾기" : "비밀번호 찾기";
    };

    tabs.forEach((button) => button.addEventListener("click", () => setTab(button.dataset.tab)));

    findForm.addEventListener("submit", (event) => {
      event.preventDefault();
      alert(activeTab === "id"
        ? "아이디 찾기 기능은 추후 연결할 예정입니다."
        : "비밀번호 찾기 기능은 추후 연결할 예정입니다.");
    });
  }

  /* ===== 회원가입 (signup.html) ===== */
  const signupForm = document.querySelector("#signup-form");
  if (signupForm) {
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
    let checkedUserId = ""; // 중복확인을 통과한 이메일

    const isEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

    // 이메일을 고치면 중복확인 다시
    emailInput.addEventListener("input", () => {
      checkedUserId = "";
    });

    // 이메일 형식 검사 + 중복확인
    checkBtn.addEventListener("click", async () => {
      const userId = emailInput.value.trim();

      if (!isEmail(userId)) {
        alert("올바른 이메일 형식으로 입력하세요. (예: user@example.com)");
        return;
      }

      try {
        const response = await fetch(`/api/auth/check-email?userId=${encodeURIComponent(userId)}`);

        if (!response.ok) {
          alert(`중복확인을 할 수 없습니다. (서버 응답: ${response.status})`);
          return;
        }

        const result = await response.json();

        if (result === true) {
          alert("이미 사용 중인 이메일입니다.");
          checkedUserId = "";
        } else if (result === false) {
          alert("사용 가능한 이메일입니다.");
          checkedUserId = userId;
        } else {
          console.log("서버 응답 내용:", result);
          alert("서버 응답 형식이 예상과 다릅니다.");
        }
      } catch (error) {
        console.error("중복확인 에러:", error);
        alert("서버와 연결할 수 없습니다.");
      }
    });

    // 비밀번호 확인 실시간 검사
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

    // 역할에 따라 입력칸 보이기
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

    const roleParam = new URLSearchParams(location.search).get("role");
    if (["LEADER", "PILOT"].includes(roleParam)) {
      const target = signupForm.querySelector(`input[name="role"][value="${roleParam}"]`);
      if (target) {
        target.checked = true;
        target.dispatchEvent(new Event("change"));
      }
    }

    // 회원가입
    signupForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = Object.fromEntries(new FormData(signupForm));

      if (data.userId.trim() !== checkedUserId) {
        alert("이메일 중복확인을 해주세요.");
        return;
      }
      if (data.password !== data.passwordConfirm) {
        alert("비밀번호가 일치하지 않습니다.");
        return;
      }
      if (data.role === "PILOT" && !/^[A-Za-z0-9]{6}$/.test(data.inviteCode || "")) {
        alert("초대 코드는 영문·숫자 6자리입니다.");
        return;
      }

      const body = {
        userId: data.userId.trim(),
        password: data.password,
        userName: data.userName.trim(),
        role: data.role,
        teamName: data.role === "LEADER" ? data.teamName.trim() : null,
        inviteCode: data.role === "PILOT" ? data.inviteCode.toUpperCase() : null,
        phone: data.role === "PILOT" ? data.phone.trim() : null,
      };

      try {
        const response = await fetch("/api/auth/signup", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        
        
        
        });

        if (response.ok) {
          alert("회원가입이 완료되었습니다. 로그인해 주세요.");
          location.href = "login.html";
        } else {
          console.log("회원가입 실패 응답:", await response.text());
          alert("회원가입에 실패했습니다. 입력값이나 초대 코드를 확인하세요.");
        }
      } catch (error) {
        console.error("회원가입 에러:", error);
        alert("서버와 연결할 수 없습니다.");
      }
    });
  }
});