import React, { createContext, useContext, useEffect, useState } from 'react'

const AuthContext = createContext(null)

export default function AuthProvider({ children }) {
  const [token, setToken] = useState(null)

  const login = (newToken) => setToken(newToken)
  const logout = () => setToken(null)

  useEffect(() => {
    window.addEventListener('auth:expired', logout)
    return () => {
      window.removeEventListener('auth:expired', logout)
    }
  }, [])

  return (
    <AuthContext.Provider value={{ token, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
