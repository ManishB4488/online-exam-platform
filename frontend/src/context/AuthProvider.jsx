import { useState } from 'react'
import api, { AUTH_KEY } from '../api/client'
import { AuthContext } from './authContext'

function readSavedLogin() {
  try {
    const raw = localStorage.getItem(AUTH_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(readSavedLogin)

  const saveLogin = (data) => {
    localStorage.setItem(AUTH_KEY, JSON.stringify(data))
    setUser(data)
  }

  const login = async (email, password) => {
    const { data } = await api.post('/api/auth/login', { email, password })
    saveLogin(data)
    return data
  }

  const register = async (name, email, password) => {
    const { data } = await api.post('/api/auth/register', { name, email, password })
    saveLogin(data)
    return data
  }

  const logout = () => {
    localStorage.removeItem(AUTH_KEY)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}