import React, { createContext, useCallback, useContext, useRef, useState } from 'react'
import { productsApi } from '../services/api'

const SyncContext = createContext(null)

export function SyncProvider({ children }) {
  const [syncOrgId, setSyncOrgId] = useState(null)
  const [syncing, setSyncing] = useState(false)
  const [syncProgress, setSyncProgress] = useState(null)
  const [syncResult, setSyncResult] = useState(null)
  const abortRef = useRef(null)

  const startSync = useCallback((orgId, failedMessage) => {
    if (syncing) return
    setSyncOrgId(orgId)
    setSyncing(true)
    setSyncProgress({ synced: 0, total: 0 })
    setSyncResult(null)

    const stream = productsApi.syncStream(orgId, {
      failedMessage,
      onProgress: (data) => {
        setSyncProgress({ synced: data.synced, total: data.total })
      },
      onDone: (data) => {
        setSyncProgress({ synced: data.synced, total: data.total })
        setSyncResult({ ok: true, synced: data.synced, orgId })
        setSyncing(false)
        abortRef.current = null
      },
      onError: (err) => {
        if (err?.name === 'AbortError') return
        setSyncResult({ ok: false, message: err?.message || failedMessage, orgId })
        setSyncing(false)
        setSyncProgress(null)
        abortRef.current = null
      },
    })
    abortRef.current = stream
  }, [syncing])

  const cancelSync = useCallback(() => {
    abortRef.current?.abort()
    abortRef.current = null
    setSyncing(false)
    setSyncProgress(null)
    setSyncResult(null)
    setSyncOrgId(null)
  }, [])

  const consumeResult = useCallback(() => {
    setSyncResult(null)
    setSyncOrgId(null)
    setSyncProgress(null)
  }, [])

  return (
    <SyncContext.Provider value={{ syncing, syncOrgId, syncProgress, syncResult, startSync, cancelSync, consumeResult }}>
      {children}
    </SyncContext.Provider>
  )
}

export function useSyncContext() {
  const ctx = useContext(SyncContext)
  if (!ctx) throw new Error('useSyncContext must be used inside SyncProvider')
  return ctx
}
