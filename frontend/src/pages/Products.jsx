import React, { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { orgsApi, productsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { useSyncContext } from '../contexts/SyncContext'

const PAGE_SIZE = 100
const SYNC_LIVE_REFRESH_MS = 4000

function emptyForm() {
  return { name: '', sku: '', ean: '', lastKnownPrice: '', isActive: true }
}

function mapSyncErrorMessage(raw, t) {
  if (!raw) return t('products.pullError')
  if (raw.includes('INCREMENTAL_FILTER_UNSUPPORTED')) {
    return t('products.incrementalFilterUnsupported')
  }
  return raw
}

function formatDateTime(value, dash) {
  if (!value) return dash
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export default function Products() {
  const { t } = useTranslation()
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'
  const {
    syncing,
    syncOrgId,
    syncProgress,
    syncType,
    syncResult,
    startSync,
    cancelSync,
    consumeResult,
    checkSyncStatus,
  } = useSyncContext()

  const [orgs, setOrgs] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState('')
  const [products, setProducts] = useState([])
  const [page, setPage] = useState(0)
  const [totalCount, setTotalCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState('add')
  const [editId, setEditId] = useState(null)
  const [form, setForm] = useState(emptyForm())
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [lastSync, setLastSync] = useState(null)

  const [searchQuery, setSearchQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [selectedIds, setSelectedIds] = useState(() => new Set())
  const [isAllPagesSelected, setIsAllPagesSelected] = useState(false)
  const [bulkActionInFlight, setBulkActionInFlight] = useState(false)

  const headerCheckboxRef = useRef(null)

  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE))
  const isSyncingThisOrg = syncing && String(syncOrgId) === String(selectedOrgId)
  const visibleProductIds = products.map((p) => p.productId)
  const allVisibleSelected = visibleProductIds.length > 0
    && visibleProductIds.every((id) => selectedIds.has(id) || isAllPagesSelected)
  const someVisibleSelected = visibleProductIds.some((id) => selectedIds.has(id) || isAllPagesSelected)
  const selectedCount = isAllPagesSelected ? totalCount : selectedIds.size
  const hasSelection = selectedCount > 0
  const bulkActionsDisabled = isSyncingThisOrg || bulkActionInFlight

  useEffect(() => {
    const loadOrgs = isSuperAdmin ? orgsApi.list() : orgsApi.myAccess()
    loadOrgs.then(setOrgs).catch(() => setOrgs([]))
  }, [isSuperAdmin])

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(searchQuery.trim()), 300)
    return () => clearTimeout(timer)
  }, [searchQuery])

  useEffect(() => {
    setPage(0)
    setSelectedIds(new Set())
    setIsAllPagesSelected(false)
  }, [debouncedQuery, selectedOrgId])

  useEffect(() => {
    if (!selectedOrgId) {
      setProducts([])
      setTotalCount(0)
      return undefined
    }
    loadProducts()
    return undefined
  }, [selectedOrgId, page, debouncedQuery])

  useEffect(() => {
    const el = headerCheckboxRef.current
    if (!el) return
    el.indeterminate = someVisibleSelected && !allVisibleSelected
  }, [someVisibleSelected, allVisibleSelected])

  useEffect(() => {
    if (!selectedOrgId) {
      setLastSync(null)
      return undefined
    }
    const selectedOrgName = orgs.find((o) => String(o.orgId) === String(selectedOrgId))?.name
    checkSyncStatus(Number(selectedOrgId), selectedOrgName)
    productsApi.syncStatus(Number(selectedOrgId))
      .then((status) => {
        if (!status.running && status.status === 'DONE' && status.finishedAt) {
          setLastSync({ synced: status.synced, finishedAt: status.finishedAt })
        } else {
          setLastSync(null)
        }
      })
      .catch(() => setLastSync(null))
    return undefined
  }, [selectedOrgId, checkSyncStatus])

  useEffect(() => {
    if (!success) return undefined
    const timer = setTimeout(() => setSuccess(null), 3500)
    return () => clearTimeout(timer)
  }, [success])

  // Pick up a sync that completed while the user was on another page.
  useEffect(() => {
    if (!syncResult) return
    if (String(syncResult.orgId) !== String(selectedOrgId)) return
    if (syncResult.ok) {
      setPage(0)
      loadProducts(0)
      productsApi.syncStatus(Number(selectedOrgId))
        .then((status) => {
          if (status.finishedAt) {
            setLastSync({ synced: status.synced, finishedAt: status.finishedAt })
          }
        })
        .catch(() => {})
      setSuccess(
        syncResult.synced === 0
          ? t('products.pullSuccessNone')
          : t('products.pullSuccess', { count: syncResult.synced }),
      )
    } else {
      setError(mapSyncErrorMessage(syncResult.message, t))
    }
    consumeResult()
  }, [syncResult, selectedOrgId])

  useEffect(() => {
    if (!isSyncingThisOrg) return undefined
    const id = setInterval(() => silentRefreshProducts(), SYNC_LIVE_REFRESH_MS)
    return () => clearInterval(id)
  }, [isSyncingThisOrg, page, debouncedQuery, selectedOrgId])

  function clearSelection() {
    setSelectedIds(new Set())
    setIsAllPagesSelected(false)
  }

  function toggleProductSelection(productId) {
    if (isAllPagesSelected) {
      setIsAllPagesSelected(false)
      setSelectedIds(new Set(visibleProductIds.filter((id) => id !== productId)))
      return
    }
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(productId)) {
        next.delete(productId)
      } else {
        next.add(productId)
      }
      return next
    })
  }

  function toggleSelectAllVisible() {
    if (allVisibleSelected) {
      if (isAllPagesSelected) {
        clearSelection()
        return
      }
      setSelectedIds((prev) => {
        const next = new Set(prev)
        visibleProductIds.forEach((id) => next.delete(id))
        return next
      })
      return
    }
    setSelectedIds((prev) => {
      const next = new Set(prev)
      visibleProductIds.forEach((id) => next.add(id))
      return next
    })
  }

  function handleSelectAllPages() {
    if (!selectedOrgId) return
    setIsAllPagesSelected(true)
  }

  async function loadProducts(pageOverride) {
    const currentPage = pageOverride ?? page
    setLoading(true)
    setError(null)
    try {
      const listParams = { page: currentPage, size: PAGE_SIZE }
      if (debouncedQuery) listParams.q = debouncedQuery
      const data = await productsApi.list(Number(selectedOrgId), listParams)
      setProducts(data.items || [])
      setTotalCount(data.totalCount ?? 0)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || t('products.loadFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setLoading(false)
    }
  }

  async function silentRefreshProducts(pageOverride) {
    if (!selectedOrgId) return
    const currentPage = pageOverride ?? page
    try {
      const listParams = { page: currentPage, size: PAGE_SIZE }
      if (debouncedQuery) listParams.q = debouncedQuery
      const data = await productsApi.list(Number(selectedOrgId), listParams)
      setProducts(data.items || [])
      setTotalCount(data.totalCount ?? 0)
    } catch {
      // silent — don't surface transient errors during background refresh
    }
  }

  function openAdd() {
    setModalMode('add')
    setEditId(null)
    setForm(emptyForm())
    setFormError(null)
    setModalOpen(true)
  }

  function openEdit(product) {
    setModalMode('edit')
    setEditId(product.productId)
    setForm({
      name: product.name || '',
      sku: product.sku || '',
      ean: product.ean || '',
      lastKnownPrice: product.lastKnownPrice != null ? String(product.lastKnownPrice) : '',
      isActive: product.isActive,
    })
    setFormError(null)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setFormError(null)
  }

  async function handleSave(e) {
    e.preventDefault()
    if (!form.name.trim()) {
      setFormError(t('products.nameRequired'))
      return
    }
    if (!form.sku.trim() && !form.ean.trim()) {
      setFormError(t('products.skuOrEanRequired'))
      return
    }
    if (form.lastKnownPrice.trim() && Number.isNaN(parseFloat(form.lastKnownPrice))) {
      setFormError(t('products.invalidPrice'))
      return
    }
    if (!selectedOrgId) {
      setFormError(t('products.selectOrgRequired'))
      return
    }

    const payload = {
      name: form.name.trim(),
      sku: form.sku.trim() || null,
      ean: form.ean.trim() || null,
      lastKnownPrice: form.lastKnownPrice ? parseFloat(form.lastKnownPrice) : null,
      isActive: form.isActive,
    }

    setSaving(true)
    setFormError(null)
    try {
      if (modalMode === 'add') {
        await productsApi.create(Number(selectedOrgId), payload)
      } else {
        await productsApi.update(editId, payload)
      }
      await loadProducts()
      closeModal()
      setSuccess(modalMode === 'add' ? t('products.created') : t('products.updated'))
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || t('products.saveFailed')
      setFormError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(product) {
    if (!window.confirm(t('products.deleteConfirm', { name: product.name }))) return
    try {
      await productsApi.remove(product.productId)
      if (products.length === 1 && page > 0) {
        setPage((p) => p - 1)
      } else {
        await loadProducts()
      }
      setSuccess(t('products.deleted'))
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || t('products.deleteFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    }
  }

  async function handleBulkDelete() {
    const count = selectedCount
    if (count === 0) return
    if (!window.confirm(t('products.deleteSelectedConfirm', { count }))) return
    setBulkActionInFlight(true)
    setError(null)
    try {
      const bulkOptions = { selectAll: isAllPagesSelected, q: debouncedQuery || undefined }
      const result = isAllPagesSelected
        ? await productsApi.removeMany(Number(selectedOrgId), [], bulkOptions)
        : await productsApi.removeMany(Number(selectedOrgId), [...selectedIds], bulkOptions)
      const deleted = result.deleted ?? count
      clearSelection()
      const remaining = totalCount - deleted
      if (remaining <= page * PAGE_SIZE && page > 0) {
        setPage((p) => Math.max(0, p - 1))
      } else {
        await loadProducts()
      }
      setSuccess(t('products.deletedMany', { count: deleted }))
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || t('products.bulkActionFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setBulkActionInFlight(false)
    }
  }

  async function handleBulkStatus(isActive) {
    const count = selectedCount
    if (count === 0) return
    const confirmKey = isActive ? 'products.activateSelectedConfirm' : 'products.deactivateSelectedConfirm'
    if (!window.confirm(t(confirmKey, { count }))) return
    setBulkActionInFlight(true)
    setError(null)
    try {
      const bulkOptions = { selectAll: isAllPagesSelected, q: debouncedQuery || undefined }
      const result = isAllPagesSelected
        ? await productsApi.updateStatusMany(Number(selectedOrgId), [], isActive, bulkOptions)
        : await productsApi.updateStatusMany(Number(selectedOrgId), [...selectedIds], isActive, bulkOptions)
      const updated = result.updated ?? count
      clearSelection()
      await loadProducts()
      const successKey = isActive ? 'products.activatedMany' : 'products.deactivatedMany'
      setSuccess(t(successKey, { count: updated }))
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || t('products.bulkActionFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setBulkActionInFlight(false)
    }
  }

  function handleCancelSync() {
    cancelSync()
  }

  function handlePullFromShop() {
    if (!selectedOrgId || isSyncingThisOrg) return
    setError(null)
    const orgName = orgs.find((o) => String(o.orgId) === String(selectedOrgId))?.name
    startSync(
      Number(selectedOrgId),
      t('products.pullError'),
      t('products.syncAlreadyRunning'),
      'AUTO',
      orgName,
    )
  }

  function handleFullRefresh() {
    if (!selectedOrgId || isSyncingThisOrg) return
    if (!window.confirm(t('products.fullRefreshConfirm'))) return
    setError(null)
    const orgName = orgs.find((o) => String(o.orgId) === String(selectedOrgId))?.name
    startSync(
      Number(selectedOrgId),
      t('products.pullError'),
      t('products.syncAlreadyRunning'),
      'RESET_FULL',
      orgName,
    )
  }

  function syncTypeLabel(type) {
    if (type === 'INCREMENTAL') return t('products.syncTypeIncremental')
    if (type === 'RESET_FULL') return t('products.syncTypeResetFull')
    return t('products.syncTypeFull')
  }

  function formatPrice(value) {
    if (value == null || value === '') return t('common.dash')
    const n = Number(value)
    return Number.isNaN(n) ? value : n.toFixed(2)
  }

  function formatCount(value) {
    return Number(value).toLocaleString()
  }

  return (
    <AppShell
      title={t('products.title')}
      subtitle={t('products.subtitle')}
      actions={
        selectedOrgId && (
          <>
            {isSyncingThisOrg && (
              <button
                type="button"
                className="secondary-button"
                onClick={handleCancelSync}
              >
                {t('common.cancel')}
              </button>
            )}
            <button
              type="button"
              className="secondary-button"
              onClick={handlePullFromShop}
              disabled={isSyncingThisOrg}
            >
              {isSyncingThisOrg ? t('products.pulling') : t('products.pullFromShop')}
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={handleFullRefresh}
              disabled={isSyncingThisOrg}
            >
              {t('products.fullRefresh')}
            </button>
            <button type="button" className="primary-button" onClick={openAdd} disabled={isSyncingThisOrg}>
              {t('products.addProduct')}
            </button>
          </>
        )
      }
    >
      <div className="card" style={{ marginBottom: '1rem' }}>
        <div className="form-group" style={{ maxWidth: 320, marginBottom: 0 }}>
          <label className="form-label">{t('common.organization')}</label>
          <select
            className="form-input"
            value={selectedOrgId}
            onChange={(e) => {
              setSelectedOrgId(e.target.value)
              setPage(0)
              setSearchQuery('')
              setDebouncedQuery('')
              clearSelection()
            }}
          >
            <option value="">{t('products.selectOrg')}</option>
            {orgs.map((org) => (
              <option key={org.orgId} value={org.orgId}>{org.name}</option>
            ))}
          </select>
        </div>
        {lastSync && !isSyncingThisOrg && (
          <p className="muted" style={{ marginTop: '0.75rem', marginBottom: 0 }}>
            {t('products.lastSyncAt', {
              count: lastSync.synced,
              time: formatDateTime(lastSync.finishedAt, t('common.dash')),
            })}
          </p>
        )}
      </div>

      {success && <div className="success-banner">{success}</div>}

      {error && <div className="error-banner">{error}</div>}

      {isSyncingThisOrg && syncProgress && (
        <div className="card sync-progress-card" style={{ marginBottom: '1rem' }}>
          {syncType && (
            <p className="muted" style={{ marginBottom: '0.25rem' }}>
              {syncTypeLabel(syncType)}
            </p>
          )}
          <p className="muted" style={{ marginBottom: '0.5rem' }}>
            {syncProgress.total > 0
              ? t('products.syncingProgress', { synced: syncProgress.synced, total: syncProgress.total })
              : t('products.syncStarting')}
          </p>
          <progress
            className="sync-progress"
            max={syncProgress.total > 0 ? syncProgress.total : undefined}
            value={syncProgress.synced}
          />
        </div>
      )}

      <section className={`table-card products-table-card${hasSelection ? ' products-table-card--has-selection' : ''}`}>
        {!selectedOrgId ? (
          <p className="muted">{t('products.selectOrgHint')}</p>
        ) : (
          <>
            <div className="products-table-toolbar">
              <div className="products-search-field">
                <span className="products-search-field__icon" aria-hidden="true">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                    <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2" />
                    <path d="M20 20L16.5 16.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  </svg>
                </span>
                <input
                  type="search"
                  className="products-search-field__input"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder={t('products.search')}
                  aria-label={t('products.search')}
                />
                {searchQuery && (
                  <button
                    type="button"
                    className="products-search-field__clear"
                    onClick={() => setSearchQuery('')}
                    aria-label={t('products.clearSearch')}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M6 6L18 18M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                    </svg>
                  </button>
                )}
              </div>
              {totalCount > 0 && (
                <span className="badge products-catalog-badge">
                  {t('products.catalogCount', { count: formatCount(totalCount) })}
                </span>
              )}
            </div>

            {loading ? (
              <p className="muted products-loading">{t('common.loadingDots')}</p>
            ) : (
              <>
            {allVisibleSelected && !isAllPagesSelected && totalCount > visibleProductIds.length && (
              <div className="products-info-banner">
                <span className="products-info-banner__icon" aria-hidden="true">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
                    <path d="M12 8V12M12 16H12.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                  </svg>
                </span>
                <p className="products-info-banner__text">
                  {t('products.allOnPageSelected', { count: visibleProductIds.length })}
                </p>
                <button
                  type="button"
                  className="products-info-banner__action"
                  onClick={handleSelectAllPages}
                  disabled={bulkActionsDisabled}
                >
                  {t('products.selectAllPages', { total: formatCount(totalCount) })}
                </button>
              </div>
            )}
            {isAllPagesSelected && (
              <div className="products-info-banner products-info-banner--active">
                <span className="products-info-banner__icon" aria-hidden="true">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                    <path d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </span>
                <p className="products-info-banner__text">
                  {t('products.allPagesSelected', { total: formatCount(selectedCount) })}
                </p>
                <button type="button" className="products-info-banner__action" onClick={clearSelection}>
                  {t('products.clearSelection')}
                </button>
              </div>
            )}
            <div className="products-table-wrap">
            <table className="products-table">
              <thead>
                <tr>
                  <th className="col-checkbox">
                    <label className="products-checkbox">
                      <input
                        ref={headerCheckboxRef}
                        type="checkbox"
                        className="products-checkbox__input"
                        checked={allVisibleSelected}
                        onChange={toggleSelectAllVisible}
                        disabled={products.length === 0 || bulkActionsDisabled}
                        aria-label={t('products.selectAllAria')}
                      />
                      <span className="products-checkbox__box" aria-hidden="true" />
                    </label>
                  </th>
                  <th>{t('products.columns.id')}</th>
                  <th>{t('products.columns.name')}</th>
                  <th>{t('products.columns.sku')}</th>
                  <th>{t('products.columns.ean')}</th>
                  <th>{t('products.columns.price')}</th>
                  <th>{t('products.columns.active')}</th>
                  <th>{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {products.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="products-empty-cell">{t('products.empty')}</td>
                  </tr>
                ) : (
                  products.map((p) => {
                    const isSelected = isAllPagesSelected || selectedIds.has(p.productId)
                    return (
                    <tr
                      key={p.productId}
                      className={isSelected ? 'products-row--selected' : undefined}
                    >
                      <td className="col-checkbox">
                        <label className="products-checkbox">
                          <input
                            type="checkbox"
                            className="products-checkbox__input"
                            checked={isSelected}
                            onChange={() => toggleProductSelection(p.productId)}
                            disabled={bulkActionsDisabled}
                            aria-label={t('products.selectProductAria', { name: p.name })}
                          />
                          <span className="products-checkbox__box" aria-hidden="true" />
                        </label>
                      </td>
                      <td className="products-cell-id">{p.productId}</td>
                      <td className="products-cell-name">{p.name}</td>
                      <td>{p.sku || t('common.dash')}</td>
                      <td>{p.ean || t('common.dash')}</td>
                      <td>{formatPrice(p.lastKnownPrice)}</td>
                      <td>
                        <span className={`status-chip ${p.isActive ? 'active' : 'inactive'}`}>
                          {p.isActive ? t('common.active') : t('common.inactive')}
                        </span>
                      </td>
                      <td>
                        <div className="table-row-actions">
                          {p.sourceType !== 'MERCHANTPRO' && (
                            <button type="button" className="secondary-button" onClick={() => openEdit(p)}>
                              {t('common.edit')}
                            </button>
                          )}
                          <button type="button" className="secondary-button danger" onClick={() => handleDelete(p)}>
                            {t('common.delete')}
                          </button>
                        </div>
                      </td>
                    </tr>
                    )
                  })
                )}
              </tbody>
            </table>
            </div>
            {totalCount > 0 && (
              <div className="pagination">
                <button
                  type="button"
                  className="secondary-button"
                  disabled={page <= 0 || loading}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  {t('common.prev')}
                </button>
                <span className="pagination-info">
                  {t('common.paginationInfo', {
                    current: page + 1,
                    total: totalPages,
                    records: formatCount(totalCount),
                  })}
                </span>
                <button
                  type="button"
                  className="secondary-button"
                  disabled={page >= totalPages - 1 || loading}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t('common.next')}
                </button>
              </div>
            )}
              </>
            )}
          </>
        )}
      </section>

      {hasSelection && (
        <div className="products-bulk-bar" role="region" aria-label={t('common.actions')}>
          <div className="products-bulk-bar__summary">
            <span className="products-bulk-bar__badge">{formatCount(selectedCount)}</span>
            <span className="products-bulk-bar__label">{t('products.selectedLabel')}</span>
          </div>
          <div className="products-bulk-bar__actions">
            <button
              type="button"
              className="secondary-button"
              onClick={() => handleBulkStatus(true)}
              disabled={bulkActionsDisabled}
            >
              {t('products.bulkActivate')}
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => handleBulkStatus(false)}
              disabled={bulkActionsDisabled}
            >
              {t('products.bulkDeactivate')}
            </button>
            <button
              type="button"
              className="secondary-button danger"
              onClick={handleBulkDelete}
              disabled={bulkActionsDisabled}
            >
              {t('products.bulkDelete')}
            </button>
          </div>
          <button
            type="button"
            className="products-bulk-bar__close"
            onClick={clearSelection}
            disabled={bulkActionsDisabled}
            aria-label={t('products.clearSelection')}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M6 6L18 18M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{modalMode === 'add' ? t('products.modal.addTitle') : t('products.modal.editTitle')}</h3>
              <button type="button" className="modal-close" onClick={closeModal} aria-label={t('common.close')}>×</button>
            </div>
            <form onSubmit={handleSave}>
              <div className="form-grid">
                <div className="field">
                  <label>{t('products.columns.name')} *</label>
                  <input
                    value={form.name}
                    onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                    required
                  />
                </div>
                <div className="field">
                  <label>{t('products.columns.sku')}</label>
                  <input
                    value={form.sku}
                    onChange={(e) => setForm((f) => ({ ...f, sku: e.target.value }))}
                  />
                </div>
                <div className="field">
                  <label>{t('products.columns.ean')}</label>
                  <input
                    value={form.ean}
                    onChange={(e) => setForm((f) => ({ ...f, ean: e.target.value }))}
                  />
                </div>
                <div className="field">
                  <label>{t('products.columns.price')}</label>
                  <input
                    type="number"
                    step="0.01"
                    value={form.lastKnownPrice}
                    onChange={(e) => setForm((f) => ({ ...f, lastKnownPrice: e.target.value }))}
                  />
                </div>
                {modalMode === 'edit' && (
                  <div className="field">
                    <label>
                      <input
                        type="checkbox"
                        checked={form.isActive}
                        onChange={(e) => setForm((f) => ({ ...f, isActive: e.target.checked }))}
                      />
                      {' '}{t('products.columns.active')}
                    </label>
                  </div>
                )}
              </div>
              <p className="muted" style={{ marginTop: '0.5rem' }}>{t('products.skuOrEanHint')}</p>
              {formError && <p className="error-text">{formError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={closeModal}>{t('common.cancel')}</button>
                <button type="submit" className="primary-button" disabled={saving}>
                  {saving ? t('common.saving') : t('common.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
