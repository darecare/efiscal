import React, { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { hasAction, hasAnyAction } from '../utils/permissions'

const getNavItems = (user) => {
  const can = (action) => hasAction(user, action)

  return [
    { path: '/orders', label: 'Orders', show: can('MERCHANTPRO_FETCH_ORDERS') },
    {
      label: 'Fiscal Bills',
      show: can('FISCAL_VIEW_BILLS') || can('FISCAL_CREATE_BILL'),
      children: [
        { path: '/fiscal-bills', label: 'Fiscal Bills', show: can('FISCAL_VIEW_BILLS') },
        { path: '/fiscal-bills/create', label: 'Create Fiscal Bill', show: can('FISCAL_CREATE_BILL') },
        { path: '/fiscal-bills/get-status', label: 'Get Status', show: can('FISCAL_VIEW_BILLS') },
        { path: '/taxes', label: 'Taxes', show: can('ORGS_MANAGE') },
      ],
    },
    {
      label: 'Administration',
      show: hasAnyAction(user, ['USERS_MANAGE', 'ROLES_MANAGE', 'ORGS_MANAGE']) || user?.roleName === 'SUPERADMIN',
      children: [
        { path: '/account', label: 'Account', show: true },
        { path: '/users', label: 'User', show: can('USERS_MANAGE') },
        { path: '/roles', label: 'Roles & Permissions', show: hasAnyAction(user, ['ROLES_MANAGE', 'USERS_MANAGE']) },
        { path: '/organizations', label: 'Organization', show: can('ORGS_MANAGE') },
        { path: '/clients', label: 'Client', show: user?.roleName === 'SUPERADMIN' },
      ],
    },
    {
      label: 'Configuration',
      show: can('ORGS_MANAGE'),
      children: [
        { path: '/api-config', label: 'API Configuration', show: can('ORGS_MANAGE') },
        { path: '/fiscal-bills/paytype-map', label: 'Payment Type Mapping', show: can('ORGS_MANAGE') },
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
