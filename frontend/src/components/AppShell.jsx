import React, { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { hasAction, hasAnyAction } from '../utils/permissions'
import { appInfoApi } from '../services/api'
import i18n from '../i18n'

function LanguageSwitcher() {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const rootRef = useRef(null)

  const supportedLngs = (i18n.options.supportedLngs || ['en']).filter((lng) => lng !== 'cimode')
  const currentLng = i18n.language?.split('-')[0] || 'en'

  useEffect(() => {
    if (!open) return undefined
    const onPointerDown = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) setOpen(false)
    }
    const onKeyDown = (e) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  function selectLanguage(lng) {
    i18n.changeLanguage(lng)
    setOpen(false)
  }

  return (
    <div className="language-switcher" ref={rootRef}>
      <span className="language-switcher__label">{t('common.language')}</span>
      <div className="language-switcher__control">
        <button
          type="button"
          className="language-switcher__trigger"
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
          aria-haspopup="listbox"
          aria-label={t('common.language')}
        >
          <span className="language-switcher__value">{t(`common.languages.${currentLng}`, { defaultValue: currentLng })}</span>
          <span className="language-switcher__chevron" aria-hidden="true" />
        </button>
        {open && (
          <ul className="language-switcher__menu" role="listbox" aria-label={t('common.language')}>
            {supportedLngs.map((lng) => {
              const isActive = lng === currentLng
              return (
                <li key={lng} role="presentation">
                  <button
                    type="button"
                    role="option"
                    aria-selected={isActive}
                    className={`language-switcher__option${isActive ? ' is-active' : ''}`}
                    onClick={() => selectLanguage(lng)}
                  >
                    {t(`common.languages.${lng}`, { defaultValue: lng })}
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </div>
  )
}

function HelpMenu({ onAbout }) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const rootRef = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    const onPointerDown = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) setOpen(false)
    }
    const onKeyDown = (e) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  function openAbout() {
    setOpen(false)
    onAbout()
  }

  return (
    <div className="help-menu" ref={rootRef}>
      <button
        type="button"
        className="help-menu__trigger"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label={t('about.menuLabel')}
      >
        i
      </button>
      {open && (
        <div className="help-menu__menu" role="menu" aria-label={t('about.menuLabel')}>
          <button type="button" className="help-menu__item" role="menuitem" onClick={openAbout}>
            {t('about.title')}
          </button>
        </div>
      )}
    </div>
  )
}

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
  const [aboutOpen, setAboutOpen] = useState(false)
  const [aboutLoading, setAboutLoading] = useState(false)
  const [aboutError, setAboutError] = useState(null)
  const [aboutInfo, setAboutInfo] = useState(null)

  async function openAbout() {
    setAboutOpen(true)
    setAboutLoading(true)
    setAboutError(null)
    setAboutInfo(null)
    try {
      setAboutInfo(await appInfoApi.get())
    } catch {
      setAboutError(t('about.loadFailed'))
    } finally {
      setAboutLoading(false)
    }
  }

  function closeAbout() {
    setAboutOpen(false)
    setAboutError(null)
  }

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

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <h1>{t('nav.brand')}</h1>
          <p>{user?.email}</p>
        </div>
        <div className="topbar-actions">
          <LanguageSwitcher />
          <HelpMenu onAbout={openAbout} />
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

      {aboutOpen && (
        <div className="modal-overlay" onClick={closeAbout}>
          <div className="modal about-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{t('about.title')}</h3>
              <button className="modal-close" onClick={closeAbout} aria-label={t('common.close')}>×</button>
            </div>

            {aboutLoading ? (
              <p className="muted">{t('common.loadingDots')}</p>
            ) : aboutError ? (
              <p className="error-text">{aboutError}</p>
            ) : (
              <div className="about-grid">
                <div className="about-row">
                  <span>{t('about.manufacturer')}</span>
                  <strong>{aboutInfo?.manufacturer || t('common.dash')}</strong>
                </div>
                <div className="about-row">
                  <span>{t('about.serialNumber')}</span>
                  <strong>{aboutInfo?.serialNumber || t('common.dash')}</strong>
                </div>
                <div className="about-row">
                  <span>{t('about.softwareVersion')}</span>
                  <strong>{aboutInfo?.softwareVersion || t('common.dash')}</strong>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
