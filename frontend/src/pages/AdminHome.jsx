import { useEffect, useState } from 'react'
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

  return (
    <>
      <Navbar />
      <main className="page">
        <h1>Admin panel, {user.name}</h1>
        <div className="card">
          <h2>All exams</h2>
          {error && <div className="error">{error}</div>}
          {!error && exams === null && <p className="muted">Loading...</p>}
          {exams && exams.length === 0 && <p className="muted">No exams yet.</p>}
          {exams && exams.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th>Exam</th>
                  <th>Duration</th>
                  <th>Questions</th>
                </tr>
              </thead>
              <tbody>
                {exams.map((e) => (
                  <tr key={e.id}>
                    <td>{e.title}</td>
                    <td>{e.durationMinutes} min</td>
                    <td>{e.questionCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <p className="muted">Creating exams and questions comes in Step 8.</p>
        </div>
      </main>
    </>
  )
}