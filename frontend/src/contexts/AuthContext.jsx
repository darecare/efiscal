import React, { createContext, useContext, useEffect, useState } from 'react'
import { authApi } from '../services/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notification, setNotification] = useState(null)
  const [notificationTimeoutId, setNotificationTimeoutId] = useState(null)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) {
      setLoading(false)
      return
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => {
        localStorage.removeItem('token')
        setUser(null)
      })
      .finally(() => setLoading(false))
  }, [])

  const showNotification = (message, type = 'success') => {
    if (notificationTimeoutId) {
      clearTimeout(notificationTimeoutId)
    }
    setNotification({ message, type })
    const id = setTimeout(() => {
      setNotification(null)
      setNotificationTimeoutId(null)
    }, 4000)
    setNotificationTimeoutId(id)
  }

  const clearNotification = () => {
    if (notificationTimeoutId) {
      clearTimeout(notificationTimeoutId)
      setNotificationTimeoutId(null)
    }
    setNotification(null)
  }

  async function login(email, password) {
    try {
      setError('')
      const result = await authApi.login(email, password)
      localStorage.setItem('token', result.accessToken)
      setUser(result.user)
      return true
    } catch (loginError) {
      setError(loginError.response?.data?.message || 'Login failed')
      return false
    }
  }

  function logout() {
    localStorage.removeItem('token')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, error, login, logout, notification, showNotification, clearNotification }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
