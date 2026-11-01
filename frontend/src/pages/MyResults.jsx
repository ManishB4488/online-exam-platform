import { useEffect, useState } from 'react'
import api from '../api/client'
import { errorMessage } from '../api/errors'
import Navbar from '../components/Navbar'
import { percentOf } from '../utils/format'

export default function MyResults() {
  const [results, setResults] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    api
      .get('/api/student/results')
      .then((res) => {
        if (!cancelled) setResults(res.data)
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
        <h1>My results</h1>
        <div className="card">
          {error && <div className="error">{error}</div>}
          {!error && results === null && <p className="muted">Loading...</p>}
          {results && results.length === 0 && (
            <p className="muted">You have not taken any exam yet.</p>
          )}
          {results && results.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th>Exam</th>
                  <th>Score</th>
                  <th>Percent</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {results.map((r) => (
                  <tr key={r.id}>
                    <td>{r.examTitle}</td>
                    <td>
                      {r.score} / {r.totalMarks}
                    </td>
                    <td>{percentOf(r.score, r.totalMarks)}%</td>
                    <td>{new Date(r.submittedAt).toLocaleString()}</td>
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