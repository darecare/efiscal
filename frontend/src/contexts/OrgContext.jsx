import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { orgsApi } from '../services/api'
import { isSuperAdmin as checkSuperAdmin } from '../utils/permissions'
import { useAuth } from './AuthContext'

const OrgContext = createContext(null)

export function storageKey(user) {
  const id = user?.userId ?? user?.id ?? user?.email
  return id ? `activeOrg:${id}` : null
}

export function resolveInitialOrgId(list, user) {
  const key = storageKey(user)
  if (key) {
    const stored = localStorage.getItem(key)
    if (stored && list.some((o) => String(o.orgId) === stored)) {
      return stored
    }
    if (stored) {
      localStorage.removeItem(key)
    }
  }
  if (list.length === 1) {
    return String(list[0].orgId)
  }
  return ''
}

export function OrgProvider({ children }) {
  const { user } = useAuth()
  const [orgs, setOrgs] = useState([])
  const [activeOrgId, setActiveOrgIdState] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const isSuperAdmin = checkSuperAdmin(user)

  const refreshOrgs = useCallback(async () => {
    if (!user) {
      setOrgs([])
      setActiveOrgIdState('')
      setError(null)
      return []
    }
    setLoading(true)
    setError(null)
    try {
      const list = isSuperAdmin ? await orgsApi.list() : await orgsApi.myAccess()
      setOrgs(list)
      setActiveOrgIdState((prev) => {
        if (prev && list.some((o) => String(o.orgId) === prev)) {
          return prev
        }
        return resolveInitialOrgId(list, user)
      })
      return list
    } catch {
      setOrgs([])
      setError('loadFailed')
      return []
    } finally {
      setLoading(false)
    }
  }, [user, isSuperAdmin])

  useEffect(() => {
    if (!user) {
      setOrgs([])
      setActiveOrgIdState('')
      setError(null)
      return
    }
    refreshOrgs()
  }, [user, refreshOrgs])

  const setActiveOrgId = useCallback((orgId) => {
    const next = orgId ? String(orgId) : ''
    setActiveOrgIdState(next)
    const key = storageKey(user)
    if (key) {
      if (next) {
        localStorage.setItem(key, next)
      } else {
        localStorage.removeItem(key)
      }
    }
  }, [user])

  const activeOrg = useMemo(
    () => orgs.find((o) => String(o.orgId) === String(activeOrgId)) ?? null,
    [orgs, activeOrgId],
  )

  const value = useMemo(
    () => ({
      orgs,
      activeOrgId,
      activeOrg,
      loading,
      error,
      setActiveOrgId,
      refreshOrgs,
      isSuperAdmin,
    }),
    [orgs, activeOrgId, activeOrg, loading, error, setActiveOrgId, refreshOrgs, isSuperAdmin],
  )

  return <OrgContext.Provider value={value}>{children}</OrgContext.Provider>
}

export function useOrg() {
  const context = useContext(OrgContext)
  if (!context) {
    throw new Error('useOrg must be used within OrgProvider')
  }
  return context
}
