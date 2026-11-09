// src/api/loginApi.js

// rememberMe 값 받도록 매개변수 값 추가
export const requestLogin = async (userId, password, rememberMe) => {
  // 백엔드 시큐리티 로그인 규칙에 맞게 Form 데이터 형식으로 변환합니다.
  const params = new URLSearchParams();
  params.append('userId', userId);
  params.append('password', password);
  
  // 체크박스 상태값 true / false 를 parms에 추가
  params.append('rememberMe', rememberMe);
  
  try {
    // fetch를 이용해 백엔드 서버로 POST 요청을 보냅니다.
    const response = await fetch('http://localhost:8080/login', {
      method: 'POST',
      body: params, // JSON이 아닌 변환한 Form 데이터를 넣습니다.
    });

    if (response.ok) {
      console.log("로그인 성공!");
      return true;
    } else {
      alert("아이디나 비밀번호가 틀렸습니다.");
      return false;
    }
  } catch (error) {
    console.error("서버 통신 에러 발생:", error);
    alert("서버와 연결할 수 없습니다.");
    return false;
  }
};