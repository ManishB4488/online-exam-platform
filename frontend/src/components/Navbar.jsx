import { useNavigate } from 'react-router-dom'
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
      <span className="brand">Online Exam Platform</span>
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