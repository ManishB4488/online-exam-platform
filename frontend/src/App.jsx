import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import useAuth from './context/useAuth'
import AdminHome from './pages/AdminHome'
import Login from './pages/Login'
import NotFound from './pages/NotFound'
import Register from './pages/Register'
import StudentHome from './pages/StudentHome'
import { homeFor } from './utils/roles'

// "/" sends you to your home page, or to the login page if you are logged out
function Home() {
  const { user } = useAuth()
  return <Navigate to={user ? homeFor(user.role) : '/login'} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<ProtectedRoute role="STUDENT" />}>
        <Route path="/student" element={<StudentHome />} />
      </Route>

      <Route element={<ProtectedRoute role="ADMIN" />}>
        <Route path="/admin" element={<AdminHome />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}