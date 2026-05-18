import React, { useState, useEffect } from 'react'
import { rolesApi, actionsApi, clientsApi } from '../services/api'
import AppShell from '../components/AppShell'

export default function Roles() {
  const [roles, setRoles] = useState([])
  const [actions, setActions] = useState([])
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [showModal, setShowModal] = useState(false)
  const [form, setForm] = useState({
    roleCode: '',
    name: '',
    description: '',
    clientId: '',
    actionIds: []
  })

  useEffect(() => {
    fetchData()
  }, [])

  async function fetchData() {
    try {
      setLoading(true)
      const [rolesData, actionsData, clientsData] = await Promise.all([
        rolesApi.list(),
        actionsApi.list(),
        clientsApi.list()
      ])
      setRoles(rolesData)
      setActions(actionsData)
      setClients(clientsData)
    } catch (err) {
      setError('Failed to load data')
    } finally {
      setLoading(false)
    }
  }

  const handleActionToggle = (actionId) => {
    setForm(prev => {
      const isSelected = prev.actionIds.includes(actionId)
      if (isSelected) {
        return { ...prev, actionIds: prev.actionIds.filter(id => id !== actionId) }
      } else {
        return { ...prev, actionIds: [...prev.actionIds, actionId] }
      }
    })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      setError('')
      const payload = {
        ...form,
        clientId: form.clientId ? Number(form.clientId) : null
      }
      await rolesApi.create(payload)
      setShowModal(false)
      fetchData()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save role')
    }
  }

  const groupedActions = actions.reduce((acc, action) => {
    if (!acc[action.moduleCode]) acc[action.moduleCode] = []
    acc[action.moduleCode].push(action)
    return acc
  }, {})

  if (loading) return <div className="loading">Loading roles...</div>

  return (
    <AppShell
      title="Roles & Permissions" 
      subtitle="Manage custom roles and assign granular action permissions."
      actions={
        <button className="primary-button" onClick={() => {
          setForm({ roleCode: '', name: '', description: '', clientId: '', actionIds: [] })
          setShowModal(true)
        }}>
          Create Role
        </button>
      }
    >

      {error && <div className="error-message">{error}</div>}

      <div className="card">
        <div className="table-responsive">
          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Description</th>
                <th>Client Scope</th>
                <th>Permissions Count</th>
              </tr>
            </thead>
            <tbody>
              {roles.map(r => (
                <tr key={r.roleId}>
                  <td><span className="badge">{r.roleCode}</span></td>
                  <td>{r.name}</td>
                  <td>{r.description}</td>
                  <td>{r.clientId ? clients.find(c => c.clientId === r.clientId)?.name || r.clientId : 'Global'}</td>
                  <td>{r.actionIds?.length || 0} actions</td>
                </tr>
              ))}
              {roles.length === 0 && (
                <tr>
                  <td colSpan="5" className="text-center">No roles found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" style={{ maxWidth: '800px' }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Create Custom Role</h3>
              <button type="button" className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <form onSubmit={handleSubmit} style={{ padding: '20px' }} className="form-grid">
              <div className="form-group">
                <label>Role Code (Unique)</label>
                <input
                  type="text"
                  required
                  value={form.roleCode}
                  onChange={e => setForm({...form, roleCode: e.target.value})}
                  placeholder="e.g. RESTRICTED_OPERATOR"
                />
              </div>
              <div className="form-group">
                <label>Display Name</label>
                <input
                  type="text"
                  required
                  value={form.name}
                  onChange={e => setForm({...form, name: e.target.value})}
                />
              </div>
              <div className="form-group full-width">
                <label>Description</label>
                <input
                  type="text"
                  value={form.description}
                  onChange={e => setForm({...form, description: e.target.value})}
                />
              </div>
              <div className="form-group">
                <label>Assign to Client</label>
                <select 
                  value={form.clientId} 
                  onChange={e => setForm({...form, clientId: e.target.value})}
                >
                  <option value="">-- System Wide (No Specific Client) --</option>
                  {clients.map(c => (
                    <option key={c.clientId} value={c.clientId}>{c.name}</option>
                  ))}
                </select>
              </div>

              <div className="form-group full-width">
                <h3>Action Permissions</h3>
                <div className="permissions-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '20px', marginTop: '15px' }}>
                  {Object.entries(groupedActions).map(([module, moduleActions]) => (
                    <div key={module} className="permission-module" style={{ background: '#f8f9fa', padding: '15px', borderRadius: '8px', border: '1px solid #eee' }}>
                      <h4 style={{ margin: '0 0 15px 0', color: '#444', borderBottom: '1px solid #ddd', paddingBottom: '8px' }}>{module}</h4>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {moduleActions.map(action => (
                          <label key={action.actionId} className="checkbox-label" style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
                            <input
                              type="checkbox"
                              checked={form.actionIds.includes(action.actionId)}
                              onChange={() => handleActionToggle(action.actionId)}
                              style={{ width: '16px', height: '16px', cursor: 'pointer' }}
                            />
                            <span style={{ fontSize: '0.9rem', color: '#333' }}>{action.name}</span>
                          </label>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="modal-actions" style={{ gridColumn: '1 / -1' }}>
                <button type="button" className="secondary-button" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="primary-button">
                  Save Role
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
