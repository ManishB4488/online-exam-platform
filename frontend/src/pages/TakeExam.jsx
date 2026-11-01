import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import api from '../api/client'
import { errorMessage } from '../api/errors'
import Navbar from '../components/Navbar'

function formatTime(total) {
  const minutes = Math.floor(total / 60)
  const seconds = total % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

// The exam screen: questions, answers and the countdown timer
function ExamScreen({ exam }) {
  const navigate = useNavigate()
  const [answers, setAnswers] = useState({}) // { questionId: "A" }
  const [secondsLeft, setSecondsLeft] = useState(exam.secondsLeft)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const submitRef = useRef(null)

  const submit = async () => {
    if (submitting) return
    setSubmitting(true)
    setError('')
    try {
      const body = {
        answers: Object.entries(answers).map(([questionId, selectedOption]) => ({
          questionId: Number(questionId),
          selectedOption,
        })),
      }
      const { data } = await api.post(`/api/student/exams/${exam.id}/submit`, body)
      navigate('/student/result', {
        replace: true,
        state: { result: data, questions: exam.questions },
      })
    } catch (err) {
      setError(errorMessage(err))
      setSubmitting(false)
    }
  }

  // Keep the newest submit function, so the timer always submits the latest answers
  useEffect(() => {
    submitRef.current = submit
  })

  // Countdown. The time is worked out from the end time, so it stays correct
  // even if the browser slows down the timer in a background tab.
  useEffect(() => {
    const timer = setInterval(() => {
      const left = Math.max(0, Math.ceil((exam.endsAt - Date.now()) / 1000))
      setSecondsLeft(left)
      if (left === 0) {
        clearInterval(timer)
        submitRef.current()
      }
    }, 1000)
    return () => clearInterval(timer)
  }, [exam.endsAt])

  const handleSubmitClick = () => {
    const unanswered = exam.questions.length - Object.keys(answers).length
    if (
      unanswered > 0 &&
      !window.confirm(`You have ${unanswered} unanswered question(s). Submit anyway?`)
    ) {
      return
    }
    submit()
  }

  return (
    <>
      <header className="navbar exam-bar">
        <span className="brand">{exam.title}</span>
        <span className={secondsLeft <= 60 ? 'timer timer-low' : 'timer'}>
          Time left: {formatTime(secondsLeft)}
        </span>
      </header>
      <main className="page">
        {error && <div className="error">{error}</div>}

        {exam.questions.map((q, index) => (
          <div className="card question" key={q.id}>
            <p className="q-text">
              <strong>{index + 1}.</strong> {q.text}{' '}
              <span className="muted">
                ({q.marks} mark{q.marks > 1 ? 's' : ''})
              </span>
            </p>
            {['A', 'B', 'C', 'D'].map((letter) => (
              <label className="option" key={letter}>
                <input
                  type="radio"
                  name={`q-${q.id}`}
                  value={letter}
                  checked={answers[q.id] === letter}
                  onChange={() => setAnswers({ ...answers, [q.id]: letter })}
                />
                <span>
                  {letter}. {q['option' + letter]}
                </span>
              </label>
            ))}
          </div>
        ))}

        <button className="btn" onClick={handleSubmitClick} disabled={submitting}>
          {submitting ? 'Submitting...' : 'Submit exam'}
        </button>
      </main>
    </>
  )
}

export default function TakeExam() {
  const { id } = useParams()
  const [exam, setExam] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    api
      .get(`/api/student/exams/${id}/start`)
      .then((res) => {
        if (!cancelled) {
          // The timer ends exactly "duration" minutes after the exam is opened
          setExam({
            ...res.data,
            endsAt: Date.now() + res.data.secondsLeft * 1000,
          })
        }
      })
      .catch((err) => {
        if (cancelled) return
        const status = err.response?.status
        if (status === 404) setError('This exam was not found.')
        else if (status === 400) setError('This exam has no questions yet.')
        else setError(errorMessage(err))
      })
    return () => {
      cancelled = true
    }
  }, [id])

  if (exam) return <ExamScreen exam={exam} />

  return (
    <>
      <Navbar />
      <main className="page">
        {error ? (
          <div className="card">
            <div className="error">{error}</div>
            <Link to="/student">Back to exams</Link>
          </div>
        ) : (
          <p className="muted">Loading exam...</p>
        )}
      </main>
    </>
  )
}