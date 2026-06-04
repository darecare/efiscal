import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { emailTemplatesApi, orgsApi } from '../services/api'

const emptyForm = {
  orgId: '',
  templateName: '',
  subject: '',
  bodyHtml: '',
  isActive: true,
}

export default function EmailTemplates() {
  const { t } = useTranslation()

  const [orgs, setOrgs] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState('')
  const [templates, setTemplates] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState('add')
  const [editTemplateId, setEditTemplateId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    orgsApi.list().then(setOrgs).catch(() => setOrgs([]))
  }, [])

  useEffect(() => {
    if (!selectedOrgId) {
      setTemplates([])
      return
    }
    loadTemplates(selectedOrgId)
  }, [selectedOrgId])

  useEffect(() => {
    if (!successMsg) return undefined
    const timer = setTimeout(() => setSuccessMsg(null), 4000)
    return () => clearTimeout(timer)
  }, [successMsg])

  async function loadTemplates(orgId) {
    try {
      setLoading(true)
      setError(null)
      setTemplates(await emailTemplatesApi.list(orgId))
    } catch (err) {
      setError(err?.response?.data?.message || err?.response?.data || t('emailTemplates.loadFailed'))
    } finally {
      setLoading(false)
    }
  }

  function openAddModal() {
    setForm({ ...emptyForm, orgId: selectedOrgId })
    setFormError(null)
    setModalMode('add')
    setEditTemplateId(null)
    setModalOpen(true)
  }

  function openEditModal(template) {
    setForm({
      orgId: String(template.orgId),
      templateName: template.templateName,
      subject: template.subject,
      bodyHtml: template.bodyHtml,
      isActive: template.isActive,
    })
    setFormError(null)
    setModalMode('edit')
    setEditTemplateId(template.emailTemplateId)
    setModalOpen(true)
  }

  async function submitForm(e) {
    e.preventDefault()
    setFormError(null)

    if (!form.orgId) {
      setFormError(t('emailTemplates.orgRequired'))
      return
    }
    if (!form.templateName.trim()) {
      setFormError(t('emailTemplates.templateNameRequired'))
      return
    }
    if (!form.subject.trim()) {
      setFormError(t('emailTemplates.subjectRequired'))
      return
    }
    if (!form.bodyHtml.trim()) {
      setFormError(t('emailTemplates.bodyRequired'))
      return
    }

    try {
      setSaving(true)
      const payload = {
        orgId: Number(form.orgId),
        templateName: form.templateName.trim(),
        subject: form.subject.trim(),
        bodyHtml: form.bodyHtml,
        isActive: form.isActive,
      }
      if (modalMode === 'add') {
        await emailTemplatesApi.create(payload)
        setSuccessMsg(t('emailTemplates.createdSuccess'))
      } else {
        await emailTemplatesApi.update(editTemplateId, payload)
        setSuccessMsg(t('emailTemplates.updatedSuccess'))
      }
      setModalOpen(false)
      await loadTemplates(selectedOrgId || form.orgId)
    } catch (err) {
      setFormError(err?.response?.data?.message || err?.response?.data || t('common.operationFailed'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(template) {
    const confirmed = window.confirm(t('emailTemplates.confirmDelete', { name: template.templateName }))
    if (!confirmed) return
    try {
      await emailTemplatesApi.remove(template.emailTemplateId)
      setSuccessMsg(t('emailTemplates.deletedSuccess'))
      await loadTemplates(selectedOrgId)
    } catch (err) {
      setError(err?.response?.data?.message || err?.response?.data || t('common.operationFailed'))
    }
  }

  return (
    <AppShell
      title={t('emailTemplates.title')}
      subtitle={t('emailTemplates.subtitle')}
      actions={<button className="primary-button" onClick={openAddModal} disabled={!selectedOrgId}>{t('emailTemplates.addTemplate')}</button>}
    >
      {successMsg && <div className="success-banner">{successMsg}</div>}
      {error && <div className="error-banner">{error}</div>}

      <section className="filters-panel">
        <div className="filter-grid">
          <div className="field">
            <label>{t('common.organization')}</label>
            <select value={selectedOrgId} onChange={(e) => setSelectedOrgId(e.target.value)}>
              <option value="">{t('common.selectOrgPlaceholder')}</option>
              {orgs.map((org) => (
                <option key={org.orgId} value={org.orgId}>{org.name}</option>
              ))}
            </select>
          </div>
        </div>
      </section>

      <section className="table-card">
        {loading ? (
          <p className="muted">{t('common.loadingDots')}</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>{t('common.name')}</th>
                <th>{t('emailTemplates.subject')}</th>
                <th>{t('emailTemplates.organization')}</th>
                <th>{t('common.status')}</th>
                <th>{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {templates.map((template) => (
                <tr key={template.emailTemplateId}>
                  <td>{template.templateName}</td>
                  <td>{template.subject}</td>
                  <td>{template.orgName}</td>
                  <td>
                    <span className={`status-chip ${template.isActive ? 'active' : 'inactive'}`}>
                      {template.isActive ? t('common.active') : t('common.inactive')}
                    </span>
                  </td>
                  <td>
                    <div className="table-row-actions">
                      <button className="secondary-button" onClick={() => openEditModal(template)}>{t('common.edit')}</button>
                      <button className="secondary-button danger" onClick={() => handleDelete(template)}>{t('common.delete')}</button>
                    </div>
                  </td>
                </tr>
              ))}
              {templates.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', opacity: 0.5, padding: '24px 0' }}>{t('emailTemplates.noTemplates')}</td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </section>

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{modalMode === 'add' ? t('emailTemplates.addTitle') : t('emailTemplates.editTitle')}</h3>
              <button className="modal-close" onClick={() => setModalOpen(false)} aria-label={t('common.close')}>×</button>
            </div>

            <form onSubmit={submitForm}>
              <div className="form-grid">
                <div className="field">
                  <label>{t('common.organization')} *</label>
                  <select value={form.orgId} onChange={(e) => setForm((prev) => ({ ...prev, orgId: e.target.value }))}>
                    <option value="">{t('common.selectOrgPlaceholder')}</option>
                    {orgs.map((org) => (
                      <option key={org.orgId} value={org.orgId}>{org.name}</option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>{t('emailTemplates.templateName')} *</label>
                  <input value={form.templateName} onChange={(e) => setForm((prev) => ({ ...prev, templateName: e.target.value }))} />
                </div>
                <div className="field">
                  <label>{t('emailTemplates.subject')} *</label>
                  <input value={form.subject} onChange={(e) => setForm((prev) => ({ ...prev, subject: e.target.value }))} />
                </div>
                <div className="field" style={{ gridColumn: '1 / -1' }}>
                  <label>{t('emailTemplates.bodyHtml')} *</label>
                  <textarea
                    value={form.bodyHtml}
                    onChange={(e) => setForm((prev) => ({ ...prev, bodyHtml: e.target.value }))}
                    rows={12}
                    style={{ fontFamily: 'monospace' }}
                    placeholder={t('emailTemplates.bodyPlaceholder')}
                  />
                  <small className="muted">{t('emailTemplates.bodyHint')}</small>
                </div>
                {modalMode === 'edit' && (
                  <div className="field">
                    <label>{t('common.status')}</label>
                    <select
                      value={form.isActive ? 'true' : 'false'}
                      onChange={(e) => setForm((prev) => ({ ...prev, isActive: e.target.value === 'true' }))}
                    >
                      <option value="true">{t('common.active')}</option>
                      <option value="false">{t('common.inactive')}</option>
                    </select>
                  </div>
                )}
              </div>

              {formError && <p className="error-text" style={{ marginTop: 12 }}>{formError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={() => setModalOpen(false)}>{t('common.cancel')}</button>
                <button type="submit" className="primary-button" disabled={saving}>{saving ? t('common.saving') : t('common.saveChanges')}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}