export function errorMessage(err) {
  if (!err.response) {
    return 'Cannot reach the server. Is the backend running?'
  }
  const { status, data } = err.response
  if (status === 400 && data?.fields) {
    return Object.entries(data.fields)
      .map(([field, message]) => `${field}: ${message}`)
      .join('. ')
  }
  if (status === 401) return 'Wrong email or password.'
  if (status === 403) return 'You are not allowed to do this.'
  if (status === 409) return 'This email is already registered.'
  return 'Something went wrong. Please try again.'
}