import { Link } from 'react-router-dom'

export default function AdminDashBoard() {
  return (
    <div style={{ padding: '40px' }}>
      <h1>메인 화면</h1>
      <Link to="/login">로그인</Link>
      <Link to="/signup">회원가입</Link>
    </div>
  )
}