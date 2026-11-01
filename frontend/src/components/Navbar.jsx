import { Link, useNavigate } from 'react-router-dom'
import useAuth from '../context/useAuth'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="navbar">
      <div className="nav-left">
        <span className="brand">Online Exam Platform</span>
        {user?.role === 'STUDENT' && (
          <nav className="nav-links">
            <Link to="/student">Exams</Link>
            <Link to="/student/results">My results</Link>
          </nav>
        )}
        {user?.role === 'ADMIN' && (
          <nav className="nav-links">
            <Link to="/admin">Exams</Link>
            <Link to="/admin/results">All results</Link>
          </nav>
        )}
      </div>
      {user && (
        <div className="nav-right">
          <span>
            {user.name} ({user.role})
          </span>
          <button className="btn btn-small" onClick={handleLogout}>
            Log out
          </button>
        </div>
      )}
    </header>
  )
}