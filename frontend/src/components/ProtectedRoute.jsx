import { Navigate, Outlet } from 'react-router-dom'
import useAuth from '../context/useAuth'
import { homeFor } from '../utils/roles'

export default function ProtectedRoute({ role }) {
  const { user } = useAuth()

  if (!user) return <Navigate to="/login" replace />
  if (role && user.role !== role) return <Navigate to={homeFor(user.role)} replace />

  return <Outlet />
}