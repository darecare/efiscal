import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { usersApi, rolesApi, clientsApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const SUBSCRIPTION_STATUSES = ['ACTIVE', 'EXPIRED', 'SUSPENDED']

const emptyForm = {
  email: '',
  fullName: '',
  roleId: '',
  clientId: '',
  subscriptionStatus: 'ACTIVE',
  subscriptionStartAt: '',
  subscriptionExpiresAt: '',
  isActive: true,
  newPassword: '',
  orgIds: [],
}

export default function Users() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'
  const canManageUsers = currentUser?.actions?.includes('USERS_MANAGE') || isSuperAdmin

  const [users, setUsers] = useState([])
  const [roles, setRoles] = useState([])
  const [clients, setClients] = useState([])
  const [allOrgs, setAllOrgs] = useState([])
  const [clientOrgs, setClientOrgs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState('add')
  const [editUserId, setEditUserId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    loadAll()
  }, [])

  useEffect(() => {
    if (form.clientId) {
      orgsApi.list(form.clientId)
        .then(setClientOrgs)
        .catch(() => setClientOrgs([]))
    } else {
      setClientOrgs([])
    }
  }, [form.clientId])

  useEffect(() => {
    if (successMsg) {
      const t = setTimeout(() => setSuccessMsg(null), 4000)
      return () => clearTimeout(t)
    }
  }, [successMsg])

  async function loadAll() {
    try {
      setLoading(true)
      setError(null)
      const [usersData, rolesData, clientsData, orgsData] = await Promise.all([
        usersApi.list(),
        rolesApi.list(),
        isSuperAdmin ? clientsApi.list() : Promise.resolve([]),
        orgsApi.list(),
      ])
      setUsers(usersData)
      setRoles(rolesData)
      setClients(clientsData)
      setAllOrgs(orgsData)
    } catch (err) {
      setError('Failed to load users')
    } finally {
      setLoading(false)
    }
  }

  function openAddModal() {
    setForm({
      ...emptyForm,
      clientId: isSuperAdmin ? '' : (currentUser?.clientId || ''),
    })
    setFormError(null)
    setModalMode('add')
    setEditUserId(null)
    setModalOpen(true)
  }

  function openEditModal(u) {
    setForm({
      email: u.email,
      fullName: u.fullName,
      roleId: u.roleId || '',
      clientId: u.clientId || '',
      subscriptionStatus: u.subscriptionStatus || 'ACTIVE',
      subscriptionStartAt: u.subscriptionStartAt ? u.subscriptionStartAt.slice(0, 10) : '',
      subscriptionExpiresAt: u.subscriptionExpiresAt ? u.subscriptionExpiresAt.slice(0, 10) : '',
      isActive: u.isActive,
      newPassword: '',
      orgIds: u.orgIds || [],
    })
    setFormError(null)
    setModalMode('edit')
    setEditUserId(u.userId)
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
    if (!form.fullName.trim()) {
      setFormError('Full name is required')
      return
    }
    if (modalMode === 'add' && !form.email.trim()) {
      setFormError('Email is required')
      return
    }
    if (modalMode === 'add' && !form.newPassword) {
      setFormError('Password is required for new users')
      return
    }
    try {
      setSaving(true)
      if (modalMode === 'add') {
        await usersApi.create({
          email: form.email.trim(),
          password: form.newPassword,
          fullName: form.fullName.trim(),
          roleId: form.roleId ? Number(form.roleId) : null,
          clientId: form.clientId ? Number(form.clientId) : null,
          subscriptionStatus: form.subscriptionStatus,
          subscriptionStartAt: form.subscriptionStartAt ? new Date(form.subscriptionStartAt).toISOString() : null,
          subscriptionExpiresAt: form.subscriptionExpiresAt ? new Date(form.subscriptionExpiresAt).toISOString() : null,
          orgIds: form.orgIds,
        })
        setSuccessMsg('User created successfully')
      } else {
        await usersApi.update(editUserId, {
          fullName: form.fullName.trim(),
          roleId: form.roleId ? Number(form.roleId) : null,
          clientId: form.clientId ? Number(form.clientId) : null,
          subscriptionStatus: form.subscriptionStatus,
          subscriptionStartAt: form.subscriptionStartAt ? new Date(form.subscriptionStartAt).toISOString() : null,
          subscriptionExpiresAt: form.subscriptionExpiresAt ? new Date(form.subscriptionExpiresAt).toISOString() : null,
          isActive: form.isActive,
          newPassword: form.newPassword || null,
          orgIds: form.orgIds,
        })
        setSuccessMsg('User updated successfully')
      }
      closeModal()
      await loadAll()
    } catch (err) {
      setFormError(err.response?.data?.message || err.response?.data || 'Operation failed')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(u) {
    if (!window.confirm(`Are you sure you want to delete user ${u.fullName}?`)) return
    try {
      await usersApi.remove(u.userId)
      setSuccessMsg('User deleted successfully')
      loadAll()
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Delete failed')
    }
  }

  return (
    <AppShell
      title="Users"
      subtitle="User accounts with role and subscription management."
      actions={
        canManageUsers && (
          <button className="primary-button" onClick={openAddModal}>
            Add User
          </button>
        )
      }
    >
      {successMsg && <div className="success-banner">{successMsg}</div>}
      {error && <div className="error-banner">{error}</div>}

      <section className="action-bar card">
        <span className="badge">{users.length} users</span>
      </section>

      <section className="table-card">
        {loading ? (
          <p className="muted">Loading…</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Client</th>
                <th>Role</th>
                <th>Subscription</th>
                <th>Active</th>
                {canManageUsers && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.userId}>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>
                    <div>{u.clientName}</div>
                    {u.orgIds && u.orgIds.length > 0 && (
                      <div className="muted" style={{ fontSize: '0.75rem', marginTop: '4px', display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                        {u.orgIds.map(oid => {
                          const o = allOrgs.find(org => org.orgId === oid)
                          return o ? <span key={oid} className="action-tag" style={{ fontSize: '0.7rem', padding: '2px 4px' }}>{o.name}</span> : null
                        })}
                      </div>
                    )}
                  </td>
                  <td>{u.roleName}</td>
                  <td>
                    <span className={`status-chip ${(u.subscriptionStatus || '').toLowerCase()}`}>
                      {u.subscriptionStatus}
                    </span>
                  </td>
                  <td>
                    <span className={`status-chip ${u.isActive ? 'active' : 'inactive'}`}>
                      {u.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {canManageUsers && (
                    <td style={{ display: 'flex', gap: '8px' }}>
                      <button className="secondary-button" onClick={() => openEditModal(u)}>
                        Edit
                      </button>
                      {String(u.userId) !== String(currentUser?.id) ? (
                        <button className="secondary-button" style={{ color: 'var(--danger-color, red)' }} onClick={() => handleDelete(u)}>
                          Delete
                        </button>
                      ) : (
                        <button className="secondary-button" disabled title="Cannot delete your own account" style={{ opacity: 0.5, cursor: 'not-allowed' }}>
                          Delete
                        </button>
                      )}
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
              <h3>{modalMode === 'add' ? 'Add User' : 'Edit User'}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                {modalMode === 'add' && (
                  <div className="field">
                    <label>Email *</label>
                    <input
                      type="email"
                      value={form.email}
                      onChange={(e) => handleChange('email', e.target.value)}
                      required
                    />
                  </div>
                )}
                <div className="field">
                  <label>Full Name *</label>
                  <input
                    value={form.fullName}
                    onChange={(e) => handleChange('fullName', e.target.value)}
                    required
                  />
                </div>
                <div className="field">
                  <label>{modalMode === 'add' ? 'Password *' : 'New Password (leave blank to keep)'}</label>
                  <input
                    type="password"
                    value={form.newPassword}
                    onChange={(e) => handleChange('newPassword', e.target.value)}
                  />
                </div>
                <div className="field">
                  <label>Role</label>
                  <select value={form.roleId} onChange={(e) => handleChange('roleId', e.target.value)}>
                    <option value="">— Select role —</option>
                    {roles
                      .filter(r => {
                        if (r.roleCode === 'SUPERADMIN' && !isSuperAdmin) {
                          return false
                        }
                        return !r.clientId || (form.clientId && r.clientId === Number(form.clientId))
                      })
                      .map((r) => (
                      <option key={r.roleId} value={r.roleId}>{r.name}</option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>Client</label>
                  <select
                    value={form.clientId}
                    onChange={(e) => handleChange('clientId', e.target.value)}
                    disabled={!isSuperAdmin}
                  >
                    {isSuperAdmin ? (
                      <>
                        <option value="">— Select client —</option>
                        {clients.map((c) => (
                          <option key={c.clientId} value={c.clientId}>{c.name}</option>
                        ))}
                      </>
                    ) : (
                      <option value={currentUser?.clientId || ''}>{currentUser?.clientName || 'My Client'}</option>
                    )}
                  </select>
                </div>
                <div className="field" style={{ gridColumn: '1 / -1' }}>
                  <label>Organization Access</label>
                  {clientOrgs.length > 0 ? (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '8px', marginTop: '6px' }}>
                      {clientOrgs.map((org) => (
                        <label key={org.orgId} style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                          <input
                            type="checkbox"
                            checked={form.orgIds.includes(org.orgId)}
                            onChange={(e) => {
                              const val = org.orgId
                              setForm(prev => {
                                const isChecked = prev.orgIds.includes(val)
                                return {
                                  ...prev,
                                  orgIds: isChecked
                                    ? prev.orgIds.filter(id => id !== val)
                                    : [...prev.orgIds, val]
                                }
                              })
                            }}
                          />
                          <span style={{ fontSize: '0.9rem' }}>{org.name}</span>
                        </label>
                      ))}
                    </div>
                  ) : (
                    <p className="muted" style={{ fontSize: '0.85rem', marginTop: '4px' }}>No organizations found for this client.</p>
                  )}
                </div>
                <div className="field">
                  <label>Subscription Status</label>
                  <select value={form.subscriptionStatus} onChange={(e) => handleChange('subscriptionStatus', e.target.value)}>
                    {SUBSCRIPTION_STATUSES.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>Subscription Start</label>
                  <input
                    type="date"
                    value={form.subscriptionStartAt}
                    onChange={(e) => handleChange('subscriptionStartAt', e.target.value)}
                  />
                </div>
                <div className="field">
                  <label>Subscription Expires</label>
                  <input
                    type="date"
                    value={form.subscriptionExpiresAt}
                    onChange={(e) => handleChange('subscriptionExpiresAt', e.target.value)}
                  />
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
                  {saving ? 'Saving…' : modalMode === 'add' ? 'Create User' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
