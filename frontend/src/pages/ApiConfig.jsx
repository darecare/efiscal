import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { apiConnApi, apiTemplateApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const PLATFORM_VALUES = ['MP', 'WO', 'SH', 'FS']
const AUTH_OPTIONS = ['BASIC_AUTH', 'OAUTH', 'MTLS', 'NONE']
const METHOD_OPTIONS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE']

const emptyConnForm = {
  orgId: '', displayName: '', apiPlatform: 'MP',
  apiBaseUrl: '', apiauthtype: 'BASIC_AUTH', apikey: '', apisecret: '',
  certData: null, certPassword: '', pac: '', isActive: true,
}
const emptyTplForm = {
  operationKey: '', httpMethod: 'GET', contentType: 'application/json', endpointPath: '', isActive: true,
}

export default function ApiConfig() {
  const { t } = useTranslation()
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [orgs, setOrgs] = useState([])
  const [connections, setConnections] = useState([])
  const [selectedConn, setSelectedConn] = useState(null)
  const [templates, setTemplates] = useState([])
  const [loadingConn, setLoadingConn] = useState(true)
  const [loadingTpl, setLoadingTpl] = useState(false)
  const [successMsg, setSuccessMsg] = useState(null)
  const [error, setError] = useState(null)

  const [connModal, setConnModal] = useState(false)
  const [connMode, setConnMode] = useState('add')
  const [editConnId, setEditConnId] = useState(null)
  const [connForm, setConnForm] = useState(emptyConnForm)
  const [connFormError, setConnFormError] = useState(null)
  const [connSaving, setConnSaving] = useState(false)

  const [tplModal, setTplModal] = useState(false)
  const [tplMode, setTplMode] = useState('add')
  const [editTplId, setEditTplId] = useState(null)
  const [tplForm, setTplForm] = useState(emptyTplForm)
  const [tplFormError, setTplFormError] = useState(null)
  const [tplSaving, setTplSaving] = useState(false)

  useEffect(() => {
    orgsApi.list().then(setOrgs).catch(() => setOrgs([]))
    loadConnections()
  }, [])

  useEffect(() => {
    if (successMsg) {
      const t = setTimeout(() => setSuccessMsg(null), 4000)
      return () => clearTimeout(t)
    }
  }, [successMsg])

  async function loadConnections() {
    try {
      setLoadingConn(true)
      setError(null)
      setConnections(await apiConnApi.list())
    } catch {
      setError(t('apiConfig.loadFailed'))
    } finally {
      setLoadingConn(false)
    }
  }

  async function selectConn(conn) {
    setSelectedConn(conn)
    setLoadingTpl(true)
    try {
      setTemplates(await apiTemplateApi.list(conn.apiconnId))
    } catch {
      setTemplates([])
    } finally {
      setLoadingTpl(false)
    }
  }

  function openAddConn() {
    setConnForm(emptyConnForm)
    setConnFormError(null)
    setConnMode('add')
    setEditConnId(null)
    setConnModal(true)
  }

  function openEditConn(c) {
    setConnForm({
      orgId: c.orgId || '', displayName: c.displayName, apiPlatform: c.apiPlatform,
      apiBaseUrl: c.apiBaseUrl || '', apiauthtype: c.apiauthtype || 'NONE',
      apikey: '', apisecret: '', certData: null, certPassword: '', pac: c.pac || '', isActive: c.isActive,
    })
    setConnFormError(null)
    setConnMode('edit')
    setEditConnId(c.apiconnId)
    setConnModal(true)
  }

  async function handleConnSubmit(e) {
    e.preventDefault()
    setConnFormError(null)
    if (!connForm.displayName.trim()) { setConnFormError(t('apiConfig.displayNameRequired')); return }
    if (!connForm.orgId) { setConnFormError(t('apiConfig.organizationRequired')); return }
    try {
      setConnSaving(true)
      const payload = {
        ...connForm,
        orgId: Number(connForm.orgId),
        apikey: connForm.apikey.trim() || null,
        apisecret: connForm.apisecret.trim() || null,
        certData: connForm.certData || null,
        certPassword: connForm.certPassword.trim() || null,
        pac: connForm.pac.trim() || null,
      }
      if (connMode === 'add') {
        await apiConnApi.create(payload)
        setSuccessMsg(t('apiConfig.connectionCreated'))
      } else {
        await apiConnApi.update(editConnId, payload)
        setSuccessMsg(t('apiConfig.connectionUpdated'))
      }
      setConnModal(false)
      const updated = await apiConnApi.list()
      setConnections(updated)
      if (connMode === 'edit' && selectedConn?.apiconnId === editConnId) {
        const found = updated.find(x => x.apiconnId === editConnId)
        if (found) setSelectedConn(found)
      }
    } catch (err) {
      setConnFormError(err.response?.data?.message || err.response?.data || t('common.operationFailed'))
    } finally {
      setConnSaving(false)
    }
  }

  function openAddTpl() {
    setTplForm(emptyTplForm)
    setTplFormError(null)
    setTplMode('add')
    setEditTplId(null)
    setTplModal(true)
  }

  function openEditTpl(t) {
    setTplForm({
      operationKey: t.operationKey, httpMethod: t.httpMethod,
      contentType: t.contentType, endpointPath: t.endpointPath, isActive: t.isActive,
    })
    setTplFormError(null)
    setTplMode('edit')
    setEditTplId(t.apitemplateId)
    setTplModal(true)
  }

  async function handleTplSubmit(e) {
    e.preventDefault()
    setTplFormError(null)
    if (!tplForm.operationKey.trim()) { setTplFormError(t('apiConfig.operationKeyRequired')); return }
    if (!tplForm.endpointPath.trim()) { setTplFormError(t('apiConfig.endpointPathRequired')); return }
    try {
      setTplSaving(true)
      if (tplMode === 'add') {
        await apiTemplateApi.create({ ...tplForm, apiconnId: selectedConn.apiconnId })
        setSuccessMsg(t('apiConfig.templateCreated'))
      } else {
        await apiTemplateApi.update(editTplId, { ...tplForm, apiconnId: selectedConn.apiconnId })
        setSuccessMsg(t('apiConfig.templateUpdated'))
      }
      setTplModal(false)
      setTemplates(await apiTemplateApi.list(selectedConn.apiconnId))
    } catch (err) {
      setTplFormError(err.response?.data?.message || err.response?.data || t('common.operationFailed'))
    } finally {
      setTplSaving(false)
    }
  }

  return (
    <AppShell
      title={t('apiConfig.title')}
      subtitle={t('apiConfig.subtitle')}
      actions={isSuperAdmin ? <button className="primary-button" onClick={openAddConn}>{t('apiConfig.addConnection')}</button> : null}
    >
      {successMsg && <div className="success-banner">{successMsg}</div>}
      {error && <div className="error-banner">{error}</div>}

      {/* ── Master: Connections ── */}
      <section className="table-card" style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0 }}>{t('apiConfig.connectionsTitle')}</h3>
          <span style={{ fontSize: 13, opacity: 0.6 }}>{t('common.counts.records', { count: connections.length })}</span>
        </div>
        {loadingConn ? <p style={{ opacity: 0.5 }}>{t('common.loadingDots')}</p> : (
          <table>
            <thead>
              <tr>
                <th style={{ width: 24 }}></th>
                <th>{t('common.name')}</th>
                <th>{t('apiConfig.platform')}</th>
                <th>{t('common.organization')}</th>
                <th>{t('apiConfig.baseUrl')}</th>
                <th>{t('apiConfig.authType')}</th>
                <th>{t('common.status')}</th>
                {isSuperAdmin && <th>{t('common.actions')}</th>}
              </tr>
            </thead>
            <tbody>
              {connections.map((c) => (
                <tr
                  key={c.apiconnId}
                  onClick={() => selectConn(c)}
                  style={{
                    cursor: 'pointer',
                    background: selectedConn?.apiconnId === c.apiconnId ? 'rgba(194,117,60,0.12)' : undefined,
                  }}
                >
                  <td style={{ textAlign: 'center', color: '#c2753c' }}>
                    {selectedConn?.apiconnId === c.apiconnId ? '▶' : ''}
                  </td>
                  <td>{c.displayName}</td>
                  <td>{t(`apiConfig.platformLabels.${c.apiPlatform}`, { defaultValue: c.apiPlatform })}</td>
                  <td>{c.orgName}</td>
                  <td style={{ fontFamily: 'monospace', fontSize: 13 }}>{c.apiBaseUrl}</td>
                  <td>{c.apiauthtype}</td>
                  <td>
                    <span className={`status-chip ${c.isActive ? 'active' : 'inactive'}`}>
                      {c.isActive ? t('common.active') : t('common.inactive')}
                    </span>
                  </td>
                  {isSuperAdmin && (
                    <td onClick={ev => ev.stopPropagation()}>
                      <button className="secondary-button" onClick={() => openEditConn(c)}>{t('common.edit')}</button>
                    </td>
                  )}
                </tr>
              ))}
              {connections.length === 0 && (
                <tr>
                  <td colSpan={isSuperAdmin ? 8 : 7} style={{ textAlign: 'center', opacity: 0.45, padding: '24px 0' }}>
                    {t('apiConfig.noConnections')}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </section>

      {/* ── Detail: Templates ── */}
      {selectedConn && (
        <section className="table-card" style={{ borderTop: '3px solid #c2753c' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
            <div>
              <h3 style={{ margin: 0 }}>
                {t('apiConfig.templatesFor')}{' '}
                <span style={{ color: '#c2753c' }}>{selectedConn.displayName}</span>
              </h3>
              <p style={{ margin: '4px 0 0', opacity: 0.55, fontSize: 13 }}>
                {t('apiConfig.templatesSubtitle')}
              </p>
            </div>
            {isSuperAdmin && (
              <button className="primary-button" onClick={openAddTpl}>{t('apiConfig.addTemplate')}</button>
            )}
          </div>
          {loadingTpl ? <p style={{ opacity: 0.5 }}>{t('common.loadingDots')}</p> : (
            <table>
              <thead>
                <tr>
                  <th>{t('apiConfig.operationKey')}</th>
                  <th>{t('apiConfig.method')}</th>
                  <th>{t('apiConfig.contentType')}</th>
                  <th>{t('apiConfig.endpointPath')}</th>
                  <th>{t('common.status')}</th>
                  {isSuperAdmin && <th>{t('common.actions')}</th>}
                </tr>
              </thead>
              <tbody>
                {templates.map((tpl) => (
                  <tr key={tpl.apitemplateId}>
                    <td><code style={{ fontSize: 13 }}>{tpl.operationKey}</code></td>
                    <td>
                      <span style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: 12,
                        background: 'rgba(194,117,60,0.15)', padding: '2px 6px', borderRadius: 4 }}>
                        {tpl.httpMethod}
                      </span>
                    </td>
                    <td style={{ fontSize: 13 }}>{tpl.contentType}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: 13 }}>{tpl.endpointPath}</td>
                    <td>
                      <span className={`status-chip ${tpl.isActive ? 'active' : 'inactive'}`}>
                        {tpl.isActive ? t('common.active') : t('common.inactive')}
                      </span>
                    </td>
                    {isSuperAdmin && (
                      <td>
                        <button className="secondary-button" onClick={() => openEditTpl(tpl)}>{t('common.edit')}</button>
                      </td>
                    )}
                  </tr>
                ))}
                {templates.length === 0 && (
                  <tr>
                    <td colSpan={isSuperAdmin ? 6 : 5} style={{ textAlign: 'center', opacity: 0.45, padding: '24px 0' }}>
                      {t('apiConfig.noTemplates')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </section>
      )}

      {/* ── Connection Modal ── */}
      {connModal && (
        <div className="modal-overlay" onClick={() => setConnModal(false)}>
          <div className="modal" style={{ maxWidth: 750, width: '100%' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{connMode === 'add' ? t('apiConfig.addConnTitle') : t('apiConfig.editConnTitle')}</h3>
              <button className="modal-close" onClick={() => setConnModal(false)} aria-label={t('common.close')}>×</button>
            </div>
            <form onSubmit={handleConnSubmit}>
              <div className="form-grid">
                <div className="field">
                  <label>{t('apiConfig.displayNameLabel')} *</label>
                  <input value={connForm.displayName} onChange={e => setConnForm(p => ({ ...p, displayName: e.target.value }))} required />
                </div>
                <div className="field">
                  <label>{t('apiConfig.organizationLabel')} *</label>
                  <select value={connForm.orgId} onChange={e => setConnForm(p => ({ ...p, orgId: e.target.value }))}>
                    <option value="">{t('apiConfig.selectOrgShort')}</option>
                    {orgs.map(o => <option key={o.orgId} value={o.orgId}>{o.name}</option>)}
                  </select>
                </div>
                <div className="field">
                  <label>{t('apiConfig.platformLabel')} *</label>
                  <select value={connForm.apiPlatform} onChange={e => setConnForm(p => ({ ...p, apiPlatform: e.target.value }))}>
                    {PLATFORM_VALUES.map(x => <option key={x} value={x}>{t(`apiConfig.platformLabels.${x}`)}</option>)}
                  </select>
                </div>
                <div className="field">
                  <label>{t('apiConfig.baseUrlLabel')}</label>
                  <input value={connForm.apiBaseUrl} onChange={e => setConnForm(p => ({ ...p, apiBaseUrl: e.target.value }))} />
                </div>
                <div className="field">
                  <label>{t('apiConfig.authTypeLabel')}</label>
                  <select value={connForm.apiauthtype} onChange={e => setConnForm(p => ({ ...p, apiauthtype: e.target.value }))}>
                    {AUTH_OPTIONS.map(x => <option key={x} value={x}>{x}</option>)}
                  </select>
                </div>
                {connForm.apiauthtype !== 'MTLS' && (
                  <>
                    <div className="field">
                      <label>{t('apiConfig.apiKey')}</label>
                      <input value={connForm.apikey} onChange={e => setConnForm(p => ({ ...p, apikey: e.target.value }))} />
                    </div>
                    <div className="field">
                      <label>{t('apiConfig.apiSecret')}</label>
                      <input type="password" value={connForm.apisecret} onChange={e => setConnForm(p => ({ ...p, apisecret: e.target.value }))} />
                    </div>
                  </>
                )}
                <div className="field">
                  <label>{t('apiConfig.pac')}</label>
                  <input
                    value={connForm.pac}
                    maxLength={10}
                    onChange={e => setConnForm(p => ({ ...p, pac: e.target.value }))}
                    placeholder={t('apiConfig.pacPlaceholder')}
                  />
                </div>
                {connForm.apiauthtype === 'MTLS' && (
                  <>
                    <div className="field" style={{ gridColumn: '1 / -1' }}>
                      <label>{t('apiConfig.certificateFile')}</label>
                      <input
                        type="file"
                        accept=".pem,.p12,.pfx,.crt,.cer"
                        onChange={e => {
                          const file = e.target.files[0]
                          if (!file) { setConnForm(p => ({ ...p, certData: null })); return }
                          const reader = new FileReader()
                          reader.onload = ev => setConnForm(p => ({ ...p, certData: ev.target.result.split(',')[1] }))
                          reader.readAsDataURL(file)
                        }}
                      />
                      {connMode === 'edit' && !connForm.certData && (
                        <span style={{ fontSize: 12, opacity: 0.55 }}>{t('apiConfig.keepCertificate')}</span>
                      )}
                    </div>
                    <div className="field">
                      <label>{t('apiConfig.certificatePassword')}</label>
                      <input
                        type="password"
                        value={connForm.certPassword}
                        onChange={e => setConnForm(p => ({ ...p, certPassword: e.target.value }))}
                        placeholder={connMode === 'edit' ? t('apiConfig.keepPassword') : ''}
                      />
                    </div>
                  </>
                )}
                {connMode === 'edit' && (
                  <div className="field">
                    <label>{t('common.status')}</label>
                    <select value={connForm.isActive ? 'true' : 'false'} onChange={e => setConnForm(p => ({ ...p, isActive: e.target.value === 'true' }))}>
                      <option value="true">{t('common.active')}</option>
                      <option value="false">{t('common.inactive')}</option>
                    </select>
                  </div>
                )}
              </div>
              {connFormError && <p style={{ color: 'var(--error, #c0392b)', marginTop: 12 }}>{connFormError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={() => setConnModal(false)}>{t('common.cancel')}</button>
                <button type="submit" className="primary-button" disabled={connSaving}>
                  {connSaving ? t('common.saving') : connMode === 'add' ? t('common.create') : t('common.saveChanges')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Template Modal ── */}
      {tplModal && (
        <div className="modal-overlay" onClick={() => setTplModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{tplMode === 'add' ? t('apiConfig.addTplTitle') : t('apiConfig.editTplTitle')}</h3>
              <button className="modal-close" onClick={() => setTplModal(false)} aria-label={t('common.close')}>×</button>
            </div>
            <form onSubmit={handleTplSubmit}>
              <div className="form-grid">
                <div className="field">
                  <label>{t('apiConfig.operationKeyLabel')} *</label>
                  <input
                    value={tplForm.operationKey}
                    onChange={e => setTplForm(p => ({ ...p, operationKey: e.target.value }))}
                    placeholder="FETCH_ORDERS"
                    required
                  />
                </div>
                <div className="field">
                  <label>{t('apiConfig.httpMethodLabel')} *</label>
                  <select value={tplForm.httpMethod} onChange={e => setTplForm(p => ({ ...p, httpMethod: e.target.value }))}>
                    {METHOD_OPTIONS.map(x => <option key={x} value={x}>{x}</option>)}
                  </select>
                </div>
                <div className="field">
                  <label>{t('apiConfig.contentTypeLabel')}</label>
                  <input value={tplForm.contentType} onChange={e => setTplForm(p => ({ ...p, contentType: e.target.value }))} />
                </div>
                <div className="field">
                  <label>{t('apiConfig.endpointPathLabel')} *</label>
                  <input
                    value={tplForm.endpointPath}
                    onChange={e => setTplForm(p => ({ ...p, endpointPath: e.target.value }))}
                    placeholder="/v1/orders"
                    required
                  />
                </div>
                {tplMode === 'edit' && (
                  <div className="field">
                    <label>{t('common.status')}</label>
                    <select value={tplForm.isActive ? 'true' : 'false'} onChange={e => setTplForm(p => ({ ...p, isActive: e.target.value === 'true' }))}>
                      <option value="true">{t('common.active')}</option>
                      <option value="false">{t('common.inactive')}</option>
                    </select>
                  </div>
                )}
              </div>
              {tplFormError && <p style={{ color: 'var(--error, #c0392b)', marginTop: 12 }}>{tplFormError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={() => setTplModal(false)}>{t('common.cancel')}</button>
                <button type="submit" className="primary-button" disabled={tplSaving}>
                  {tplSaving ? t('common.saving') : tplMode === 'add' ? t('common.create') : t('common.saveChanges')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
