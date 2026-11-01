import axios from 'axios'

export const AUTH_KEY = 'auth'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
})

// Add the token to every request
api.interceptors.request.use((config) => {
  try {
    const saved = JSON.parse(localStorage.getItem(AUTH_KEY))
    if (saved?.token) {
      config.headers.Authorization = `Bearer ${saved.token}`
    }
  } catch {
    // ignore a broken saved value
  }
  return config
})

// If the token is no longer valid, log out and go to the login page
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthCall = error.config?.url?.startsWith('/api/auth/')
    if (error.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem(AUTH_KEY)
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export default api