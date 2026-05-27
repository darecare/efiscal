import React, { useEffect } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { hasAction, hasAnyAction, isSuperAdmin } from '../utils/permissions'

export default function ActionProtectedRoute({
  children,
  action,
  actions,
  requireSuperAdmin = false,
  fallback = '/account',
}) {
  const { user, loading, showNotification } = useAuth()

  const allowed = !loading && user && (
    requireSuperAdmin
      ? isSuperAdmin(user)
      : (action ? hasAction(user, action) : hasAnyAction(user, actions ?? []))
  )

  useEffect(() => {
    if (!loading && user && !allowed) {
      showNotification('You do not have permission to access this page.', 'error')
    }
  }, [loading, user, allowed, showNotification])

  if (loading) {
    return <div className="center-state">Loading...</div>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!allowed) {
    return <Navigate to={fallback} replace />
  }

  return children
}
