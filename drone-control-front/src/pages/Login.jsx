import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { requestLogin } from '../api/loginApi'; // 만들어둔 API 파일 불러오기

export default function Login() {
  // api.js의 파라미터명과 맞추어 userId로 상태 관리
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  
  const navigate = useNavigate();

  const handleLoginSubmit = async (e) => {
    e.preventDefault();

    // requestLogin 함수에 userId와 password 전달
    const isSuccess = await requestLogin(userId, password);

    if (isSuccess) {
      navigate('/'); // 성공 시 메인으로 이동
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

        <form className="login-form" onSubmit={handleLoginSubmit}>
          <div className="login-field">
            <label htmlFor="userId">아이디</label>
            <input
              id="userId"
              name="userId"
              type="text"
              placeholder="아이디를 입력하세요"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
            />
          </div>

          <div className="login-field">
            <label htmlFor="password">비밀번호</label>
            <input
              id="password"
              name="password"
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
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