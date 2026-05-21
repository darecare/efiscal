import React, { useState, useEffect } from 'react'
import { rolesApi, actionsApi, clientsApi } from '../services/api'
import AppShell from '../components/AppShell'
import { useAuth } from '../contexts/AuthContext'

export default function Roles() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'
  const canManageRoles = isSuperAdmin || currentUser?.actions?.includes('ROLES_MANAGE')

  const [roles, setRoles] = useState([])
  const [actions, setActions] = useState([])
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showInactive, setShowInactive] = useState(false)

  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('add')
  const [editRoleId, setEditRoleId] = useState(null)
  const [form, setForm] = useState({
    roleCode: '',
    name: '',
    description: '',
    clientId: '',
    actionIds: [],
    isActive: true,
  })

  useEffect(() => {
    fetchData(showInactive)
  }, [])

  async function fetchData(incInactive = showInactive) {
    try {
      setLoading(true)
      const [rolesData, actionsData, clientsData] = await Promise.all([
        rolesApi.list(incInactive),
        actionsApi.list(),
        isSuperAdmin ? clientsApi.list() : Promise.resolve([]),
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

  const handleToggleShowInactive = (e) => {
    const val = e.target.checked
    setShowInactive(val)
    fetchData(val)
  }

  function openEditModal(role) {
    setForm({
      roleCode: role.roleCode,
      name: role.name,
      description: role.description || '',
      clientId: role.clientId || '',
      actionIds: role.actionIds || [],
      isActive: role.isActive !== false,
    })
    setModalMode('edit')
    setEditRoleId(role.roleId)
    setShowModal(true)
  }

  const handleActionToggle = (actionId) => {
    setForm((prev) => {
      const isSelected = prev.actionIds.includes(actionId)
      if (isSelected) {
        return { ...prev, actionIds: prev.actionIds.filter((id) => id !== actionId) }
      }
      return { ...prev, actionIds: [...prev.actionIds, actionId] }
    })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      setError('')
      if (modalMode === 'add') {
        const payload = {
          roleCode: form.roleCode.trim(),
          name: form.name.trim(),
          description: form.description.trim(),
          clientId: isSuperAdmin 
            ? (form.clientId ? Number(form.clientId) : null)
            : currentUser?.clientId,
          actionIds: form.actionIds,
        }
        await rolesApi.create(payload)
      } else {
        await rolesApi.update(editRoleId, {
          name: form.name.trim(),
          description: form.description.trim(),
          isActive: form.isActive,
          actionIds: form.actionIds,
        })
      }
      setShowModal(false)
      fetchData(showInactive)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save role')
    }
  }

  const groupedActions = actions.reduce((acc, action) => {
    if (!acc[action.moduleCode]) acc[action.moduleCode] = []
    acc[action.moduleCode].push(action)
    return acc
  }, {})

  return (
    <AppShell
      title="Roles & Permissions"
      subtitle="Manage custom roles and assign granular action permissions."
      actions={
        canManageRoles && (
          <button
            className="primary-button"
            onClick={() => {
              setForm({ roleCode: '', name: '', description: '', clientId: '', actionIds: [], isActive: true })
              setModalMode('add')
              setEditRoleId(null)
              setShowModal(true)
            }}
          >
            Create Role
          </button>
        )
      }
    >
      {error && <div className="error-banner">{error}</div>}

      <section className="action-bar card" style={{ display: 'flex', alignItems: 'center' }}>
        <span className="badge">{roles.length} roles</span>
        <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', marginLeft: 'auto' }}>
          <input
            type="checkbox"
            checked={showInactive}
            onChange={handleToggleShowInactive}
          />
          <span style={{ fontSize: '0.9rem' }}>Show Inactive Roles</span>
        </label>
      </section>

      <section className="table-card">
        {loading ? (
          <p className="muted">Loading…</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Description</th>
                <th>Client Scope</th>
                <th>Permissions</th>
                <th>Status</th>
                {canManageRoles && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {roles.map((r) => (
                <tr key={r.roleId}>
                  <td><span className="badge">{r.roleCode}</span></td>
                  <td>{r.name}</td>
                  <td>{r.description}</td>
                  <td>{r.clientId ? clients.find((c) => c.clientId === r.clientId)?.name || r.clientId : 'Global'}</td>
                  <td>
                    <div className="action-tags-container">
                      {r.actionIds && r.actionIds.length > 0 ? (
                        r.actionIds.map(id => {
                          const action = actions.find(a => a.actionId === id);
                          return action ? (
                            <span key={id} className="action-tag" title={action.description}>
                              {action.name}
                            </span>
                          ) : (
                            <span key={id} className="action-tag">
                              {id}
                            </span>
                          );
                        })
                      ) : (
                        <span className="muted" style={{ fontSize: '0.85rem' }}>None</span>
                      )}
                    </div>
                  </td>
                  <td>
                    <span className={`status-chip ${r.isActive !== false ? 'active' : 'inactive'}`}>
                      {r.isActive !== false ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {canManageRoles && (
                    <td>
                      <button className="secondary-button" onClick={() => openEditModal(r)}>
                        Edit
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              {roles.length === 0 && (
                <tr>
                  <td colSpan={canManageRoles ? "7" : "6"} className="muted">No roles found</td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </section>

      {showModal && canManageRoles && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{modalMode === 'add' ? 'Create Custom Role' : 'Edit Custom Role'}</h3>
              <button type="button" className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="field">
                  <label>Role Code (Unique)</label>
                  <input
                    type="text"
                    required
                    disabled={modalMode === 'edit'}
                    value={form.roleCode}
                    onChange={(e) => setForm({ ...form, roleCode: e.target.value })}
                    placeholder="e.g. RESTRICTED_OPERATOR"
                  />
                </div>
                <div className="field">
                  <label>Display Name</label>
                  <input
                    type="text"
                    required
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                  />
                </div>
                <div className="field" style={{ gridColumn: '1 / -1' }}>
                  <label>Description</label>
                  <input
                    type="text"
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                  />
                </div>
                {isSuperAdmin && modalMode === 'add' && (
                  <div className="field">
                    <label>Assign to Client</label>
                    <select
                      value={form.clientId}
                      onChange={(e) => setForm({ ...form, clientId: e.target.value })}
                    >
                      <option value="">Global (All Clients)</option>
                      {clients.map((c) => (
                        <option key={c.clientId} value={c.clientId}>{c.name}</option>
                      ))}
                    </select>
                  </div>
                )}
                {modalMode === 'edit' && (
                  <div className="field">
                    <label>Status</label>
                    <select
                      value={form.isActive ? 'true' : 'false'}
                      onChange={(e) => setForm({ ...form, isActive: e.target.value === 'true' })}
                    >
                      <option value="true">Active</option>
                      <option value="false">Inactive</option>
                    </select>
                  </div>
                )}
                <div className="field" style={{ gridColumn: '1 / -1' }}>
                  <label>Action Permissions</label>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '16px', marginTop: '12px' }}>
                    {Object.entries(groupedActions).map(([module, moduleActions]) => (
                      <div key={module} style={{ background: '#f8f9fa', padding: '12px', borderRadius: '8px', border: '1px solid #eee' }}>
                        <h4 style={{ margin: '0 0 12px 0', fontSize: '0.9rem' }}>{module}</h4>
                        {moduleActions.map((action) => {
                          const userHasPermission = isSuperAdmin || currentUser?.actions?.includes(action.actionCode);
                          return (
                            <label
                              key={action.actionId}
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                marginBottom: '8px',
                                cursor: userHasPermission ? 'pointer' : 'not-allowed',
                                opacity: userHasPermission ? 1 : 0.5,
                              }}
                              title={userHasPermission ? '' : 'You do not possess this permission'}
                            >
                              <input
                                type="checkbox"
                                checked={form.actionIds.includes(action.actionId)}
                                disabled={!userHasPermission}
                                onChange={() => handleActionToggle(action.actionId)}
                              />
                              <span style={{ fontSize: '0.9rem', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                                {action.name} {!userHasPermission && <span title="You do not possess this permission" style={{ cursor: 'help' }}>🔒</span>}
                              </span>
                            </label>
                          )
                        })}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
              <div className="modal-actions">
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
