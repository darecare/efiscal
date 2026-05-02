import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { paytypeMapApi, clientsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const PAYMENT_TYPES = [
  { value: 0, label: '0 – Other' },
  { value: 1, label: '1 – Cash' },
  { value: 2, label: '2 – Card' },
  { value: 3, label: '3 – Check' },
  { value: 4, label: '4 – Wire Transfer' },
  { value: 5, label: '5 – Voucher' },
  { value: 6, label: '6 – Mobile Money' },
]

function emptyForm() {
  return { paymentMethodCode: '', paymentType: 0, description: '' }
}

export default function PayTypeMap() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [clients, setClients] = useState([])
  const [selectedClientId, setSelectedClientId] = useState('')
  const [mappings, setMappings] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // Edit/Add state
  const [editId, setEditId] = useState(null)   // null = adding new
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(emptyForm())
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState(null)

  useEffect(() => {
    clientsApi.list().then(setClients).catch(() => setClients([]))
  }, [])

  useEffect(() => {
    if (!selectedClientId) { setMappings([]); return }
    setLoading(true)
    setError(null)
    paytypeMapApi.list(Number(selectedClientId))
      .then(setMappings)
      .catch(() => setError('Failed to load mappings.'))
      .finally(() => setLoading(false))
  }, [selectedClientId])

  function openAdd() {
    setEditId(null)
    setForm(emptyForm())
    setFormError(null)
    setShowForm(true)
  }

  function openEdit(mapping) {
    setEditId(mapping.paytypeMapId)
    setForm({
      paymentMethodCode: mapping.paymentMethodCode || '',
      paymentType: mapping.paymentType ?? 0,
      description: mapping.description || '',
    })
    setFormError(null)
    setShowForm(true)
  }

  function cancelForm() {
    setShowForm(false)
    setEditId(null)
    setFormError(null)
  }

  async function handleSave() {
    if (!form.paymentMethodCode.trim()) {
      setFormError('Payment method code is required.')
      return
    }
    if (!selectedClientId) {
      setFormError('No client selected.')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      if (editId) {
        await paytypeMapApi.update(editId, {
          clientId: Number(selectedClientId),
          paymentMethodCode: form.paymentMethodCode.trim(),
          paymentType: parseInt(form.paymentType),
          description: form.description,
        })
      } else {
        await paytypeMapApi.create({
          clientId: Number(selectedClientId),
          paymentMethodCode: form.paymentMethodCode.trim(),
          paymentType: parseInt(form.paymentType),
          description: form.description,
        })
      }
      // Refresh list
      const updated = await paytypeMapApi.list(Number(selectedClientId))
      setMappings(updated)
      setShowForm(false)
      setEditId(null)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || 'Save failed.'
      setFormError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(mapping) {
    if (!window.confirm(`Delete mapping for "${mapping.paymentMethodCode}"?`)) return
    try {
      await paytypeMapApi.remove(mapping.paytypeMapId)
      setMappings(prev => prev.filter(m => m.paytypeMapId !== mapping.paytypeMapId))
    } catch {
      setError('Failed to delete mapping.')
    }
  }

  const paymentTypeLabel = (v) => PAYMENT_TYPES.find(t => t.value === v)?.label ?? String(v)

  return (
    <AppShell
      title="Payment Type Mapping"
      subtitle="Map MerchantPro payment method codes to fiscal payment types (spec 4.1.6)"
      actions={
        selectedClientId && (
          <button className="primary-button" onClick={openAdd}>+ Add Mapping</button>
        )
      }
    >
      {/* Client selector */}
      <div className="card" style={{ marginBottom: '1rem', display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <div className="form-group" style={{ flex: 1, minWidth: '220px', marginBottom: 0 }}>
          <label className="form-label">Client</label>
          <select className="form-input" value={selectedClientId} onChange={e => setSelectedClientId(e.target.value)}>
            <option value="">Select client…</option>
            {clients.map(c => (
              <option key={c.clientId} value={c.clientId}>{c.name || c.clientId}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Add / Edit form */}
      {showForm && (
        <div className="card" style={{ marginBottom: '1rem', borderLeft: '3px solid #3b82f6' }}>
          <h3 style={{ marginTop: 0 }}>{editId ? 'Edit Mapping' : 'New Mapping'}</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '1rem' }}>
            <div className="form-group">
              <label className="form-label">Payment Method Code</label>
              <input
                className="form-input"
                value={form.paymentMethodCode}
                onChange={e => setForm(f => ({ ...f, paymentMethodCode: e.target.value }))}
                placeholder="e.g. cash_delivery"
                disabled={!!editId}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Fiscal Payment Type</label>
              <select className="form-input" value={form.paymentType} onChange={e => setForm(f => ({ ...f, paymentType: e.target.value }))}>
                {PAYMENT_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Description (optional)</label>
              <input className="form-input" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} placeholder="Description" />
            </div>
          </div>
          {formError && <p style={{ color: 'red', marginTop: '0.5rem' }}>{formError}</p>}
          <div style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem' }}>
            <button className="primary-button" onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
            <button className="secondary-button" onClick={cancelForm}>Cancel</button>
          </div>
        </div>
      )}

      {/* Error */}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {/* Table */}
      {selectedClientId && !loading && (
        mappings.length === 0 ? (
          <div className="card">
            <p style={{ color: '#64748b' }}>No payment type mappings found for this client. Click <strong>+ Add Mapping</strong> to create one.</p>
          </div>
        ) : (
          <div className="card" style={{ padding: 0 }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Payment Method Code</th>
                  <th>Fiscal Payment Type</th>
                  <th>Description</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {mappings.map(m => (
                  <tr key={m.paytypeMapId}>
                    <td><code>{m.paymentMethodCode}</code></td>
                    <td>{paymentTypeLabel(m.paymentType)}</td>
                    <td>{m.description || '—'}</td>
                    <td style={{ display: 'flex', gap: '0.5rem' }}>
                      <button className="secondary-button" style={{ padding: '0.25rem 0.75rem' }} onClick={() => openEdit(m)}>Edit</button>
                      <button className="secondary-button" style={{ padding: '0.25rem 0.75rem', color: 'red' }} onClick={() => handleDelete(m)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {loading && <p>Loading…</p>}
    </AppShell>
  )
}
