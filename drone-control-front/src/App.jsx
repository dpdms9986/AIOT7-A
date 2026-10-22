import { Routes, Route } from 'react-router-dom'
import AdminDashBoard from './pages/AdminDashBoard.jsx'
import Login from './pages/Login.jsx'

function App() {
  return (
    <Routes>
      <Route path="/" element={<AdminDashBoard />} />
      <Route path="/login" element={<Login />} />
    </Routes>
  )
}

export default App