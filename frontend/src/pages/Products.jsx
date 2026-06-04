import React, { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { orgsApi, productsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

function emptyForm() {
  return { name: '', sku: '', ean: '', lastKnownPrice: '', isActive: true }
}

export default function Products() {
  const { t } = useTranslation()
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [orgs, setOrgs] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState('')
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [syncing, setSyncing] = useState(false)
  const [syncProgress, setSyncProgress] = useState(null)
  const syncAbortRef = useRef(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState('add')
  const [editId, setEditId] = useState(null)
  const [form, setForm] = useState(emptyForm())
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const loadOrgs = isSuperAdmin ? orgsApi.list() : orgsApi.myAccess()
    loadOrgs.then(setOrgs).catch(() => setOrgs([]))
  }, [isSuperAdmin])

  useEffect(() => {
    if (!selectedOrgId) {
      setProducts([])
      return
    }
    loadProducts()
  }, [selectedOrgId])

  useEffect(() => {
    if (!success) return undefined
    const timer = setTimeout(() => setSuccess(null), 3500)
    return () => clearTimeout(timer)
  }, [success])

  async function loadProducts() {
    setLoading(true)
    setError(null)
    try {
      const data = await productsApi.list(Number(selectedOrgId))
      setProducts(data)
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
      setProducts((prev) => prev.filter((p) => p.productId !== product.productId))
      setSuccess(t('products.deleted'))
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || t('products.deleteFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    }
  }

  function handlePullFromShop() {
    if (!selectedOrgId || syncing) return
    setSyncing(true)
    setError(null)
    setSyncProgress({ synced: 0, total: 0 })

    const stream = productsApi.syncStream(Number(selectedOrgId), {
      onProgress: (data) => {
        setSyncProgress({ synced: data.synced, total: data.total })
      },
      onDone: async (data) => {
        setSyncProgress({ synced: data.synced, total: data.total })
        try {
          await loadProducts()
          setSuccess(t('products.pullSuccess', { count: data.synced }))
        } catch {
          setError(t('products.loadFailed'))
        } finally {
          setSyncing(false)
          setSyncProgress(null)
          syncAbortRef.current = null
        }
      },
      onError: (err) => {
        const msg = err?.message || t('products.pullError')
        setError(msg)
        setSyncing(false)
        setSyncProgress(null)
        syncAbortRef.current = null
      },
    })
    syncAbortRef.current = stream
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
            <button
              type="button"
              className="secondary-button"
              onClick={handlePullFromShop}
              disabled={syncing}
            >
              {syncing ? t('products.pulling') : t('products.pullFromShop')}
            </button>
            <button type="button" className="primary-button" onClick={openAdd}>
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
            onChange={(e) => setSelectedOrgId(e.target.value)}
          >
            <option value="">{t('products.selectOrg')}</option>
            {orgs.map((org) => (
              <option key={org.orgId} value={org.orgId}>{org.name}</option>
            ))}
          </select>
        </div>
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


      {syncing && syncProgress && (
        <div className="card sync-progress-card" style={{ marginBottom: '1rem' }}>
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
