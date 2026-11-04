import { useEffect, useRef, useState } from 'react'

const G = 1600 // gravity
const R = 24 // ball size used for the rim bounce
const K = 18 // how strong the pull is
const MAX_PULL = 130 // the longest pull in pixels
const CHIP_W = 62
const CHIP_H = 80
const MAX_BYTES = 5 * 1024 * 1024

// The lines of the net, worked out once
const NET_LINES = (() => {
  const lines = []
  for (let i = 0; i <= 8; i++) {
    const t = 4 + i * 14
    const b = 26 + i * 8.5
    lines.push([t, 0, b, 76])
    lines.push([116 - (t - 4), 0, 94 - (b - 26), 76])
  }
  for (let y = 18; y < 80; y += 19) {
    lines.push([6 + y * 0.28, y, 114 - y * 0.28, y])
  }
  return lines
})()

function pullOf(px, py, start) {
  const dx = start.x - px
  const dy = start.y - py
  const length = Math.hypot(dx, dy)
  const k = length > MAX_PULL ? MAX_PULL / length : 1
  return { dx: dx * k, dy: dy * k, length: length * k }
}

/**
 * A basketball game for choosing a file.
 * Choose or drop a PDF, pull it back and shoot it into the hoop.
 * When the shot goes in, onFile(file) is called.
 */
