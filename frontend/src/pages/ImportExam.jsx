import { useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client'
import { errorMessage } from '../api/errors'
import HoopDrop from '../components/HoopDrop'
import Navbar from '../components/Navbar'

const EXAMPLE = `1. Which keyword is used to create an object in Java?
A) new
B) class
C) this
D) static
Answer: A`

function formatSize(bytes) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function ImportExam() {
  const [file, setFile] = useState(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [duration, setDuration] = useState(30)
  const [marks, setMarks] = useState(1)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(null)

  // Called when the shot goes into the hoop
  const handleFileLoaded = (loaded) => {
    setError('')
    setFile(loaded)
    // Use the file name as the exam title, if the title is still empty
    if (title === '') {
      setTitle(loaded.name.replace(/\.pdf$/i, '').replace(/[_-]+/g, ' '))
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!file) {
      setError('Shoot a PDF into the hoop first.')
      return
    }
    setError('')
    setLoading(true)

    const form = new FormData()
    form.append('file', file)
    form.append('title', title)
    form.append('description', description)
    form.append('durationMinutes', duration)
    form.append('marksPerQuestion', marks)

    try {
      const { data } = await api.post('/api/admin/exams/import', form)
      setDone(data)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const importAnother = () => {
    setDone(null)
    setFile(null)
    setTitle('')
    setDescription('')
  }

  if (done) {
    return (
      <>
        <Navbar />
        <main className="page">
          <h1>Exam created</h1>
          <div className="card">
            <p>
              <strong>{done.title}</strong> was created with{' '}
              <strong>{done.questionCount}</strong> questions.
            </p>
            {done.warnings.length > 0 && (
              <div className="warn">
                Some parts of the PDF were skipped:
                <ul>
                  {done.warnings.map((w, i) => (
                    <li key={i}>{w}</li>
                  ))}
                </ul>
                You can add those questions by hand on the Questions page.
              </div>
            )}
            <div className="actions">
              <Link className="btn btn-link" to={`/admin/exams/${done.examId}/questions`}>
                Review the questions
              </Link>
              <Link className="btn btn-link btn-secondary" to="/admin">
                Back to exams
              </Link>
              <button className="btn btn-secondary" onClick={importAnother}>
                Import another PDF
              </button>
            </div>
          </div>
        </main>
      </>
    )
  }

  return (
    <>
      <Navbar />
      <main className="page">
        <h1>Import exam from PDF</h1>

        <div className="hint">
          <strong>How to prepare your PDF</strong>
          <ul>
            <li>It must be a text PDF. A scan or a photo of pages will not work.</li>
            <li>Number the questions 1, 2, 3 ... in order.</li>
            <li>Give every question four options: A, B, C and D.</li>
            <li>
              Write the right answer after each question, or put an &quot;Answer Key&quot; at the
              end, like <code>1-B 2-C 3-A</code>.
            </li>
          </ul>
          <pre>{EXAMPLE}</pre>
        </div>

        <div className="card">
          {error && <div className="error">{error}</div>}

          <p className="muted">
            Drop a PDF on this page or choose one. Then pull it back and shoot it into the hoop to
            load it.
          </p>
          <HoopDrop onFile={handleFileLoaded} onError={setError} />

          {file ? (
            <div className="loaded">
              <span className="loaded-badge">PDF</span>
              <div className="dz-info">
                <strong>{file.name}</strong>
                <span className="muted">{formatSize(file.size)} &middot; ready</span>
              </div>
              <button
                type="button"
                className="btn btn-small btn-secondary"
                onClick={() => setFile(null)}
              >
                Remove
              </button>
            </div>
          ) : (
            <p className="muted">No file loaded yet.</p>
          )}

          <form onSubmit={handleSubmit}>
            <label htmlFor="title">Exam title</label>
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
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />

            <div className="form-row-2">
              <div>
                <label htmlFor="duration">Duration (minutes)</label>
                <input
                  id="duration"
                  type="number"
                  min="1"
                  value={duration}
                  onChange={(e) => setDuration(e.target.value)}
                  required
                />
              </div>
              <div>
                <label htmlFor="marks">Marks for each question</label>
                <input
                  id="marks"
                  type="number"
                  min="1"
                  value={marks}
                  onChange={(e) => setMarks(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-actions">
              <button className="btn" type="submit" disabled={loading}>
                {loading ? 'Reading the PDF...' : 'Create exam from PDF'}
              </button>
              <Link className="btn btn-link btn-secondary" to="/admin">
                Cancel
              </Link>
            </div>
          </form>
        </div>
      </main>
    </>
  )
}
