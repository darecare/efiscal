import React from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const navItems = [
  { path: '/orders', label: 'Orders' },
  { path: '/account', label: 'Account' },
  { path: '/users', label: 'Users' },
  { path: '/clients-orgs', label: 'Clients & Orgs' },
  { path: '/api-config', label: 'API Config' },
]

export default function AppShell({ title, subtitle, actions, children }) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <h1>eFiscal</h1>
          <p>{user?.email}</p>
        </div>
        <div className="topbar-actions">
          <span className="badge">{user?.roleName}</span>
          <button className="secondary-button" onClick={handleLogout}>Logout</button>
        </div>
      </header>
      <div className="shell-body">
        <aside className="sidebar">
          <nav>
            {navItems.map((item) => (
              <Link key={item.path} to={item.path} className={location.pathname === item.path ? 'nav-link active' : 'nav-link'}>
                {item.label}
              </Link>
            ))}
          </nav>
        </aside>
        <main className="content-area">
          <section className="page-header">
            <div>
              <h2>{title}</h2>
              {subtitle ? <p>{subtitle}</p> : null}
            </div>
            {actions ? <div className="page-actions">{actions}</div> : null}
          </section>
          {children}
        </main>
      </div>
    </div>
  )
}
