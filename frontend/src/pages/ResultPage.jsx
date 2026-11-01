import { Link, Navigate, useLocation } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { percentOf } from '../utils/format'

export default function ResultPage() {
  const { state } = useLocation()

  // Opened without taking an exam? Show the list of past results instead
  if (!state?.result) return <Navigate to="/student/results" replace />

  const { result, questions } = state
  const questionById = new Map(questions.map((q) => [q.id, q]))
  const percent = percentOf(result.score, result.totalMarks)

  return (
    <>
      <Navbar />
      <main className="page">
        <h1>{result.examTitle}: your result</h1>

        <div className="card score-card">
          <div className="score">
            {result.score} / {result.totalMarks}
          </div>
          <p>
            {percent}% &middot; {result.correctCount} of {result.totalQuestions} questions
            correct
          </p>
        </div>

        <h2>Review</h2>
        {result.review.map((item, index) => {
          const q = questionById.get(item.questionId)
          const optionText = (letter) =>
            q && letter ? `${letter}. ${q['option' + letter]}` : 'Not answered'
          return (
            <div
              className={item.correct ? 'card review right' : 'card review wrong'}
              key={item.questionId}
            >
              <p>
                <strong>{index + 1}.</strong> {q?.text}
              </p>
              <p>Your answer: {optionText(item.selectedOption)}</p>
              {!item.correct && <p>Correct answer: {optionText(item.correctOption)}</p>}
              <span className="badge">{item.correct ? 'Correct' : 'Wrong'}</span>
            </div>
          )
        })}

        <Link className="btn btn-link" to="/student">
          Back to exams
        </Link>
      </main>
    </>
  )
}