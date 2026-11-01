import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="auth-wrap">
      <div className="card auth-card">
        <h1>Page not found</h1>
        <p className="muted">This page does not exist.</p>
        <Link to="/">Go to the home page</Link>
      </div>
    </div>
  )
}