export default function HoopDrop({ onFile, onError }) {
  const stageRef = useRef(null)
  const rimRef = useRef(null)
  const netRef = useRef(null)
  const canvasRef = useRef(null)
  const chipRef = useRef(null)
  const ballRef = useRef(null)
  const startRef = useRef(null)
  const rafRef = useRef(0)
  const msgTimerRef = useRef(0)
  const shakeTimerRef = useRef(0)
  const callbacks = useRef({})

  const [pending, setPending] = useState(null) // the file waiting to be shot
  const [held, setHeld] = useState(false)
  const [flying, setFlying] = useState(false)
  const [shaking, setShaking] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [message, setMessage] = useState('')

  const say = (text) => {
    setMessage(text)
    clearTimeout(msgTimerRef.current)
    msgTimerRef.current = setTimeout(() => setMessage(''), 2600)
  }

  const addFile = (file) => {
    if (!file || flying) return
    const isPdf = file.type === 'application/pdf' || /\.pdf$/i.test(file.name)
    if (!isPdf) {
      onError('Only PDF files are allowed.')
      return
    }
    if (file.size > MAX_BYTES) {
      onError('The file is too big. The maximum size is 5 MB.')
      return
    }
    onError('')
    setPending(file)
    say('Pull the file back and let it fly into the hoop.')
  }

  // The window listeners need the newest functions
  useEffect(() => {
    callbacks.current = { addFile, onFile }
  })

  // Dropping a file anywhere on the page chooses it (and stops the browser opening the PDF)
  useEffect(() => {
    const hasFiles = (e) => e.dataTransfer?.types?.includes('Files')
    const onDragEnter = (e) => {
      if (hasFiles(e)) setDragging(true)
    }
    const onDragOver = (e) => e.preventDefault()
    const onDragLeave = (e) => {
      if (!e.relatedTarget) setDragging(false)
    }
    const onDrop = (e) => {
      e.preventDefault()
      setDragging(false)
      callbacks.current.addFile(e.dataTransfer.files[0])
    }
    window.addEventListener('dragenter', onDragEnter)
    window.addEventListener('dragover', onDragOver)
    window.addEventListener('dragleave', onDragLeave)
    window.addEventListener('drop', onDrop)
    return () => {
      window.removeEventListener('dragenter', onDragEnter)
      window.removeEventListener('dragover', onDragOver)
      window.removeEventListener('dragleave', onDragLeave)
      window.removeEventListener('drop', onDrop)
    }
  }, [])

  // The drawing canvas always has the size of the stage
  useEffect(() => {
    const stage = stageRef.current
    const canvas = canvasRef.current
    const resize = () => {
      canvas.width = stage.clientWidth
      canvas.height = stage.clientHeight
    }
    resize()
    const observer = new ResizeObserver(resize)
    observer.observe(stage)
    return () => observer.disconnect()
  }, [])

  // Stop everything when the page is left
  useEffect(() => {
    return () => {
      cancelAnimationFrame(rafRef.current)
      clearTimeout(msgTimerRef.current)
      clearTimeout(shakeTimerRef.current)
    }
  }, [])

  // ---------- drawing and physics ----------

  const drawAim = (x, y, vx, vy) => {
    const canvas = canvasRef.current
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    if (!vx && !vy) return
    ctx.fillStyle = '#e8562a'
    for (let k = 1; k < 30; k++) {
      const t = k * 0.05
      const px = x + vx * t
      const py = y + vy * t + 0.5 * G * t * t
      if (py > canvas.height) break
      ctx.globalAlpha = 1 - k / 34
      ctx.beginPath()
      ctx.arc(px, py, 5 - k * 0.1, 0, 7)
      ctx.fill()
    }
    ctx.globalAlpha = 1
  }

  // Centre and half width of the rim, measured inside the stage
  const getRim = () => {
    const s = stageRef.current.getBoundingClientRect()
    const r = rimRef.current.getBoundingClientRect()
    return {
      x: r.left - s.left + r.width / 2,
      y: r.top - s.top + r.height / 2,
      w: r.width / 2,
    }
  }

  const swish = () => {
    const net = netRef.current
    net.classList.remove('swish')
    void net.getBoundingClientRect() // lets the animation start again
    net.classList.add('swish')
  }

  const shoot = (startX, startY, startVx, startVy) => {
    const file = pending
    const ball = ballRef.current
    const stage = stageRef.current
    const rim = getRim()
    let x = startX
    let y = startY
    let vx = startVx
    let vy = startVy
    let last = performance.now()
    let scored = false
    let rotation = 0
    let scale = 1

    ball.style.transform = `translate(${x - CHIP_W / 2}px, ${y - CHIP_H / 2}px)`
    setFlying(true)

    const frame = (now) => {
      const dt = Math.min((now - last) / 1000, 0.03)
      last = now
      const previousY = y
      vy += G * dt
      x += vx * dt
      y += vy * dt
      rotation += vx * dt * 0.3

      if (!scored) {
        // bounce off the two ends of the rim
        for (const edge of [rim.x - rim.w, rim.x + rim.w]) {
          const dx = x - edge
          const dy = y - rim.y
          const distance = Math.hypot(dx, dy)
          const minimum = R * 0.55 + 6
          if (distance < minimum && distance > 0) {
            const nx = dx / distance
            const ny = dy / distance
            const dot = vx * nx + vy * ny
            if (dot < 0) {
              vx = (vx - 1.7 * dot * nx) * 0.8
              vy = (vy - 1.7 * dot * ny) * 0.8
            }
            x = edge + nx * minimum
            y = rim.y + ny * minimum
          }
        }
        // it is a goal when the ball goes down through the middle of the rim
        if (previousY < rim.y && y >= rim.y && vy > 0 && Math.abs(x - rim.x) < rim.w - 10) {
          scored = true
          swish()
          vy = Math.min(vy, 320)
        }
      } else {
        // fall slowly through the net
        x += (rim.x - x) * Math.min(1, dt * 10)
        vy = Math.min(vy, 320)
        scale = Math.max(0.5, scale - dt * 0.9)
      }

      ball.style.transform =
        `translate(${x - CHIP_W / 2}px, ${y - CHIP_H / 2}px) rotate(${rotation}deg) scale(${scale})`

      if (scored && y > rim.y + 110) {
        setFlying(false)
        setPending(null)
        say(`Swish! ${file.name} is loaded.`)
        onFile(file)
        return
      }
      if (y > stage.clientHeight + 90 || x < -90 || x > stage.clientWidth + 90) {
        setFlying(false)
        setShaking(true)
        clearTimeout(shakeTimerRef.current)
        shakeTimerRef.current = setTimeout(() => setShaking(false), 450)
        say('Missed. Pull back a little differently.')
        return
      }
      rafRef.current = requestAnimationFrame(frame)
    }
    rafRef.current = requestAnimationFrame(frame)
  }

  // ---------- the file chip: pull back and release ----------

  const handlePointerDown = (e) => {
    const chip = chipRef.current
    chip.setPointerCapture(e.pointerId)
    const stageBox = stageRef.current.getBoundingClientRect()
    const box = chip.getBoundingClientRect()
    startRef.current = {
      x: box.left + box.width / 2 - stageBox.left,
      y: box.top + box.height / 2 - stageBox.top,
      left: stageBox.left,
      top: stageBox.top,
    }
    setHeld(true)
  }

  const handlePointerMove = (e) => {
    const s = startRef.current
    if (!s) return
    const p = pullOf(e.clientX - s.left, e.clientY - s.top, s)
    chipRef.current.style.transform = `translate(${-p.dx}px, ${-p.dy}px)`
    drawAim(s.x, s.y, p.dx * K, p.dy * K)
  }

  const finishPull = (e, cancelled) => {
    const s = startRef.current
    if (!s) return
    startRef.current = null
    setHeld(false)
    const p = pullOf(e.clientX - s.left, e.clientY - s.top, s)
    chipRef.current.style.transform = ''
    drawAim(0, 0, 0, 0)
    if (!cancelled && p.length > 14) {
      shoot(s.x, s.y, p.dx * K, p.dy * K)
    }
  }

  // Enter or Space on the chip: an automatic shot for keyboard users
  const handleClick = (e) => {
    if (e.detail !== 0) return // a mouse click is part of the pull, not a shot
    const stageBox = stageRef.current.getBoundingClientRect()
    const box = chipRef.current.getBoundingClientRect()
    const x = box.left + box.width / 2 - stageBox.left
    const y = box.top + box.height / 2 - stageBox.top
    const rim = getRim()
    const t = 1.1
    shoot(x, y, (rim.x - x) / t, (rim.y - y - 0.5 * G * t * t) / t)
  }

  const chipClass = `hd-chip${held ? ' hd-held' : ''}${flying ? ' hd-hidden' : ''}${shaking ? ' hd-shake' : ''}`

  return (
    <div className={dragging ? 'hd-stage hd-over' : 'hd-stage'} ref={stageRef}>
      <div className="hd-hoop" aria-hidden="true">
        <div className="hd-board" />
        <div className="hd-rim" ref={rimRef} />
        <svg className="hd-net" ref={netRef} viewBox="0 0 120 80" height="80">
          {NET_LINES.map((l, i) => (
            <line key={i} x1={l[0]} y1={l[1]} x2={l[2]} y2={l[3]} />
          ))}
        </svg>
      </div>

      <canvas className="hd-aim" ref={canvasRef} aria-hidden="true" />

      <div className="hd-msg" role="status" aria-live="polite">
        {message}
      </div>

      <div className={flying ? 'hd-chip hd-ball' : 'hd-chip hd-ball hd-hidden'} ref={ballRef} aria-hidden="true">
        <i>PDF</i>
        <span>{pending ? pending.name : ''}</span>
      </div>

      <div className="hd-tray">
        {pending ? (
          <button
            type="button"
            ref={chipRef}
            className={chipClass}
            aria-label={`${pending.name}. Drag back and release to shoot, or press Enter for an automatic shot.`}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={(e) => finishPull(e, false)}
            onPointerCancel={(e) => finishPull(e, true)}
            onClick={handleClick}
          >
            <i>PDF</i>
            <span>{pending.name}</span>
          </button>
        ) : (
          <div className="hd-slot" />
        )}
        <label className="hd-pick">
          Choose a PDF
          <input
            type="file"
            accept="application/pdf"
            aria-label="Choose a PDF"
            onChange={(e) => {
              addFile(e.target.files[0])
              e.target.value = ''
            }}
          />
        </label>
      </div>
    </div>
  )
}
