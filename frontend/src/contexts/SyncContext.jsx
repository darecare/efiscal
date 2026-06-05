import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import i18next from 'i18next'
import { productsApi } from '../services/api'

const SyncContext = createContext(null)
const POLL_MS = 2500

export function SyncProvider({ children }) {
  const [syncOrgId, setSyncOrgId] = useState(null)
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

  const applyRunningStatus = useCallback((orgId, status) => {
    setSyncOrgId(orgId)
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

  const checkSyncStatus = useCallback(async (orgId) => {
    if (!orgId) return
    try {
      const status = await productsApi.syncStatus(orgId)
      if (status.running) {
        applyRunningStatus(orgId, status)
        startPolling(orgId)
      }
    } catch {
      /* ignore */
    }
  }, [applyRunningStatus, startPolling])

  const startSync = useCallback((orgId, failedMessage, alreadyRunningMessage) => {
    if (syncing && syncOrgId === orgId) return
    setSyncOrgId(orgId)
    setSyncing(true)
    setSyncProgress({ synced: 0, total: 0 })
    setSyncType(null)
    setSyncResult(null)
    startPolling(orgId)

    const stream = productsApi.syncStream(orgId, {
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
        abortRef.current = null
        stopPolling()
      },
      onConflict: (status) => {
        if (status?.running) {
          applyRunningStatus(orgId, status)
          startPolling(orgId)
          return
        }
        setSyncing(false)
        setSyncProgress(null)
        setSyncType(null)
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
  }, [stopPolling, syncOrgId])

  const consumeResult = useCallback(() => {
    setSyncResult(null)
    setSyncOrgId(null)
    setSyncProgress(null)
    setSyncType(null)
  }, [])

  return (
    <SyncContext.Provider
      value={{
        syncing,
        syncOrgId,
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
