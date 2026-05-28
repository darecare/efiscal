import React, { useEffect } from 'react'
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

  const allowed = !loading && user && (
    requireSuperAdmin
      ? isSuperAdmin(user)
      : (action ? hasAction(user, action) : hasAnyAction(user, actions ?? []))
  )

  useEffect(() => {
    if (!loading && user && !allowed) {
      showNotification(t('common.permissionDenied'), 'error')
    }
  }, [loading, user, allowed, showNotification, t])

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
