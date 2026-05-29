import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { orgsApi, clientsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const emptyForm = {
  clientId: '',
  name: '',
  taxId: '',
  status: 'ACTIVE',
  currency: 'RSD',
  isActive: true,
  isSearchshopproducts: false,
}
const STATUS_OPTIONS = ['ACTIVE', 'SETUP', 'SUSPENDED', 'INACTIVE']
const CURRENCY_OPTIONS = ['RSD', 'EUR', 'USD']
const PAYMENT_TYPES = [
  { value: 0, label: 'Other' },
  { value: 1, label: 'Cash' },
  { value: 2, label: 'Card' },
  { value: 3, label: 'Check' },
  { value: 4, label: 'Wire Transfer' },
  { value: 5, label: 'Voucher' },
  { value: 6, label: 'Mobile Money' },
]

export default function Organizations() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [orgs, setOrgs] = useState([])
  const [clients, setClients] = useState([])
  const [filterClientId, setFilterClientId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState('add')
  const [editOrgId, setEditOrgId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [activeTab, setActiveTab] = useState('main')
  const [paymentTypes, setPaymentTypes] = useState([])
  const [paymentTypesLoading, setPaymentTypesLoading] = useState(false)

  useEffect(() => {
    clientsApi.list().then(setClients).catch(() => setClients([]))
  }, [])

  useEffect(() => {
    loadOrgs()
  }, [filterClientId])

  useEffect(() => {
    if (successMsg) {
      const t = setTimeout(() => setSuccessMsg(null), 4000)
      return () => clearTimeout(t)
    }
  }, [successMsg])

  async function loadOrgs() {
    try {
      setLoading(true)
      setError(null)
      setOrgs(await orgsApi.list(filterClientId || undefined))
    } catch {
      setError('Failed to load organizations')
    } finally {
      setLoading(false)
    }
  }

  function openAddModal() {
    setForm({ ...emptyForm, clientId: filterClientId || '' })
    setFormError(null)
    setModalMode('add')
    setEditOrgId(null)
    setActiveTab('main')
    setPaymentTypes([])
    setModalOpen(true)
  }

  function openEditModal(o) {
    setForm({
      clientId: o.clientId || '',
      name: o.name,
      taxId: o.taxId || '',
      status: o.status,
      currency: o.currency,
      isActive: o.isActive,
      isSearchshopproducts: Boolean(o.isSearchshopproducts),
    })
    setFormError(null)
    setModalMode('edit')
    setEditOrgId(o.orgId)
    setActiveTab('main')
    setPaymentTypesLoading(true)
    orgsApi.getPaymentTypes(o.orgId)
      .then(setPaymentTypes)
      .catch(() => setPaymentTypes([]))
      .finally(() => setPaymentTypesLoading(false))
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setFormError(null)
    setActiveTab('main')
  }

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function togglePaymentType(paymentTypeValue) {
    setPaymentTypes((prev) =>
      prev.includes(paymentTypeValue)
        ? prev.filter((pt) => pt !== paymentTypeValue)
        : [...prev, paymentTypeValue]
    )
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setFormError(null)
    if (!form.name.trim()) {
      setFormError('Name is required')
      return
    }
    if (!form.clientId) {
      setFormError('Client is required')
      return
    }
    try {
      setSaving(true)
      const payload = {
        ...form,
        clientId: form.clientId ? Number(form.clientId) : null,
        name: form.name.trim(),
        taxId: form.taxId || null,
      }
      if (modalMode === 'add') {
        await orgsApi.create(payload)
        setSuccessMsg('Organization created successfully')
      } else {
        await orgsApi.update(editOrgId, payload)
        await orgsApi.setPaymentTypes(editOrgId, paymentTypes)
        setSuccessMsg('Organization updated successfully')
      }
      closeModal()
      await loadOrgs()
    } catch (err) {
      setFormError(err.response?.data?.message || err.response?.data || 'Operation failed')
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppShell
      title="Organizations"
      subtitle="Organizations linked to clients with access boundaries."
      actions={
        isSuperAdmin && (
          <button className="primary-button" onClick={openAddModal}>
            Add Organization
          </button>
        )
      }
    >
      {successMsg && <div className="success-banner">{successMsg}</div>}
      {error && <div className="error-banner">{error}</div>}

      <section className="filters-panel">
        <div className="filter-grid">
          <div className="field">
            <label>Filter by Client</label>
            <select value={filterClientId} onChange={(e) => setFilterClientId(e.target.value)}>
              <option value="">All Clients</option>
              {clients.map((c) => (
                <option key={c.clientId} value={c.clientId}>{c.name}</option>
              ))}
            </select>
          </div>
        </div>
      </section>

      <section className="action-bar card" style={{ marginTop: 16 }}>
        <span className="badge">{orgs.length} organizations</span>
      </section>

      <section className="table-card">
        {loading ? (
          <p className="muted">Loading…</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Client</th>
                <th>Tax ID (PIB)</th>
                <th>Status</th>
                <th>Currency</th>
                <th>Active</th>
                {isSuperAdmin && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {orgs.map((o) => (
                <tr key={o.orgId}>
                  <td>{o.name}</td>
                  <td>{o.clientName}</td>
                  <td>{o.taxId || '—'}</td>
                  <td>
                    <span className={`status-chip ${(o.status || '').toLowerCase()}`}>{o.status}</span>
                  </td>
                  <td>{o.currency}</td>
                  <td>
                    <span className={`status-chip ${o.isActive ? 'active' : 'inactive'}`}>
                      {o.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {isSuperAdmin && (
                    <td>
                      <button className="secondary-button" onClick={() => openEditModal(o)}>
                        Edit
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {modalOpen && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{modalMode === 'add' ? 'Add Organization' : 'Edit Organization'}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            
            {modalMode === 'edit' && (
              <div className="modal-tabs">
                <button
                  className={`tab-button ${activeTab === 'main' ? 'active' : ''}`}
                  onClick={() => setActiveTab('main')}
                >
                  Main
                </button>
                <button
                  className={`tab-button ${activeTab === 'payment-types' ? 'active' : ''}`}
                  onClick={() => setActiveTab('payment-types')}
                >
                  Payment Types
                </button>
              </div>
            )}
            
            <form onSubmit={handleSubmit}>
              {activeTab === 'main' && (
                <div className="form-grid">
                  <div className="field">
                    <label>Client *</label>
                    <select value={form.clientId} onChange={(e) => handleChange('clientId', e.target.value)} required>
                      <option value="">— Select client —</option>
                      {clients.map((c) => (
                        <option key={c.clientId} value={c.clientId}>{c.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className="field">
                    <label>Organization Name *</label>
                    <input
                      value={form.name}
                      onChange={(e) => handleChange('name', e.target.value)}
                      required
                    />
                  </div>
                  <div className="field">
                    <label>Tax ID (PIB)</label>
                    <input
                      value={form.taxId}
                      onChange={(e) => handleChange('taxId', e.target.value)}
                      placeholder="e.g. 101234567"
                    />
                  </div>
                  <div className="field">
                    <label>Status</label>
                    <select value={form.status} onChange={(e) => handleChange('status', e.target.value)}>
                      {STATUS_OPTIONS.map((s) => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </select>
                  </div>
                  <div className="field">
                    <label>Currency</label>
                    <select value={form.currency} onChange={(e) => handleChange('currency', e.target.value)}>
                      {CURRENCY_OPTIONS.map((c) => (
                        <option key={c} value={c}>{c}</option>
                      ))}
                    </select>
                  </div>
                  {modalMode === 'edit' && (
                    <div className="field">
                      <label>Active</label>
                      <select
                        value={form.isActive ? 'true' : 'false'}
                        onChange={(e) => handleChange('isActive', e.target.value === 'true')}
                      >
                        <option value="true">Active</option>
                        <option value="false">Inactive</option>
                      </select>
                    </div>
                  )}
                  <div className="field" style={{ gridColumn: '1 / -1' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <input
                        type="checkbox"
                        checked={form.isSearchshopproducts}
                        onChange={(e) => handleChange('isSearchshopproducts', e.target.checked)}
                      />
                      <span>Search product data from shop</span>
                    </label>
                    <small className="muted">Search products directly from shop for manual creation of fiscal bill</small>
                  </div>
                </div>
              )}
              
              {activeTab === 'payment-types' && (
                <div style={{ padding: '20px' }}>
                  <h4 style={{ marginBottom: '16px' }}>Allowed Payment Types</h4>
                  {paymentTypesLoading ? (
                    <p className="muted">Loading…</p>
                  ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                      {PAYMENT_TYPES.map((pt) => (
                        <label key={pt.value} style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                          <input
                            type="checkbox"
                            checked={paymentTypes.includes(pt.value)}
                            onChange={() => togglePaymentType(pt.value)}
                          />
                          <span>{pt.label}</span>
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              )}
              
              {formError && <p className="error-text" style={{ marginTop: 12, padding: '0 20px' }}>{formError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={closeModal}>Cancel</button>
                <button type="submit" className="primary-button" disabled={saving}>
                  {saving ? 'Saving…' : modalMode === 'add' ? 'Create Organization' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
