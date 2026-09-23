import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    try {
      await api.get('/auth/csrf')
      const { data } = await api.get('/auth/me')
      setUser(data)
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])
  useEffect(() => {
    const unauthorized = () => setUser(null)
    window.addEventListener('libertyreckoner:unauthorized', unauthorized)
    return () => window.removeEventListener('libertyreckoner:unauthorized', unauthorized)
  }, [])

  const login = async credentials => {
    const { data } = await api.post('/auth/login', credentials)
    setUser(data.user)
    return data.user
  }

  const logout = async () => {
    try { await api.post('/auth/logout') } finally { setUser(null) }
  }

  const value = useMemo(() => ({ user, loading, login, logout, refresh }), [user, loading, refresh])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
