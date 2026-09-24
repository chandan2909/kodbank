import { createContext, useContext, useState, useCallback } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('atm_token'))
  const [cardNumber, setCardNumber] = useState(() => localStorage.getItem('atm_card'))
  const [username, setUsername] = useState(() => localStorage.getItem('atm_username'))
  const [role, setRole] = useState(() => localStorage.getItem('atm_role') || 'USER')

  const applySession = useCallback((data) => {
    localStorage.setItem('atm_token', data.token)
    localStorage.setItem('atm_refresh', data.refreshToken || '')
    localStorage.setItem('atm_card', data.cardNumber || '')
    localStorage.setItem('atm_username', data.username || '')
    localStorage.setItem('atm_role', data.role || 'USER')
    setToken(data.token)
    setCardNumber(data.cardNumber || '')
    setUsername(data.username || '')
    setRole(data.role || 'USER')
    return data
  }, [])

  const login = useCallback(async ({ cardNumber: card, username: user, pin }) => {
    const body = { pin }
    if (user) body.username = user
    else body.cardNumber = card
    const { data } = await api.post('/auth/login', body)
    return applySession(data)
  }, [applySession])

  const logout = useCallback(() => {
    localStorage.removeItem('atm_token')
    localStorage.removeItem('atm_refresh')
    localStorage.removeItem('atm_card')
    localStorage.removeItem('atm_username')
    localStorage.removeItem('atm_role')
    setToken(null)
    setCardNumber(null)
    setUsername(null)
    setRole('USER')
  }, [])

  const isAuthenticated = Boolean(token)
  const isAdmin = role === 'ADMIN'

  return (
    <AuthContext.Provider value={{
      token,
      cardNumber,
      username,
      role,
      isAdmin,
      isAuthenticated,
      login,
      logout,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
