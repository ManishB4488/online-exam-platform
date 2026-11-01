import { useEffect, useState } from 'react'
import api from '../api/client'
import { errorMessage } from '../api/errors'
import Navbar from '../components/Navbar'
import useAuth from '../context/useAuth'

export default function StudentHome() {
  const { user } = useAuth()
  const [exams, setExams] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    api
      .get('/api/student/exams')
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
        <h1>Welcome, {user.name}</h1>
        <div className="card">
          <h2>Available exams</h2>
          {error && <div className="error">{error}</div>}
          {!error && exams === null && <p className="muted">Loading...</p>}
          {exams && exams.length === 0 && (
            <p className="muted">No exams are available yet.</p>
          )}
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
          <p className="muted">Taking an exam comes in Step 7.</p>
        </div>
      </main>
    </>
  )
}