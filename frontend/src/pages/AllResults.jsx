import { useEffect, useState } from 'react'
import api from '../api/client'
import { errorMessage } from '../api/errors'
import Navbar from '../components/Navbar'
import { percentOf } from '../utils/format'

export default function AllResults() {
  const [results, setResults] = useState(null)
  const [error, setError] = useState('')
  const [examFilter, setExamFilter] = useState('')

  useEffect(() => {
    let cancelled = false
    api
      .get('/api/admin/results')
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

  const titles = results ? [...new Set(results.map((r) => r.examTitle))] : []
  const shown = results ? results.filter((r) => !examFilter || r.examTitle === examFilter) : []

  return (
    <>
      <Navbar />
      <main className="page page-wide">
        <h1>All results</h1>
        <div className="card">
          {error && <div className="error">{error}</div>}
          {!error && results === null && <p className="muted">Loading...</p>}
          {results && results.length === 0 && (
            <p className="muted">No student has taken an exam yet.</p>
          )}
          {results && results.length > 0 && (
            <>
              <label htmlFor="filter">Show results for</label>
              <select
                id="filter"
                className="filter"
                value={examFilter}
                onChange={(e) => setExamFilter(e.target.value)}
              >
                <option value="">All exams</option>
                {titles.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>

              <table>
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Exam</th>
                    <th>Score</th>
                    <th>Percent</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {shown.map((r) => (
                    <tr key={r.id}>
                      <td>
                        {r.studentName}
                        <br />
                        <span className="muted">{r.studentEmail}</span>
                      </td>
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
            </>
          )}
        </div>
      </main>
    </>
  )
}