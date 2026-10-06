import React, { useEffect, useRef } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { getLandingPath, hasAction, hasAnyAction, isSuperAdmin } from '../utils/permissions'

export default function ActionProtectedRoute({
  children,
  action,
  actions,
  requireSuperAdmin = false,
  fallback,
}) {
  const { t } = useTranslation()
  const { user, loading, logout, showNotification } = useAuth()
  const location = useLocation()
  const lastDeniedSignatureRef = useRef(null)

  const allowed = !loading && user && (
    requireSuperAdmin
      ? isSuperAdmin(user)
      : (action ? hasAction(user, action) : hasAnyAction(user, actions ?? []))
  )
  const redirectTo = fallback ?? getLandingPath(user)

  useEffect(() => {
    if (!loading && user && !allowed) {
      const deniedSignature = JSON.stringify({
        userId: user.userId ?? user.email ?? 'unknown',
        action: action ?? null,
        actions: actions ?? [],
        requireSuperAdmin,
        redirectTo,
      })
      if (lastDeniedSignatureRef.current !== deniedSignature) {
        lastDeniedSignatureRef.current = deniedSignature
        showNotification(t('common.permissionDenied'), 'error')
      }
    } else if (allowed) {
      lastDeniedSignatureRef.current = null
    }
  }, [loading, user, allowed, showNotification, t, action, actions, requireSuperAdmin, redirectTo])

  if (loading) {
    return <div className="center-state">{t('common.loading')}</div>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!allowed) {
    if (!redirectTo || redirectTo === location.pathname) {
      return (
        <div className="center-state">
          <p>{t('common.noAccessiblePages')}</p>
          <button type="button" className="secondary-button" onClick={logout}>
            {t('nav.logout')}
          </button>
        </div>
      )
    }
    return <Navigate to={redirectTo} replace />
  }

  return children
}
