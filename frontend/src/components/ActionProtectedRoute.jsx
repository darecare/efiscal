import React, { useEffect, useRef } from 'react'
import { Navigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../contexts/AuthContext'
import { hasAction, hasAnyAction, isSuperAdmin } from '../utils/permissions'

export default function ActionProtectedRoute({
  children,
  action,
  actions,
  requireSuperAdmin = false,
  fallback = '/account',
}) {
  const { t } = useTranslation()
  const { user, loading, showNotification } = useAuth()
  const lastDeniedSignatureRef = useRef(null)

  const allowed = !loading && user && (
    requireSuperAdmin
      ? isSuperAdmin(user)
      : (action ? hasAction(user, action) : hasAnyAction(user, actions ?? []))
  )

  useEffect(() => {
    if (!loading && user && !allowed) {
      const deniedSignature = JSON.stringify({
        userId: user.userId ?? user.email ?? 'unknown',
        action: action ?? null,
        actions: actions ?? [],
        requireSuperAdmin,
        fallback,
      })
      if (lastDeniedSignatureRef.current !== deniedSignature) {
        lastDeniedSignatureRef.current = deniedSignature
        showNotification(t('common.permissionDenied'), 'error')
      }
    } else if (allowed) {
      lastDeniedSignatureRef.current = null
    }
  }, [loading, user, allowed, showNotification, t, action, actions, requireSuperAdmin, fallback])

  if (loading) {
    return <div className="center-state">{t('common.loading')}</div>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!allowed) {
    return <Navigate to={fallback} replace />
  }

  return children
}
