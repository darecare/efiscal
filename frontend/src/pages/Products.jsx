import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { orgsApi, productsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { useSyncContext } from '../contexts/SyncContext'

const PAGE_SIZE = 100

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

  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE))
  const isSyncingThisOrg = syncing && String(syncOrgId) === String(selectedOrgId)

  useEffect(() => {
    const loadOrgs = isSuperAdmin ? orgsApi.list() : orgsApi.myAccess()
    loadOrgs.then(setOrgs).catch(() => setOrgs([]))
  }, [isSuperAdmin])

  useEffect(() => {
    if (!selectedOrgId) {
      setProducts([])
      setTotalCount(0)
      return undefined
    }
    loadProducts()
    return undefined
  }, [selectedOrgId, page])

  useEffect(() => {
    if (!selectedOrgId) {
      setLastSync(null)
      return undefined
    }
    checkSyncStatus(Number(selectedOrgId))
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

  async function loadProducts(pageOverride) {
    const currentPage = pageOverride ?? page
    setLoading(true)
    setError(null)
    try {
      const data = await productsApi.list(Number(selectedOrgId), { page: currentPage, size: PAGE_SIZE })
      setProducts(data.items || [])
      setTotalCount(data.totalCount ?? 0)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || t('products.loadFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setLoading(false)
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

  function handleCancelSync() {
    cancelSync()
  }

  function handlePullFromShop() {
    if (!selectedOrgId || isSyncingThisOrg) return
    setError(null)
    startSync(
      Number(selectedOrgId),
      t('products.pullError'),
      t('products.syncAlreadyRunning'),
    )
  }

  function formatPrice(value) {
    if (value == null || value === '') return t('common.dash')
    const n = Number(value)
    return Number.isNaN(n) ? value : n.toFixed(2)
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

      {success && (
        <div className="card" style={{ marginBottom: '1rem', borderLeft: '4px solid green', background: '#f0fff4' }}>
          {success}
        </div>
      )}

      {error && (
        <div className="card" style={{ marginBottom: '1rem', borderLeft: '4px solid red', background: '#fff5f5' }}>
          {error}
        </div>
      )}

      {isSyncingThisOrg && syncProgress && (
        <div className="card sync-progress-card" style={{ marginBottom: '1rem' }}>
          {syncType && (
            <p className="muted" style={{ marginBottom: '0.25rem' }}>
              {syncType === 'INCREMENTAL'
                ? t('products.syncTypeIncremental')
                : t('products.syncTypeFull')}
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

      <section className="table-card">
        {!selectedOrgId ? (
          <p className="muted">{t('products.selectOrgHint')}</p>
        ) : loading ? (
          <p className="muted">{t('common.loadingDots')}</p>
        ) : (
          <>
            <table>
              <thead>
                <tr>
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
                    <td colSpan={7} className="muted">{t('products.empty')}</td>
                  </tr>
                ) : (
                  products.map((p) => (
                    <tr key={p.productId}>
                      <td>{p.productId}</td>
                      <td>{p.name}</td>
                      <td>{p.sku || t('common.dash')}</td>
                      <td>{p.ean || t('common.dash')}</td>
                      <td>{formatPrice(p.lastKnownPrice)}</td>
                      <td>
                        <span className={`status-chip ${p.isActive ? 'active' : 'inactive'}`}>
                          {p.isActive ? t('common.active') : t('common.inactive')}
                        </span>
                      </td>
                      <td>
                        <button type="button" className="secondary-button" onClick={() => openEdit(p)}>
                          {t('common.edit')}
                        </button>
                        {' '}
                        <button type="button" className="secondary-button" onClick={() => handleDelete(p)}>
                          {t('common.delete')}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
            {totalCount > 0 && (
              <div className="pagination-bar" style={{ marginTop: '1rem', display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
                <span className="muted">
                  {t('common.paginationInfo', {
                    current: page + 1,
                    total: totalPages,
                    records: totalCount,
                  })}
                </span>
                <button
                  type="button"
                  className="secondary-button"
                  disabled={page <= 0 || loading}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  {t('common.prev')}
                </button>
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
      </section>

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
