import React, { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { userService } from '../services/api'
import { IconDashboard, IconOrders, IconAccount, IconUsers, IconProfile, IconLogOut } from '../components/Icons'

const Account = () => {
  const { user, logout, refreshUser } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [formState, setFormState] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (user) {
      setFormState((prev) => ({
        ...prev,
        username: user.username || '',
        email: user.email || '',
        password: '',
        confirmPassword: '',
      }))
    }
  }, [user])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormState((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!user) return

    if (formState.password && formState.password !== formState.confirmPassword) {
      setError('Passwords must match')
      return
    }

    const payload = {
      username: formState.username,
      email: formState.email,
    }

    if (formState.password) {
      payload.password = formState.password
    }

    try {
      setSaving(true)
      setError('')
      setMessage('')
      await userService.updateUser(user.id, payload)
      setMessage('User account details have been updated.')
      setFormState((prev) => ({
        ...prev,
        password: '',
        confirmPassword: '',
      }))
      await refreshUser?.()
    } catch (err) {
      const serverMessage = err.response?.data?.detail || 'Failed to update account'
      setError(serverMessage)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="dashboard-layout">
      <nav className="navbar">
        <h1><span className="title-brand">Kliklak</span> <span className="title-product">Dashboard</span></h1>
        <div className="navbar-actions">
          <span className="navbar-welcome">Dobrodošli, {user?.username}</span>
          <Link to="/account" className="account-link" aria-label="Nalog">
            <IconProfile />
          </Link>
          <button onClick={handleLogout} className="btn btn-nav">
            <IconLogOut /> Izlogujte se
          </button>
        </div>
      </nav>

      <div className="main-layout">
        <aside className="sidebar">
          <ul className="sidebar-menu">
            <li>
              <Link to="/dashboard" className={location.pathname === '/dashboard' ? 'active' : ''}>
                <IconDashboard /> Kontrolni panel
              </Link>
            </li>
            <li>
              <Link to="/orders" className={location.pathname === '/orders' ? 'active' : ''}>
                <IconOrders /> Obrada porudžbina
              </Link>
            </li>
            <li>
              <Link to="/account" className={location.pathname === '/account' ? 'active' : ''}>
                <IconAccount /> Nalog
              </Link>
            </li>
            {user?.role === 'superuser' && (
              <li>
                <Link to="/users" className={location.pathname === '/users' ? 'active' : ''}>
                  <IconUsers /> Korisnici
                </Link>
              </li>
            )}
          </ul>
        </aside>

        <main className="dashboard-content">
          <div className="card account-card">
            <h3>Account Settings</h3>
            <p>Update your username, email, or password</p>

            {message && <div className="success-message">{message}</div>}
            {error && <div className="error-message">{error}</div>}

            <form className="account-form" onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="username">Username</label>
                <input
                  id="username"
                  name="username"
                  type="text"
                  value={formState.username}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="email">Email</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={formState.email}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="password">New Password</label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  value={formState.password}
                  onChange={handleChange}
                  placeholder="Leave blank to keep current password"
                />
              </div>

              <div className="form-group">
                <label htmlFor="confirmPassword">Confirm Password</label>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  value={formState.confirmPassword}
                  onChange={handleChange}
                  placeholder="Repeat new password"
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="btn" disabled={saving}>
                  {saving ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </main>
      </div>
    </div>
  )
}

export default Account
