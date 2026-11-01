import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import useAuth from './context/useAuth'
import AdminHome from './pages/AdminHome'
import AllResults from './pages/AllResults'
import ExamForm from './pages/ExamForm'
import Login from './pages/Login'
import ManageQuestions from './pages/ManageQuestions'
import MyResults from './pages/MyResults'
import NotFound from './pages/NotFound'
import Register from './pages/Register'
import ResultPage from './pages/ResultPage'
import StudentHome from './pages/StudentHome'
import TakeExam from './pages/TakeExam'
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
        <Route path="/student/exams/:id" element={<TakeExam />} />
        <Route path="/student/result" element={<ResultPage />} />
        <Route path="/student/results" element={<MyResults />} />
      </Route>

      <Route element={<ProtectedRoute role="ADMIN" />}>
        <Route path="/admin" element={<AdminHome />} />
        <Route path="/admin/exams/new" element={<ExamForm />} />
        <Route path="/admin/exams/:id/edit" element={<ExamForm />} />
        <Route path="/admin/exams/:id/questions" element={<ManageQuestions />} />
        <Route path="/admin/results" element={<AllResults />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}