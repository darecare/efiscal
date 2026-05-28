import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
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
const PAYMENT_TYPE_VALUES = [0, 1, 2, 3, 4, 5, 6]

export default function Organizations() {
  const { t } = useTranslation()
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
      const timer = setTimeout(() => setSuccessMsg(null), 4000)
      return () => clearTimeout(timer)
    }
  }, [successMsg])

  async function loadOrgs() {
    try {
      setLoading(true)
      setError(null)
      setOrgs(await orgsApi.list(filterClientId || undefined))
    } catch {
      setError(t('organizations.loadFailed'))
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
      setFormError(t('organizations.nameRequired'))
      return
    }
    if (!form.clientId) {
      setFormError(t('organizations.clientRequired'))
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
        setSuccessMsg(t('organizations.createdSuccess'))
      } else {
        await orgsApi.update(editOrgId, payload)
        await orgsApi.setPaymentTypes(editOrgId, paymentTypes)
        setSuccessMsg(t('organizations.updatedSuccess'))
      }
      closeModal()
      await loadOrgs()
    } catch (err) {
      setFormError(err.response?.data?.message || err.response?.data || t('common.operationFailed'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppShell
      title={t('organizations.title')}
      subtitle={t('organizations.subtitle')}
      actions={
        isSuperAdmin && (
          <button className="primary-button" onClick={openAddModal}>
            {t('organizations.addOrganization')}
          </button>
        )
      }
    >
      {successMsg && <div className="success-banner">{successMsg}</div>}
      {error && <div className="error-banner">{error}</div>}

      <section className="filters-panel">
        <div className="filter-grid">
          <div className="field">
            <label>{t('organizations.filterByClient')}</label>
            <select value={filterClientId} onChange={(e) => setFilterClientId(e.target.value)}>
              <option value="">{t('organizations.allClients')}</option>
              {clients.map((c) => (
                <option key={c.clientId} value={c.clientId}>{c.name}</option>
              ))}
            </select>
          </div>
        </div>
      </section>

      <section className="action-bar card" style={{ marginTop: 16 }}>
        <span className="badge">{t('common.counts.organizations', { count: orgs.length })}</span>
      </section>

      <section className="table-card">
        {loading ? (
          <p className="muted">{t('common.loadingDots')}</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>{t('common.name')}</th>
                <th>{t('common.client')}</th>
                <th>{t('organizations.taxId')}</th>
                <th>{t('common.status')}</th>
                <th>{t('common.currency')}</th>
                <th>{t('common.active')}</th>
                {isSuperAdmin && <th>{t('common.actions')}</th>}
              </tr>
            </thead>
            <tbody>
              {orgs.map((o) => (
                <tr key={o.orgId}>
                  <td>{o.name}</td>
                  <td>{o.clientName}</td>
                  <td>{o.taxId || t('common.dash')}</td>
                  <td>
                    <span className={`status-chip ${(o.status || '').toLowerCase()}`}>{o.status}</span>
                  </td>
                  <td>{o.currency}</td>
                  <td>
                    <span className={`status-chip ${o.isActive ? 'active' : 'inactive'}`}>
                      {o.isActive ? t('common.active') : t('common.inactive')}
                    </span>
                  </td>
                  {isSuperAdmin && (
                    <td>
                      <button className="secondary-button" onClick={() => openEditModal(o)}>
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
              <h3>{modalMode === 'add' ? t('organizations.addModalTitle') : t('organizations.editModalTitle')}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>

            {modalMode === 'edit' && (
              <div className="modal-tabs">
                <button
                  className={`tab-button ${activeTab === 'main' ? 'active' : ''}`}
                  onClick={() => setActiveTab('main')}
                >
                  {t('common.main')}
                </button>
                <button
                  className={`tab-button ${activeTab === 'payment-types' ? 'active' : ''}`}
                  onClick={() => setActiveTab('payment-types')}
                >
                  {t('organizations.paymentTypes')}
                </button>
              </div>
            )}

            <form onSubmit={handleSubmit}>
              {activeTab === 'main' && (
                <div className="form-grid">
                  <div className="field">
                    <label>{t('organizations.clientLabel')} *</label>
                    <select value={form.clientId} onChange={(e) => handleChange('clientId', e.target.value)} required>
                      <option value="">{t('common.selectClientPlaceholder')}</option>
                      {clients.map((c) => (
                        <option key={c.clientId} value={c.clientId}>{c.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className="field">
                    <label>{t('organizations.orgNameLabel')} *</label>
                    <input
                      value={form.name}
                      onChange={(e) => handleChange('name', e.target.value)}
                      required
                    />
                  </div>
                  <div className="field">
                    <label>{t('organizations.taxId')}</label>
                    <input
                      value={form.taxId}
                      onChange={(e) => handleChange('taxId', e.target.value)}
                      placeholder={t('organizations.taxIdPlaceholder', { example: '101234567' })}
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
                  <h4 style={{ marginBottom: '16px' }}>{t('organizations.allowedPaymentTypes')}</h4>
                  {paymentTypesLoading ? (
                    <p className="muted">{t('common.loadingDots')}</p>
                  ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                      {PAYMENT_TYPE_VALUES.map((pt) => (
                        <label key={pt} style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                          <input
                            type="checkbox"
                            checked={paymentTypes.includes(pt)}
                            onChange={() => togglePaymentType(pt)}
                          />
                          <span>{t(`organizations.paymentTypeLabels.${pt}`)}</span>
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {formError && <p className="error-text" style={{ marginTop: 12, padding: '0 20px' }}>{formError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={closeModal}>{t('common.cancel')}</button>
                <button type="submit" className="primary-button" disabled={saving}>
                  {saving ? t('common.saving') : modalMode === 'add' ? t('organizations.createOrganization') : t('common.saveChanges')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
