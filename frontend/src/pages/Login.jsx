import React, { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function Login() {
  const { user, login, error } = useAuth()
  const [email, setEmail] = useState('admin@efiscal.local')
  const [password, setPassword] = useState('Admin123!')
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    return <Navigate to="/orders" replace />
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    await login(email, password)
    setSubmitting(false)
  }

  return (
    <div className="centered-page">
      <form className="login-card" onSubmit={handleSubmit}>
        <h2>Sign in</h2>
        <p>Use the bootstrap superadmin account to start configuring clients, users, and integrations.</p>
        <div className="form-grid">
          <label className="field">
            <span>Email</span>
            <input value={email} onChange={(event) => setEmail(event.target.value)} />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
          </label>
        </div>
        {error ? <p className="error-text">{error}</p> : null}
        <div className="inline-actions">
          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? 'Signing in...' : 'Login'}
          </button>
        </div>
      </form>
    </div>
  )
}
