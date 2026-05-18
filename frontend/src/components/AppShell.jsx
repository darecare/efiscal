import React, { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const getNavItems = (user) => {
  const hasAction = (action) => user?.actions?.includes(action) || user?.roleName === 'SUPERADMIN'

  return [
    { path: '/orders', label: 'Orders', show: hasAction('MERCHANTPRO_FETCH_ORDERS') || user?.roleName === 'SUPERADMIN' },
    {
      label: 'Fiscal Bills',
      show: hasAction('FISCAL_VIEW_BILLS') || hasAction('FISCAL_CREATE_BILL') || user?.roleName === 'SUPERADMIN',
      children: [
        { path: '/fiscal-bills', label: 'Fiscal Bills', show: hasAction('FISCAL_VIEW_BILLS') || user?.roleName === 'SUPERADMIN' },
        { path: '/fiscal-bills/create', label: 'Create Fiscal Bill', show: hasAction('FISCAL_CREATE_BILL') || user?.roleName === 'SUPERADMIN' },
        { path: '/fiscal-bills/get-status', label: 'Get Status', show: true },
        { path: '/taxes', label: 'Taxes', show: hasAction('ORGS_MANAGE') || user?.roleName === 'SUPERADMIN' },
      ],
    },
    {
      label: 'Administration',
      show: hasAction('USERS_MANAGE') || hasAction('ROLES_MANAGE') || hasAction('ORGS_MANAGE') || user?.roleName === 'SUPERADMIN',
      children: [
        { path: '/account', label: 'Account', show: true },
        { path: '/users', label: 'User', show: hasAction('USERS_MANAGE') || user?.roleName === 'SUPERADMIN' },
        { path: '/roles', label: 'Roles & Permissions', show: hasAction('ROLES_MANAGE') || user?.roleName === 'SUPERADMIN' },
        { path: '/organizations', label: 'Organization', show: hasAction('ORGS_MANAGE') || user?.roleName === 'SUPERADMIN' },
        { path: '/clients', label: 'Client', show: user?.roleName === 'SUPERADMIN' },
      ],
    },
    {
      label: 'Configuration',
      show: hasAction('ORGS_MANAGE') || user?.roleName === 'SUPERADMIN',
      children: [
        { path: '/api-config', label: 'API Configuration', show: hasAction('ORGS_MANAGE') || user?.roleName === 'SUPERADMIN' },
        { path: '/fiscal-bills/paytype-map', label: 'Payment Type Mapping', show: hasAction('ORGS_MANAGE') || user?.roleName === 'SUPERADMIN' },
      ],
    },
  ]
}

export default function AppShell({ title, subtitle, actions, children }) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const filteredNavItems = getNavItems(user)
    .filter((item) => item.show !== false)
    .map((item) => ({
      ...item,
      children: item.children ? item.children.filter((c) => c.show !== false) : null,
    }))

  // Track which group menus are open; start with Fiscal Bills open if on that path
  const [openGroups, setOpenGroups] = useState(() => {
    const initial = {}
    filteredNavItems.forEach((item) => {
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
            {filteredNavItems.map((item) => {
              if (item.children) {
                const isOpen = openGroups[item.label]
                const isGroupActive = item.children.some((c) => location.pathname.startsWith(c.path))
                return (
                  <div key={item.label}>
                    <button
                      className={`nav-link nav-group-toggle${isGroupActive ? ' active' : ''}`}
                      onClick={() => toggleGroup(item.label)}
                    >
                      <span>{item.label}</span>
                      <span style={{ fontSize: '0.7rem', opacity: 0.7 }}>{isOpen ? '▾' : '▸'}</span>
                    </button>
                    {isOpen && (
                      <div className="nav-submenu">
                        {item.children.map((child) => (
                          <Link
                            key={child.path}
                            to={child.path}
                            className={location.pathname === child.path ? 'nav-link nav-sublink active' : 'nav-link nav-sublink'}
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
