import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import api from '../api/client'
import { errorMessage } from '../api/errors'
import Navbar from '../components/Navbar'

function ExamFields({ exam, examId }) {
  const navigate = useNavigate()
  const [title, setTitle] = useState(exam.title)
  const [description, setDescription] = useState(exam.description ?? '')
  const [duration, setDuration] = useState(exam.durationMinutes)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    const body = { title, description, durationMinutes: Number(duration) }
    try {
      if (examId) {
        await api.put(`/api/admin/exams/${examId}`, body)
      } else {
        await api.post('/api/admin/exams', body)
      }
      navigate('/admin')
    } catch (err) {
      setError(errorMessage(err))
      setSaving(false)
    }
  }

  return (
    <div className="card">
      {error && <div className="error">{error}</div>}
      <form onSubmit={handleSubmit}>
        <label htmlFor="title">Title</label>
        <input
          id="title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />

        <label htmlFor="description">Description (optional)</label>
        <textarea
          id="description"
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <label htmlFor="duration">Duration (minutes)</label>
        <input
          id="duration"
          type="number"
          min="1"
          value={duration}
          onChange={(e) => setDuration(e.target.value)}
          required
        />

        <div className="form-actions">
          <button className="btn" type="submit" disabled={saving}>
            {saving ? 'Saving...' : 'Save exam'}
          </button>
          <Link className="btn btn-link btn-secondary" to="/admin">
            Cancel
          </Link>
        </div>
      </form>
    </div>
  )
}

export default function ExamForm() {
  const { id } = useParams() // there is an id only when we edit
  const [exam, setExam] = useState(
    id ? null : { title: '', description: '', durationMinutes: 10 },
  )
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    let cancelled = false
    api
      .get(`/api/admin/exams/${id}`)
      .then((res) => {
        if (!cancelled) setExam(res.data)
      })
      .catch((err) => {
        if (!cancelled) {
          setError(
            err.response?.status === 404 ? 'This exam was not found.' : errorMessage(err),
          )
        }
      })
    return () => {
      cancelled = true
    }
  }, [id])

  return (
    <>
      <Navbar />
      <main className="page">
        <h1>{id ? 'Edit exam' : 'New exam'}</h1>
        {error && (
          <div className="card">
            <div className="error">{error}</div>
            <Link to="/admin">Back to exams</Link>
          </div>
        )}
        {!error && exam === null && <p className="muted">Loading...</p>}
        {exam && <ExamFields exam={exam} examId={id} />}
      </main>
    </>
  )
}