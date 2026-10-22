

import { Link } from 'react-router-dom'
import { requestLogin } from '../api/loginApi.js';

export default function Login() {
  // 아이디와 비밀번호 입력값 관리
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');

   // 로그인 버튼을 눌렀을 때 실행될 함수
  const handleLoginClick = async () => {
    // requestLogin 함수에 입력한 아이디, 비번을 넣어 실행
    const isSuccess = await requestLogin(userId, password);


    if (isSuccess) {
      alert('로그인 환영합니다!');
    }
  };





  return (
    <main className="login-page">
      <section className="login-card">
        <header className="login-brand">
          <div className="login-brand-logo">A</div>
          <h1>농업 드론 관제 시스템</h1>
          <p>계정에 로그인하여 관제 서비스를 이용하세요.</p>
        </header>

        <form className="login-form">
          <div className="login-field">
            <label htmlFor="username">아이디</label>
            <input
              id="username"
              name="username"
              type="text"
              placeholder="아이디를 입력하세요"
            />
          </div>

          <div className="login-field">
            <label htmlFor="password">비밀번호</label>
            <input
              id="password"
              name="password"
              type="password"
              placeholder="비밀번호를 입력하세요"
            />
          </div>

          <div className="login-options">
            <label className="login-remember">
              <input type="checkbox" />
              아이디 기억하기
            </label>
            <Link to="/findaccount" className="login-link">
               계정 찾기
                 </Link>
          </div>

          <button type="submit" className="login-submit">
            로그인
          </button>
        </form>

        <div className="login-signup-link">
  계정이 없으신가요? <Link to="/signup">회원가입</Link>
</div>
      </section>
    </main>
  );
}