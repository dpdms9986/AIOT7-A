// 모든 페이지가 이 파일 하나를 같이 씁니다.
// 각 기능은 "해당 폼이 있는 페이지에서만" 실행되도록 if로 감싸 두었습니다.
document.addEventListener("DOMContentLoaded", () => {

  /* ===== 로그인 (login.html) - 기존 loginApi.js + Login.jsx ===== */
  const loginForm = document.querySelector("#login-form");
  if (loginForm) {
    const userIdInput = document.querySelector("#userId");
    const passwordInput = document.querySelector("#password");
    const rememberCheck = document.querySelector("#remember-id");

    // 아이디 기억하기: 저장된 아이디가 있으면 채워 넣기
    const savedId = localStorage.getItem("savedUserId");
    if (savedId) {
      userIdInput.value = savedId;
      rememberCheck.checked = true;
    }

    loginForm.addEventListener("submit", async (event) => {
      event.preventDefault(); // 새로고침 막기
      const userId = userIdInput.value.trim();
      const password = passwordInput.value;

      if (rememberCheck.checked) {
        localStorage.setItem("savedUserId", userId);
      } else {
        localStorage.removeItem("savedUserId");
      }

      // 백엔드 시큐리티 로그인 규칙에 맞게 Form 데이터 형식으로 변환
      const params = new URLSearchParams();
      params.append("userId", userId);
      params.append("password", password);

      try {
        // 같은 서버(8080)에서 열리므로 "/login"만 씀
        const response = await fetch("/login", {
          method: "POST",
          body: params,
        });
        if (response.ok) {
          alert("로그인 환영합니다!");
          location.href = "index.html"; // 성공 시 메인으로 이동
        } else {
          alert("아이디나 비밀번호가 틀렸습니다.");
        }
      } catch (error) {
        console.error("서버 통신 에러 발생:", error);
        alert("서버와 연결할 수 없습니다.");
      }
    });
  }

  /* ===== 계정 찾기 (findaccount.html) - 기존 FindAccount.jsx ===== */
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
    const userIdInput = document.querySelector("#signup-userId");
    const checkBtn = document.querySelector("#check-userId-btn");
    let checkedUserId = ""; // 중복확인을 통과한 아이디를 기억

    // 아이디를 다시 고치면 중복확인을 다시 해야 함
    userIdInput.addEventListener("input", () => {
      checkedUserId = "";
    });

    // 중복확인 버튼
    checkBtn.addEventListener("click", async () => {
      const userId = userIdInput.value.trim();

      if (userId.length < 4 || userId.length > 50) {
        alert("아이디는 4~50자로 입력하세요.");
        return;
      }

      try {
        // ⚠️ 백엔드 주소는 팀원이 만든 API에 맞게 바꾸세요
        const response = await fetch(`/api/users/check?userId=${encodeURIComponent(userId)}`);
        const isDuplicate = await response.json(); // 서버가 true(중복)/false(사용 가능)를 준다고 가정

        if (isDuplicate) {
          alert("이미 사용 중인 아이디입니다.");
        } else {
          alert("사용 가능한 아이디입니다.");
          checkedUserId = userId;
        }
      } catch (error) {
        console.error("중복확인 에러:", error);
        alert("중복확인 기능은 백엔드 연결 후 사용할 수 있습니다.");
      }
    });

    // 회원가입 버튼
    signupForm.addEventListener("submit", (event) => {
      event.preventDefault();

      const data = Object.fromEntries(new FormData(signupForm));

      if (data.userId !== checkedUserId) {
        alert("아이디 중복확인을 해주세요.");
        return;
      }
      if (data.password !== data.passwordConfirm) {
        alert("비밀번호가 일치하지 않습니다.");
        return;
      }

      alert("회원가입 기능은 추후 연결할 예정입니다.");
    });
  }
});