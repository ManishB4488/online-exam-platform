export function percentOf(score, total) {
  return total > 0 ? Math.round((score * 100) / total) : 0
}