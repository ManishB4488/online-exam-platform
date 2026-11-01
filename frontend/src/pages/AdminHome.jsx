import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client'
import { errorMessage } from '../api/errors'
import Navbar from '../components/Navbar'
import useAuth from '../context/useAuth'

export default function AdminHome() {
  const { user } = useAuth()
  const [exams, setExams] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    api
      .get('/api/admin/exams')
      .then((res) => {
        if (!cancelled) setExams(res.data)
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err))
      })
    return () => {
      cancelled = true
    }
  }, [])

  const handleDelete = async (exam) => {
    const ok = window.confirm(
      `Delete "${exam.title}"? Its questions and all student results for it will be deleted too.`,
    )
    if (!ok) return
    try {
      await api.delete(`/api/admin/exams/${exam.id}`)
      setExams(exams.filter((e) => e.id !== exam.id))
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <>
      <Navbar />
      <main className="page">
        <div className="page-head">
          <h1>Admin panel, {user.name}</h1>
          <Link className="btn btn-link" to="/admin/exams/new">
            New exam
          </Link>
        </div>

        <div className="card">
          <h2>All exams</h2>
          {error && <div className="error">{error}</div>}
          {!error && exams === null && <p className="muted">Loading...</p>}
          {exams && exams.length === 0 && (
            <p className="muted">No exams yet. Click "New exam" to create one.</p>
          )}
          {exams && exams.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th>Exam</th>
                  <th>Duration</th>
                  <th>Questions</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {exams.map((e) => (
                  <tr key={e.id}>
                    <td>{e.title}</td>
                    <td>{e.durationMinutes} min</td>
                    <td>{e.questionCount}</td>
                    <td>
                      <div className="actions">
                        <Link className="btn btn-link btn-tight" to={`/admin/exams/${e.id}/questions`}>
                          Questions
                        </Link>
                        <Link
                          className="btn btn-link btn-tight btn-secondary"
                          to={`/admin/exams/${e.id}/edit`}
                        >
                          Edit
                        </Link>
                        <button
                          className="btn btn-tight btn-danger"
                          onClick={() => handleDelete(e)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </main>
    </>
  )
}