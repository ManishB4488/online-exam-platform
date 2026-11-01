import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../api/client'
import { errorMessage } from '../api/errors'
import Navbar from '../components/Navbar'

const EMPTY_QUESTION = {
  text: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  correctOption: 'A',
  marks: 1,
}

function QuestionForm({ initial, onSave, onCancel }) {
  const [form, setForm] = useState(initial)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const change = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      await onSave({ ...form, marks: Number(form.marks) })
    } catch (err) {
      setError(errorMessage(err))
      setSaving(false)
    }
  }

  return (
    <div className="card question">
      {error && <div className="error">{error}</div>}
      <form onSubmit={handleSubmit}>
        <label htmlFor="q-text">Question</label>
        <textarea id="q-text" rows={3} value={form.text} onChange={change('text')} required />

        {['A', 'B', 'C', 'D'].map((letter) => (
          <div key={letter}>
            <label htmlFor={`q-opt-${letter}`}>Option {letter}</label>
            <input
              id={`q-opt-${letter}`}
              type="text"
              value={form['option' + letter]}
              onChange={change('option' + letter)}
              required
            />
          </div>
        ))}

        <div className="form-row-2">
          <div>
            <label htmlFor="q-correct">Correct option</label>
            <select id="q-correct" value={form.correctOption} onChange={change('correctOption')}>
              <option value="A">A</option>
              <option value="B">B</option>
              <option value="C">C</option>
              <option value="D">D</option>
            </select>
          </div>
          <div>
            <label htmlFor="q-marks">Marks</label>
            <input
              id="q-marks"
              type="number"
              min="1"
              value={form.marks}
              onChange={change('marks')}
              required
            />
          </div>
        </div>

        <div className="form-actions">
          <button className="btn" type="submit" disabled={saving}>
            {saving ? 'Saving...' : 'Save question'}
          </button>
          <button className="btn btn-secondary" type="button" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}

export default function ManageQuestions() {
  const { id } = useParams()
  const [exam, setExam] = useState(null)
  const [questions, setQuestions] = useState(null)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(null) // null, 'new', or a question

  useEffect(() => {
    let cancelled = false
    Promise.all([
      api.get(`/api/admin/exams/${id}`),
      api.get(`/api/admin/exams/${id}/questions`),
    ])
      .then(([examRes, questionsRes]) => {
        if (!cancelled) {
          setExam(examRes.data)
          setQuestions(questionsRes.data)
        }
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

  const handleSave = async (body) => {
    if (editing === 'new') {
      const { data } = await api.post(`/api/admin/exams/${id}/questions`, body)
      setQuestions([...questions, data])
    } else {
      const { data } = await api.put(`/api/admin/questions/${editing.id}`, body)
      setQuestions(questions.map((q) => (q.id === data.id ? data : q)))
    }
    setEditing(null)
  }

  const handleDelete = async (question) => {
    if (!window.confirm('Delete this question?')) return
    try {
      await api.delete(`/api/admin/questions/${question.id}`)
      setQuestions(questions.filter((q) => q.id !== question.id))
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  if (!exam || !questions) {
    return (
      <>
        <Navbar />
        <main className="page">
          {error ? (
            <div className="card">
              <div className="error">{error}</div>
              <Link to="/admin">Back to exams</Link>
            </div>
          ) : (
            <p className="muted">Loading...</p>
          )}
        </main>
      </>
    )
  }

  return (
    <>
      <Navbar />
      <main className="page">
        <p>
          <Link to="/admin">&larr; Back to exams</Link>
        </p>
        <div className="page-head">
          <h1>Questions: {exam.title}</h1>
          {editing === null && (
            <button className="btn" onClick={() => setEditing('new')}>
              Add question
            </button>
          )}
        </div>
        <p className="muted">
          Students only see an exam after it has at least one question.
        </p>

        {error && <div className="error">{error}</div>}

        {editing !== null && (
          <QuestionForm
            key={editing === 'new' ? 'new' : editing.id}
            initial={editing === 'new' ? EMPTY_QUESTION : editing}
            onSave={handleSave}
            onCancel={() => setEditing(null)}
          />
        )}

        {questions.length === 0 && editing === null && (
          <div className="card">
            <p className="muted">No questions yet. Click "Add question".</p>
          </div>
        )}

        {questions.map((q, index) => (
          <div className="card question" key={q.id}>
            <p className="q-text">
              <strong>{index + 1}.</strong> {q.text}{' '}
              <span className="muted">
                ({q.marks} mark{q.marks > 1 ? 's' : ''})
              </span>
            </p>
            <ul className="opt-list">
              {['A', 'B', 'C', 'D'].map((letter) => (
                <li key={letter} className={q.correctOption === letter ? 'opt-correct' : ''}>
                  {letter}. {q['option' + letter]}
                  {q.correctOption === letter && ' (correct)'}
                </li>
              ))}
            </ul>
            <div className="actions">
              <button
                className="btn btn-small btn-secondary"
                onClick={() => setEditing(q)}
                disabled={editing !== null}
              >
                Edit
              </button>
              <button
                className="btn btn-small btn-danger"
                onClick={() => handleDelete(q)}
                disabled={editing !== null}
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </main>
    </>
  )
}