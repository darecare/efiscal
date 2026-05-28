import React, { useEffect, useState } from 'react'
import { useTranslation, Trans } from 'react-i18next'
import AppShell from '../components/AppShell'
import { paytypeMapApi, clientsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const PAYMENT_TYPE_VALUES = [0, 1, 2, 3, 4, 5, 6]

function emptyForm() {
  return { paymentMethodCode: '', paymentType: 0, description: '' }
}

export default function PayTypeMap() {
  const { t } = useTranslation()
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [clients, setClients] = useState([])
  const [selectedClientId, setSelectedClientId] = useState('')
  const [mappings, setMappings] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const [editId, setEditId] = useState(null)
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
      .catch(() => setError(t('payTypeMap.loadFailed')))
      .finally(() => setLoading(false))
  }, [selectedClientId, t])

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
      setFormError(t('payTypeMap.paymentMethodRequired'))
      return
    }
    if (!selectedClientId) {
      setFormError(t('payTypeMap.noClientSelected'))
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
      const updated = await paytypeMapApi.list(Number(selectedClientId))
      setMappings(updated)
      setShowForm(false)
      setEditId(null)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || t('payTypeMap.saveFailed')
      setFormError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(mapping) {
    if (!window.confirm(t('payTypeMap.confirmDelete', { code: mapping.paymentMethodCode }))) return
    try {
      await paytypeMapApi.remove(mapping.paytypeMapId)
      setMappings(prev => prev.filter(m => m.paytypeMapId !== mapping.paytypeMapId))
    } catch {
      setError(t('payTypeMap.deleteFailed'))
    }
  }

  const paymentTypeLabel = (v) => t(`payTypeMap.paymentTypes.${v}`, { defaultValue: String(v) })

  return (
    <AppShell
      title={t('payTypeMap.title')}
      subtitle={t('payTypeMap.subtitle')}
      actions={
        selectedClientId && (
          <button className="primary-button" onClick={openAdd}>{t('payTypeMap.addMapping')}</button>
        )
      }
    >
      <div className="card" style={{ marginBottom: '1rem', display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <div className="form-group" style={{ flex: 1, minWidth: '220px', marginBottom: 0 }}>
          <label className="form-label">{t('common.client')}</label>
          <select className="form-input" value={selectedClientId} onChange={e => setSelectedClientId(e.target.value)}>
            <option value="">{t('payTypeMap.selectClient')}</option>
            {clients.map(c => (
              <option key={c.clientId} value={c.clientId}>{c.name || c.clientId}</option>
            ))}
          </select>
        </div>
      </div>

      {showForm && (
        <div className="card" style={{ marginBottom: '1rem', borderLeft: '3px solid #3b82f6' }}>
          <h3 style={{ marginTop: 0 }}>{editId ? t('payTypeMap.editMapping') : t('payTypeMap.newMapping')}</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '1rem' }}>
            <div className="form-group">
              <label className="form-label">{t('payTypeMap.paymentMethodCode')}</label>
              <input
                className="form-input"
                value={form.paymentMethodCode}
                onChange={e => setForm(f => ({ ...f, paymentMethodCode: e.target.value }))}
                placeholder="cash_delivery"
                disabled={!!editId}
              />
            </div>
            <div className="form-group">
              <label className="form-label">{t('payTypeMap.fiscalPaymentType')}</label>
              <select className="form-input" value={form.paymentType} onChange={e => setForm(f => ({ ...f, paymentType: e.target.value }))}>
                {PAYMENT_TYPE_VALUES.map(v => <option key={v} value={v}>{paymentTypeLabel(v)}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">{t('payTypeMap.descriptionOptional')}</label>
              <input className="form-input" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} placeholder={t('payTypeMap.descriptionPlaceholder')} />
            </div>
          </div>
          {formError && <p style={{ color: 'red', marginTop: '0.5rem' }}>{formError}</p>}
          <div style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem' }}>
            <button className="primary-button" onClick={handleSave} disabled={saving}>{saving ? t('common.saving') : t('common.save')}</button>
            <button className="secondary-button" onClick={cancelForm}>{t('common.cancel')}</button>
          </div>
        </div>
      )}

      {error && <p style={{ color: 'red' }}>{error}</p>}

      {selectedClientId && !loading && (
        mappings.length === 0 ? (
          <div className="card">
            <p style={{ color: '#64748b' }}>
              <Trans i18nKey="payTypeMap.emptyHint" components={{ strong: <strong /> }} />
            </p>
          </div>
        ) : (
          <div className="card" style={{ padding: 0 }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>{t('payTypeMap.paymentMethodCode')}</th>
                  <th>{t('payTypeMap.fiscalPaymentType')}</th>
                  <th>{t('common.description')}</th>
                  <th>{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {mappings.map(m => (
                  <tr key={m.paytypeMapId}>
                    <td><code>{m.paymentMethodCode}</code></td>
                    <td>{paymentTypeLabel(m.paymentType)}</td>
                    <td>{m.description || t('common.dash')}</td>
                    <td style={{ display: 'flex', gap: '0.5rem' }}>
                      <button className="secondary-button" style={{ padding: '0.25rem 0.75rem' }} onClick={() => openEdit(m)}>{t('common.edit')}</button>
                      <button className="secondary-button" style={{ padding: '0.25rem 0.75rem', color: 'red' }} onClick={() => handleDelete(m)}>{t('common.delete')}</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {loading && <p>{t('common.loadingDots')}</p>}
    </AppShell>
  )
}
