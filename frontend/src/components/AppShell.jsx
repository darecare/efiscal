import React, { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { hasAction, hasAnyAction } from '../utils/permissions'
import i18n from '../i18n'

const getNavItems = (user) => {
  const can = (action) => hasAction(user, action)

  return [
    { path: '/orders', labelKey: 'nav.orders', show: can('MERCHANTPRO_FETCH_ORDERS') },
    {
      labelKey: 'nav.fiscalBills',
      show: can('FISCAL_VIEW_BILLS') || can('FISCAL_CREATE_BILL'),
      children: [
        { path: '/fiscal-bills', labelKey: 'nav.fiscalBills', show: can('FISCAL_VIEW_BILLS') },
        { path: '/fiscal-bills/create', labelKey: 'nav.createFiscalBill', show: can('FISCAL_CREATE_BILL') },
        { path: '/fiscal-bills/get-status', labelKey: 'nav.getStatus', show: can('FISCAL_VIEW_BILLS') },
        { path: '/taxes', labelKey: 'nav.taxes', show: can('ORGS_MANAGE') },
      ],
    },
    {
      labelKey: 'nav.administration',
      show: hasAnyAction(user, ['USERS_MANAGE', 'ROLES_MANAGE', 'ORGS_MANAGE']) || user?.roleName === 'SUPERADMIN',
      children: [
        { path: '/account', labelKey: 'nav.account', show: true },
        { path: '/users', labelKey: 'nav.user', show: can('USERS_MANAGE') },
        { path: '/roles', labelKey: 'nav.rolesPermissions', show: hasAnyAction(user, ['ROLES_MANAGE', 'USERS_MANAGE']) },
        { path: '/organizations', labelKey: 'nav.organization', show: can('ORGS_MANAGE') },
        { path: '/clients', labelKey: 'nav.client', show: user?.roleName === 'SUPERADMIN' },
      ],
    },
    {
      labelKey: 'nav.configuration',
      show: can('ORGS_MANAGE'),
      children: [
        { path: '/api-config', labelKey: 'nav.apiConfiguration', show: can('ORGS_MANAGE') },
        { path: '/fiscal-bills/paytype-map', labelKey: 'nav.paymentTypeMapping', show: can('ORGS_MANAGE') },
      ],
    },
  ]
}

export default function AppShell({ title, subtitle, actions, children }) {
  const { t } = useTranslation()
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
        initial[item.labelKey] = item.children.some((c) => location.pathname.startsWith(c.path))
      }
    })
    return initial
  })

  function toggleGroup(labelKey) {
    setOpenGroups((prev) => ({ ...prev, [labelKey]: !prev[labelKey] }))
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const supportedLngs = (i18n.options.supportedLngs || ['en']).filter((lng) => lng !== 'cimode')

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <h1>{t('nav.brand')}</h1>
          <p>{user?.email}</p>
        </div>
        <div className="topbar-actions">
          <label className="language-switcher" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '0.85rem', opacity: 0.8 }}>{t('common.language')}</span>
            <select
              value={i18n.language?.split('-')[0] || 'en'}
              onChange={(e) => i18n.changeLanguage(e.target.value)}
              className="secondary-button"
              style={{ padding: '4px 8px', fontSize: '0.85rem' }}
              aria-label={t('common.language')}
            >
              {supportedLngs.map((lng) => (
                <option key={lng} value={lng}>
                  {t(`common.languages.${lng}`, { defaultValue: lng })}
                </option>
              ))}
            </select>
          </label>
          <span className="badge">{user?.roleName}</span>
          <button className="secondary-button" onClick={handleLogout}>{t('nav.logout')}</button>
        </div>
      </header>
      <div className="shell-body">
        <aside className="sidebar">
          <nav>
            {filteredNavItems.map((item) => {
              const groupLabel = t(item.labelKey)
              if (item.children) {
                const isOpen = openGroups[item.labelKey]
                const isGroupActive = item.children.some((c) => location.pathname.startsWith(c.path))
                return (
                  <div key={item.labelKey}>
                    <button
                      className={`nav-link nav-group-toggle${isGroupActive ? ' active' : ''}`}
                      onClick={() => toggleGroup(item.labelKey)}
                      aria-expanded={isOpen}
                    >
                      <span>{groupLabel}</span>
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
                            {t(child.labelKey)}
                          </Link>
                        ))}
                      </div>
                    )}
                  </div>
                )
              }
              return (
                <Link key={item.path} to={item.path} className={location.pathname === item.path ? 'nav-link active' : 'nav-link'}>
                  {groupLabel}
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
