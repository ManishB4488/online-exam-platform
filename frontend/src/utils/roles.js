// Where each role lands after login
export function homeFor(role) {
  return role === 'ADMIN' ? '/admin' : '/student'
}