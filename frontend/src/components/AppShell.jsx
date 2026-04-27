import React, { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const navItems = [
  { path: '/orders', label: 'Orders' },
  {
    label: 'Fiscal Bills',
    children: [
      { path: '/fiscal-bills/get-status', label: 'Get Status' },
    ],
  },
  { path: '/account', label: 'Account' },
  { path: '/users', label: 'Users' },
  { path: '/clients', label: 'Clients' },
  { path: '/organizations', label: 'Organizations' },
  { path: '/api-config', label: 'API Config' },
]

export default function AppShell({ title, subtitle, actions, children }) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  // Track which group menus are open; start with Fiscal Bills open if on that path
  const [openGroups, setOpenGroups] = useState(() => {
    const initial = {}
    navItems.forEach((item) => {
      if (item.children) {
        initial[item.label] = item.children.some((c) => location.pathname.startsWith(c.path))
      }
    })
    return initial
  })

  function toggleGroup(label) {
    setOpenGroups((prev) => ({ ...prev, [label]: !prev[label] }))
  }

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
            {navItems.map((item) => {
              if (item.children) {
                const isOpen = openGroups[item.label]
                const isGroupActive = item.children.some((c) => location.pathname.startsWith(c.path))
                return (
                  <div key={item.label}>
                    <button
                      className={`nav-link nav-group-toggle${isGroupActive ? ' active' : ''}`}
                      onClick={() => toggleGroup(item.label)}
                      style={{ width: '100%', textAlign: 'left', background: 'none', border: 'none', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                    >
                      <span>{item.label}</span>
                      <span style={{ fontSize: '0.7rem', opacity: 0.7 }}>{isOpen ? '▾' : '▸'}</span>
                    </button>
                    {isOpen && (
                      <div className="nav-submenu" style={{ paddingLeft: '1rem' }}>
                        {item.children.map((child) => (
                          <Link
                            key={child.path}
                            to={child.path}
                            className={location.pathname === child.path ? 'nav-link active' : 'nav-link'}
                          >
                            {child.label}
                          </Link>
                        ))}
                      </div>
                    )}
                  </div>
                )
              }
              return (
                <Link key={item.path} to={item.path} className={location.pathname === item.path ? 'nav-link active' : 'nav-link'}>
                  {item.label}
                </Link>
              )
            })}
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
