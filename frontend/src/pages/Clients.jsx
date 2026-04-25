import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { clientsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const emptyForm = { name: '', status: 'ACTIVE', currency: 'RSD', isActive: true }
const STATUS_OPTIONS = ['ACTIVE', 'SETUP', 'SUSPENDED', 'INACTIVE']
const CURRENCY_OPTIONS = ['RSD', 'EUR', 'USD']

export default function Clients() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState('add')
  const [editClientId, setEditClientId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    loadClients()
  }, [])

  useEffect(() => {
    if (successMsg) {
      const t = setTimeout(() => setSuccessMsg(null), 4000)
      return () => clearTimeout(t)
    }
  }, [successMsg])

  async function loadClients() {
    try {
      setLoading(true)
      setError(null)
      setClients(await clientsApi.list())
    } catch {
      setError('Failed to load clients')
    } finally {
      setLoading(false)
    }
  }

  function openAddModal() {
    setForm(emptyForm)
    setFormError(null)
    setModalMode('add')
    setEditClientId(null)
    setModalOpen(true)
  }

  function openEditModal(c) {
    setForm({ name: c.name, status: c.status, currency: c.currency, isActive: c.isActive })
    setFormError(null)
    setModalMode('edit')
    setEditClientId(c.clientId)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setFormError(null)
  }

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setFormError(null)
    if (!form.name.trim()) {
      setFormError('Name is required')
      return
    }
    try {
      setSaving(true)
      if (modalMode === 'add') {
        await clientsApi.create({ ...form, name: form.name.trim() })
        setSuccessMsg('Client created successfully')
      } else {
        await clientsApi.update(editClientId, { ...form, name: form.name.trim() })
        setSuccessMsg('Client updated successfully')
      }
      closeModal()
      await loadClients()
    } catch (err) {
      setFormError(err.response?.data?.message || err.response?.data || 'Operation failed')
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppShell
      title="Clients"
      subtitle="Client accounts and subscription boundaries."
      actions={
        isSuperAdmin && (
          <button className="primary-button" onClick={openAddModal}>
            Add Client
          </button>
        )
      }
    >
      {successMsg && <div className="success-banner">{successMsg}</div>}
      {error && <div className="error-banner">{error}</div>}

      <section className="action-bar card">
        <span className="badge">{clients.length} clients</span>
      </section>

      <section className="table-card">
        {loading ? (
          <p className="muted">Loading…</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Status</th>
                <th>Currency</th>
                <th>Active</th>
                <th>Created</th>
                {isSuperAdmin && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {clients.map((c) => (
                <tr key={c.clientId}>
                  <td>{c.name}</td>
                  <td>
                    <span className={`status-chip ${(c.status || '').toLowerCase()}`}>{c.status}</span>
                  </td>
                  <td>{c.currency}</td>
                  <td>
                    <span className={`status-chip ${c.isActive ? 'active' : 'inactive'}`}>
                      {c.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '—'}</td>
                  {isSuperAdmin && (
                    <td>
                      <button className="secondary-button" onClick={() => openEditModal(c)}>
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
              <h3>{modalMode === 'add' ? 'Add Client' : 'Edit Client'}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="field">
                  <label>Name *</label>
                  <input
                    value={form.name}
                    onChange={(e) => handleChange('name', e.target.value)}
                    required
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
              </div>
              {formError && <p className="error-text" style={{ marginTop: 12 }}>{formError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={closeModal}>Cancel</button>
                <button type="submit" className="primary-button" disabled={saving}>
                  {saving ? 'Saving…' : modalMode === 'add' ? 'Create Client' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
