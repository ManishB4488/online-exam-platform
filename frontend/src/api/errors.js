export function errorMessage(err) {
  if (!err.response) {
    return 'Cannot reach the server. Is the backend running?'
  }
  const { status, data } = err.response

  // Field problems, such as "title: must not be blank"
  if (status === 400 && data?.fields) {
    return Object.entries(data.fields)
      .map(([field, message]) => `${field}: ${message}`)
      .join('. ')
  }

  // A clear message from the server, such as "Email already registered"
  if (status < 500 && typeof data?.error === 'string' && data.error) {
    return data.error
  }

  if (status === 401) return 'Wrong email or password.'
  if (status === 403) return 'You are not allowed to do this.'
  return 'Something went wrong. Please try again.'
}