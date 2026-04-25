import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { apiConnApi, apiTemplateApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const PLATFORM_OPTIONS = ['MERCHANTPRO', 'WOOCOMMERCE', 'SHOPIFY', 'OTHER']
const AUTH_OPTIONS = ['BASIC_AUTH', 'OAUTH', 'MTLS', 'NONE']
const METHOD_OPTIONS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE']

const emptyConnForm = {
  orgId: '', displayName: '', apiPlatform: 'MERCHANTPRO',
  apiBaseUrl: '', apiauthtype: 'BASIC_AUTH', apikey: '', apisecret: '', isActive: true,
}
const emptyTplForm = {
  operationKey: '', httpMethod: 'GET', contentType: 'application/json', endpointPath: '', isActive: true,
}

export default function ApiConfig() {
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
      setError('Failed to load connections')
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
      apikey: '', apisecret: '', isActive: c.isActive,
    })
    setConnFormError(null)
    setConnMode('edit')
    setEditConnId(c.apiconnId)
    setConnModal(true)
  }

  async function handleConnSubmit(e) {
    e.preventDefault()
    setConnFormError(null)
    if (!connForm.displayName.trim()) { setConnFormError('Display name is required'); return }
    if (!connForm.orgId) { setConnFormError('Organization is required'); return }
    try {
      setConnSaving(true)
      const payload = {
        ...connForm,
        orgId: Number(connForm.orgId),
        apikey: connForm.apikey.trim() || null,
        apisecret: connForm.apisecret.trim() || null,
      }
      if (connMode === 'add') {
        await apiConnApi.create(payload)
        setSuccessMsg('Connection created')
      } else {
        await apiConnApi.update(editConnId, payload)
        setSuccessMsg('Connection updated')
      }
      setConnModal(false)
      const updated = await apiConnApi.list()
      setConnections(updated)
      if (connMode === 'edit' && selectedConn?.apiconnId === editConnId) {
        const found = updated.find(x => x.apiconnId === editConnId)
        if (found) setSelectedConn(found)
      }
    } catch (err) {
      setConnFormError(err.response?.data?.message || err.response?.data || 'Operation failed')
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
    if (!tplForm.operationKey.trim()) { setTplFormError('Operation key is required'); return }
    if (!tplForm.endpointPath.trim()) { setTplFormError('Endpoint path is required'); return }
    try {
      setTplSaving(true)
      if (tplMode === 'add') {
        await apiTemplateApi.create({ ...tplForm, apiconnId: selectedConn.apiconnId })
        setSuccessMsg('Template created')
      } else {
        await apiTemplateApi.update(editTplId, { ...tplForm, apiconnId: selectedConn.apiconnId })
        setSuccessMsg('Template updated')
      }
      setTplModal(false)
      setTemplates(await apiTemplateApi.list(selectedConn.apiconnId))
    } catch (err) {
      setTplFormError(err.response?.data?.message || err.response?.data || 'Operation failed')
    } finally {
      setTplSaving(false)
    }
  }

  return (
    <AppShell
      title="API Configuration"
      subtitle="Platform connections and operation templates."
      actions={isSuperAdmin ? <button className="primary-button" onClick={openAddConn}>Add Connection</button> : null}
    >
      {successMsg && <div className="success-banner">{successMsg}</div>}
      {error && <div className="error-banner">{error}</div>}

      {/* ── Master: Connections ── */}
      <section className="table-card" style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0 }}>API Connections</h3>
          <span style={{ fontSize: 13, opacity: 0.6 }}>{connections.length} record{connections.length !== 1 ? 's' : ''}</span>
        </div>
        {loadingConn ? <p style={{ opacity: 0.5 }}>Loading…</p> : (
          <table>
            <thead>
              <tr>
                <th style={{ width: 24 }}></th>
                <th>Name</th>
                <th>Platform</th>
                <th>Organization</th>
                <th>Base URL</th>
                <th>Auth Type</th>
                <th>Status</th>
                {isSuperAdmin && <th>Actions</th>}
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
                  <td>{c.apiPlatform}</td>
                  <td>{c.orgName}</td>
                  <td style={{ fontFamily: 'monospace', fontSize: 13 }}>{c.apiBaseUrl}</td>
                  <td>{c.apiauthtype}</td>
                  <td>
                    <span className={`status-chip ${c.isActive ? 'active' : 'inactive'}`}>
                      {c.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {isSuperAdmin && (
                    <td onClick={ev => ev.stopPropagation()}>
                      <button className="secondary-button" onClick={() => openEditConn(c)}>Edit</button>
                    </td>
                  )}
                </tr>
              ))}
              {connections.length === 0 && (
                <tr>
                  <td colSpan={isSuperAdmin ? 8 : 7} style={{ textAlign: 'center', opacity: 0.45, padding: '24px 0' }}>
                    No connections configured yet
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
                Templates —{' '}
                <span style={{ color: '#c2753c' }}>{selectedConn.displayName}</span>
              </h3>
              <p style={{ margin: '4px 0 0', opacity: 0.55, fontSize: 13 }}>
                Operation templates for this connection
              </p>
            </div>
            {isSuperAdmin && (
              <button className="primary-button" onClick={openAddTpl}>Add Template</button>
            )}
          </div>
          {loadingTpl ? <p style={{ opacity: 0.5 }}>Loading…</p> : (
            <table>
              <thead>
                <tr>
                  <th>Operation Key</th>
                  <th>Method</th>
                  <th>Content Type</th>
                  <th>Endpoint Path</th>
                  <th>Status</th>
                  {isSuperAdmin && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {templates.map((t) => (
                  <tr key={t.apitemplateId}>
                    <td><code style={{ fontSize: 13 }}>{t.operationKey}</code></td>
                    <td>
                      <span style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: 12,
                        background: 'rgba(194,117,60,0.15)', padding: '2px 6px', borderRadius: 4 }}>
                        {t.httpMethod}
                      </span>
                    </td>
                    <td style={{ fontSize: 13 }}>{t.contentType}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: 13 }}>{t.endpointPath}</td>
                    <td>
                      <span className={`status-chip ${t.isActive ? 'active' : 'inactive'}`}>
                        {t.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    {isSuperAdmin && (
                      <td>
                        <button className="secondary-button" onClick={() => openEditTpl(t)}>Edit</button>
                      </td>
                    )}
                  </tr>
                ))}
                {templates.length === 0 && (
                  <tr>
                    <td colSpan={isSuperAdmin ? 6 : 5} style={{ textAlign: 'center', opacity: 0.45, padding: '24px 0' }}>
                      No templates for this connection
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
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{connMode === 'add' ? 'Add Connection' : 'Edit Connection'}</h3>
              <button className="modal-close" onClick={() => setConnModal(false)}>✕</button>
            </div>
            <form onSubmit={handleConnSubmit}>
              <div className="form-grid">
                <div className="field">
                  <label>Display Name *</label>
                  <input value={connForm.displayName} onChange={e => setConnForm(p => ({ ...p, displayName: e.target.value }))} required />
                </div>
                <div className="field">
                  <label>Organization *</label>
                  <select value={connForm.orgId} onChange={e => setConnForm(p => ({ ...p, orgId: e.target.value }))}>
                    <option value="">— Select org —</option>
                    {orgs.map(o => <option key={o.orgId} value={o.orgId}>{o.name}</option>)}
                  </select>
                </div>
                <div className="field">
                  <label>Platform *</label>
                  <select value={connForm.apiPlatform} onChange={e => setConnForm(p => ({ ...p, apiPlatform: e.target.value }))}>
                    {PLATFORM_OPTIONS.map(x => <option key={x} value={x}>{x}</option>)}
                  </select>
                </div>
                <div className="field">
                  <label>Base URL</label>
                  <input value={connForm.apiBaseUrl} onChange={e => setConnForm(p => ({ ...p, apiBaseUrl: e.target.value }))} />
                </div>
                <div className="field">
                  <label>Auth Type</label>
                  <select value={connForm.apiauthtype} onChange={e => setConnForm(p => ({ ...p, apiauthtype: e.target.value }))}>
                    {AUTH_OPTIONS.map(x => <option key={x} value={x}>{x}</option>)}
                  </select>
                </div>
                <div className="field">
                  <label>API Key</label>
                  <input value={connForm.apikey} onChange={e => setConnForm(p => ({ ...p, apikey: e.target.value }))} />
                </div>
                <div className="field">
                  <label>API Secret</label>
                  <input type="password" value={connForm.apisecret} onChange={e => setConnForm(p => ({ ...p, apisecret: e.target.value }))} />
                </div>
                {connMode === 'edit' && (
                  <div className="field">
                    <label>Status</label>
                    <select value={connForm.isActive ? 'true' : 'false'} onChange={e => setConnForm(p => ({ ...p, isActive: e.target.value === 'true' }))}>
                      <option value="true">Active</option>
                      <option value="false">Inactive</option>
                    </select>
                  </div>
                )}
              </div>
              {connFormError && <p style={{ color: 'var(--error, #c0392b)', marginTop: 12 }}>{connFormError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={() => setConnModal(false)}>Cancel</button>
                <button type="submit" className="primary-button" disabled={connSaving}>
                  {connSaving ? 'Saving…' : connMode === 'add' ? 'Create' : 'Save Changes'}
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
              <h3>{tplMode === 'add' ? 'Add Template' : 'Edit Template'}</h3>
              <button className="modal-close" onClick={() => setTplModal(false)}>✕</button>
            </div>
            <form onSubmit={handleTplSubmit}>
              <div className="form-grid">
                <div className="field">
                  <label>Operation Key *</label>
                  <input
                    value={tplForm.operationKey}
                    onChange={e => setTplForm(p => ({ ...p, operationKey: e.target.value }))}
                    placeholder="FETCH_ORDERS"
                    required
                  />
                </div>
                <div className="field">
                  <label>HTTP Method *</label>
                  <select value={tplForm.httpMethod} onChange={e => setTplForm(p => ({ ...p, httpMethod: e.target.value }))}>
                    {METHOD_OPTIONS.map(x => <option key={x} value={x}>{x}</option>)}
                  </select>
                </div>
                <div className="field">
                  <label>Content Type</label>
                  <input value={tplForm.contentType} onChange={e => setTplForm(p => ({ ...p, contentType: e.target.value }))} />
                </div>
                <div className="field">
                  <label>Endpoint Path *</label>
                  <input
                    value={tplForm.endpointPath}
                    onChange={e => setTplForm(p => ({ ...p, endpointPath: e.target.value }))}
                    placeholder="/v1/orders"
                    required
                  />
                </div>
                {tplMode === 'edit' && (
                  <div className="field">
                    <label>Status</label>
                    <select value={tplForm.isActive ? 'true' : 'false'} onChange={e => setTplForm(p => ({ ...p, isActive: e.target.value === 'true' }))}>
                      <option value="true">Active</option>
                      <option value="false">Inactive</option>
                    </select>
                  </div>
                )}
              </div>
              {tplFormError && <p style={{ color: 'var(--error, #c0392b)', marginTop: 12 }}>{tplFormError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={() => setTplModal(false)}>Cancel</button>
                <button type="submit" className="primary-button" disabled={tplSaving}>
                  {tplSaving ? 'Saving…' : tplMode === 'add' ? 'Create' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
