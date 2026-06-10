import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import i18next from 'i18next'
import { orgsApi, productsApi } from '../services/api'
import { useAuth } from './AuthContext'

const SyncContext = createContext(null)
const POLL_MS = 2500

export function SyncProvider({ children }) {
  const { user } = useAuth()
  const [syncOrgId, setSyncOrgId] = useState(null)
  const [syncOrgName, setSyncOrgName] = useState(null)
  const [syncing, setSyncing] = useState(false)
  const [syncProgress, setSyncProgress] = useState(null)
  const [syncType, setSyncType] = useState(null)
  const [syncResult, setSyncResult] = useState(null)
  const abortRef = useRef(null)
  const pollRef = useRef(null)
  const syncingRef = useRef(false)

  useEffect(() => {
    syncingRef.current = syncing
  }, [syncing])

  const stopPolling = useCallback(() => {
    if (pollRef.current) {
      clearInterval(pollRef.current)
      pollRef.current = null
    }
  }, [])

  const applyRunningStatus = useCallback((orgId, status, orgName) => {
    setSyncOrgId(orgId)
    if (orgName !== undefined) setSyncOrgName(orgName ?? null)
    setSyncing(true)
    setSyncProgress({ synced: status.synced ?? 0, total: status.total ?? 0 })
    setSyncType(status.syncType ?? null)
    setSyncResult(null)
  }, [])

  const handleTerminalStatus = useCallback((orgId, status) => {
    stopPolling()
    abortRef.current?.abort()
    abortRef.current = null
    setSyncing(false)
    setSyncProgress(null)
    setSyncType(null)
    setSyncOrgName(null)
    if (status.status === 'DONE') {
      setSyncResult({ ok: true, synced: status.synced, orgId })
    } else if (status.status === 'FAILED') {
      setSyncResult({
        ok: false,
        message: status.errorMessage || i18next.t('products.syncFailed'),
        orgId,
      })
    }
  }, [stopPolling])

  const pollOnce = useCallback(async (orgId) => {
    try {
      const status = await productsApi.syncStatus(orgId)
      if (status.running) {
        applyRunningStatus(orgId, status)
        return
      }
      if (syncingRef.current) {
        handleTerminalStatus(orgId, status)
      }
    } catch {
      /* ignore transient poll errors */
    }
  }, [applyRunningStatus, handleTerminalStatus])

  const startPolling = useCallback((orgId) => {
    stopPolling()
    pollRef.current = setInterval(() => {
      pollOnce(orgId)
    }, POLL_MS)
  }, [pollOnce, stopPolling])

  useEffect(() => () => stopPolling(), [stopPolling])

  const checkSyncStatus = useCallback(async (orgId, orgName) => {
    if (!orgId) return
    try {
      const status = await productsApi.syncStatus(orgId)
      if (status.running) {
        applyRunningStatus(orgId, status, orgName)
        startPolling(orgId)
      }
    } catch {
      /* ignore */
    }
  }, [applyRunningStatus, startPolling])

  // On login / app reload: scan all accessible orgs for a running sync and
  // auto-recover the indicator without requiring the user to open Products first.
  useEffect(() => {
    if (!user) return
    const fetchOrgs = user.roleName === 'SUPERADMIN' ? orgsApi.list() : orgsApi.myAccess()
    fetchOrgs
      .then(async (orgs) => {
        for (const org of orgs) {
          try {
            const status = await productsApi.syncStatus(org.orgId)
            if (status.running) {
              applyRunningStatus(org.orgId, status, org.name)
              startPolling(org.orgId)
              break
            }
          } catch {
            /* skip org on error */
          }
        }
      })
      .catch(() => {})
  }, [user, applyRunningStatus, startPolling])

  const startSync = useCallback((orgId, failedMessage, alreadyRunningMessage, mode = 'AUTO', orgName) => {
    if (syncing && syncOrgId === orgId) return
    setSyncOrgId(orgId)
    setSyncOrgName(orgName ?? null)
    setSyncing(true)
    setSyncProgress({ synced: 0, total: 0 })
    setSyncType(null)
    setSyncResult(null)
    startPolling(orgId)

    abortRef.current?.abort()

    const stream = productsApi.syncStream(orgId, {
      mode,
      failedMessage,
      onProgress: (data) => {
        setSyncProgress({ synced: data.synced, total: data.total })
        if (data.syncType) setSyncType(data.syncType)
      },
      onDone: (data) => {
        setSyncProgress({ synced: data.synced, total: data.total })
        if (data.syncType) setSyncType(data.syncType)
        setSyncResult({ ok: true, synced: data.synced, orgId })
        setSyncing(false)
        setSyncType(null)
        setSyncOrgName(null)
        abortRef.current = null
        stopPolling()
      },
      onConflict: (status) => {
        if (status?.running) {
          applyRunningStatus(orgId, status, orgName)
          startPolling(orgId)
          return
        }
        setSyncing(false)
        setSyncProgress(null)
        setSyncType(null)
        setSyncOrgName(null)
        stopPolling()
        setSyncResult({
          ok: false,
          message: alreadyRunningMessage || failedMessage,
          orgId,
        })
      },
      onError: (err) => {
        if (err?.name === 'AbortError') return
        setSyncResult({ ok: false, message: err?.message || failedMessage, orgId })
        setSyncing(false)
        setSyncProgress(null)
        setSyncType(null)
        setSyncOrgName(null)
        abortRef.current = null
        stopPolling()
      },
    })
    abortRef.current = stream
  }, [syncing, syncOrgId, applyRunningStatus, startPolling, stopPolling])

  const cancelSync = useCallback(() => {
    const orgId = syncOrgId
    if (orgId) {
      productsApi.cancelSync(orgId).catch(() => {})
    }
    abortRef.current?.abort()
    abortRef.current = null
    stopPolling()
    setSyncing(false)
    setSyncProgress(null)
    setSyncType(null)
    setSyncResult(null)
    setSyncOrgId(null)
    setSyncOrgName(null)
  }, [stopPolling, syncOrgId])

  const consumeResult = useCallback(() => {
    setSyncResult(null)
    setSyncOrgId(null)
    setSyncOrgName(null)
    setSyncProgress(null)
    setSyncType(null)
  }, [])

  return (
    <SyncContext.Provider
      value={{
        syncing,
        syncOrgId,
        syncOrgName,
        syncProgress,
        syncType,
        syncResult,
        startSync,
        cancelSync,
        consumeResult,
        checkSyncStatus,
      }}
    >
      {children}
    </SyncContext.Provider>
  )
}

export function useSyncContext() {
  const ctx = useContext(SyncContext)
  if (!ctx) throw new Error('useSyncContext must be used inside SyncProvider')
  return ctx
}
