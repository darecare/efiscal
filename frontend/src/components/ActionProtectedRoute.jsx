import React from 'react'
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
  const { user, loading } = useAuth()

  if (loading) {
    return <div className="center-state">Loading...</div>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (requireSuperAdmin && !isSuperAdmin(user)) {
    return <Navigate to={fallback} replace />
  }

  const allowed = action
    ? hasAction(user, action)
    : hasAnyAction(user, actions ?? [])

  if (!allowed) {
    return <Navigate to={fallback} replace />
  }

  return children
}
