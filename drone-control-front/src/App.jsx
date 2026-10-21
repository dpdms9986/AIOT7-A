import { Routes, Route } from 'react-router-dom'
import AdminDashBoard from './pages/AdminDashBoard.jsx'
import Login from './pages/Login.jsx'
import Signup from './pages/Signup.jsx'

function App() {
  return (
    <Routes>
      {/*로그인 전 메인화면*/}
      <Route path="/" element={<AdminDashBoard />} />

      {/*로그인 화면*/}
      <Route path="/login" element={<Login />} />

      {/* 회원가입 화면 */}
      <Route path="/signup" element={<Signup />} />
    </Routes>
  )
}

export default App