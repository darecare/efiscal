import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { clientsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const emptyForm = { name: '', status: 'ACTIVE', currency: 'RSD', isActive: true }
const STATUS_OPTIONS = ['ACTIVE', 'SETUP', 'SUSPENDED', 'INACTIVE']
const CURRENCY_OPTIONS = ['RSD', 'EUR', 'USD']

export default function Clients() {
  const { t } = useTranslation()
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
      const timer = setTimeout(() => setSuccessMsg(null), 4000)
      return () => clearTimeout(timer)
    }
  }, [successMsg])

  async function loadClients() {
    try {
      setLoading(true)
      setError(null)
      setClients(await clientsApi.list())
    } catch {
      setError(t('clients.loadFailed'))
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
      setFormError(t('clients.nameRequired'))
      return
    }
    try {
      setSaving(true)
      if (modalMode === 'add') {
        await clientsApi.create({ ...form, name: form.name.trim() })
        setSuccessMsg(t('clients.createdSuccess'))
      } else {
        await clientsApi.update(editClientId, { ...form, name: form.name.trim() })
        setSuccessMsg(t('clients.updatedSuccess'))
      }
      closeModal()
      await loadClients()
    } catch (err) {
      setFormError(err.response?.data?.message || err.response?.data || t('common.operationFailed'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppShell
      title={t('clients.title')}
      subtitle={t('clients.subtitle')}
      actions={
        isSuperAdmin && (
          <button className="primary-button" onClick={openAddModal}>
            {t('clients.addClient')}
          </button>
        )
      }
    >
      {successMsg && <div className="success-banner">{successMsg}</div>}
      {error && <div className="error-banner">{error}</div>}

      <section className="action-bar card">
        <span className="badge">{t('common.counts.clients', { count: clients.length })}</span>
      </section>

      <section className="table-card">
        {loading ? (
          <p className="muted">{t('common.loadingDots')}</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>{t('common.name')}</th>
                <th>{t('common.status')}</th>
                <th>{t('common.currency')}</th>
                <th>{t('common.active')}</th>
                <th>{t('common.created')}</th>
                {isSuperAdmin && <th>{t('common.actions')}</th>}
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
                      {c.isActive ? t('common.active') : t('common.inactive')}
                    </span>
                  </td>
                  <td>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : t('common.dash')}</td>
                  {isSuperAdmin && (
                    <td>
                      <button className="secondary-button" onClick={() => openEditModal(c)}>
                        {t('common.edit')}
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
              <h3>{modalMode === 'add' ? t('clients.addModalTitle') : t('clients.editModalTitle')}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="field">
                  <label>{t('clients.nameLabel')} *</label>
                  <input
                    value={form.name}
                    onChange={(e) => handleChange('name', e.target.value)}
                    required
                  />
                </div>
                <div className="field">
                  <label>{t('common.status')}</label>
                  <select value={form.status} onChange={(e) => handleChange('status', e.target.value)}>
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>{t('common.currency')}</label>
                  <select value={form.currency} onChange={(e) => handleChange('currency', e.target.value)}>
                    {CURRENCY_OPTIONS.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>
                {modalMode === 'edit' && (
                  <div className="field">
                    <label>{t('common.active')}</label>
                    <select
                      value={form.isActive ? 'true' : 'false'}
                      onChange={(e) => handleChange('isActive', e.target.value === 'true')}
                    >
                      <option value="true">{t('common.active')}</option>
                      <option value="false">{t('common.inactive')}</option>
                    </select>
                  </div>
                )}
              </div>
              {formError && <p className="error-text" style={{ marginTop: 12 }}>{formError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={closeModal}>{t('common.cancel')}</button>
                <button type="submit" className="primary-button" disabled={saving}>
                  {saving ? t('common.saving') : modalMode === 'add' ? t('clients.createClient') : t('common.saveChanges')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
