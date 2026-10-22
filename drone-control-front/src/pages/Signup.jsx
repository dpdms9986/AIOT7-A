import "../styles/signup.css";

export default function Signup() {
  const handleSubmit = (event) => {
    event.preventDefault();
    alert("회원가입 기능은 추후 연결할 예정입니다.");
  };

  return (
    <main className="signup-page">
      <section className="signup-card">
        <h1 className="signup-title">회원가입</h1>

        <form className="signup-form" onSubmit={handleSubmit}>
          <div className="signup-fields">
            <input
              type="text"
              name="name"
              placeholder="이름"
              aria-label="이름"
              required
            />

            <input
              type="text"
              name="userId"
              placeholder="아이디"
              aria-label="아이디"
              required
            />

            <input
              type="password"
              name="password"
              placeholder="비밀번호"
              aria-label="비밀번호"
              required
            />

            <input
              type="password"
              name="passwordConfirm"
              placeholder="비밀번호 확인"
              aria-label="비밀번호 확인"
              required
            />
          </div>

          <div className="signup-extra-fields">
            <select
              name="role"
              aria-label="역할 선택"
              defaultValue=""
              required
            >
              <option value="" disabled>
                역할 선택
              </option>
              <option value="pilot">조종수</option>
              <option value="admin">단장</option>
            </select>

            <input
              type="email"
              name="email"
              placeholder="이메일"
              aria-label="이메일"
              required
            />
          </div>

          <button className="signup-submit" type="submit">
            회원가입
          </button>
        </form>
      </section>
    </main>
  );
}