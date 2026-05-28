import React, { useEffect, useState } from 'react'
import { useTranslation, Trans } from 'react-i18next'
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
  const { t } = useTranslation()
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'
  const canManageUsers = currentUser?.actions?.includes('USERS_MANAGE') || isSuperAdmin
  const canListOrgs = isSuperAdmin || currentUser?.actions?.includes('ORGS_MANAGE')

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

  const [deleteUserModalOpen, setDeleteUserModalOpen] = useState(false)
  const [userToDelete, setUserToDelete] = useState(null)
  const [deletingUser, setDeletingUser] = useState(false)

  useEffect(() => {
    loadAll()
  }, [])

  useEffect(() => {
    if (form.clientId && canListOrgs) {
      orgsApi.list(form.clientId)
        .then(setClientOrgs)
        .catch(() => setClientOrgs([]))
    } else {
      setClientOrgs([])
    }
  }, [form.clientId, canListOrgs])

  useEffect(() => {
    if (successMsg) {
      const timer = setTimeout(() => setSuccessMsg(null), 4000)
      return () => clearTimeout(timer)
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
        canListOrgs ? orgsApi.list().catch(() => []) : Promise.resolve([]),
      ])
      setUsers(usersData)
      setRoles(rolesData)
      setClients(clientsData)
      setAllOrgs(orgsData)
    } catch {
      setError(t('users.loadFailed'))
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
      setFormError(t('users.fullNameRequired'))
      return
    }
    if (modalMode === 'add' && !form.email.trim()) {
      setFormError(t('users.emailRequired'))
      return
    }
    if (modalMode === 'add' && !form.newPassword) {
      setFormError(t('users.passwordRequired'))
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
        setSuccessMsg(t('users.createdSuccess'))
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
        setSuccessMsg(t('users.updatedSuccess'))
      }
      closeModal()
      await loadAll()
    } catch (err) {
      setFormError(err.response?.data?.message || err.response?.data || t('common.operationFailed'))
    } finally {
      setSaving(false)
    }
  }

  function handleDeleteClick(u) {
    setUserToDelete(u)
    setDeleteUserModalOpen(true)
  }

  async function handleConfirmDelete() {
    if (!userToDelete) return
    try {
      setDeletingUser(true)
      setError(null)
      await usersApi.remove(userToDelete.userId)
      setDeleteUserModalOpen(false)
      setUserToDelete(null)
      setSuccessMsg(t('users.deletedSuccess'))
      await loadAll()
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || t('users.deleteFailed'))
    } finally {
      setDeletingUser(false)
    }
  }

  return (
    <AppShell
      title={t('users.title')}
      subtitle={t('users.subtitle')}
      actions={
        canManageUsers && (
          <button className="primary-button" onClick={openAddModal}>
            {t('users.addUser')}
          </button>
        )
      }
    >
      {successMsg && <div className="success-banner">{successMsg}</div>}
      {error && <div className="error-banner">{error}</div>}

      <section className="action-bar card">
        <span className="badge">{t('common.counts.users', { count: users.length })}</span>
      </section>

      <section className="table-card">
        {loading ? (
          <p className="muted">{t('common.loadingDots')}</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>{t('common.name')}</th>
                <th>{t('common.email')}</th>
                <th>{t('common.client')}</th>
                <th>{t('common.role')}</th>
                <th>{t('account.subscription')}</th>
                <th>{t('common.active')}</th>
                {canManageUsers && <th>{t('common.actions')}</th>}
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
                      {u.isActive ? t('common.active') : t('common.inactive')}
                    </span>
                  </td>
                  {canManageUsers && (
                    <td>
                      <div className="table-row-actions">
                        <button type="button" className="secondary-button" onClick={() => openEditModal(u)}>
                          {t('common.edit')}
                        </button>
                        {String(u.userId) !== String(currentUser?.id) ? (
                          <button type="button" className="secondary-button danger" onClick={() => handleDeleteClick(u)}>
                            {t('common.delete')}
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="secondary-button danger"
                            disabled
                            title={t('users.cannotDeleteSelfTitle')}
                          >
                            {t('common.delete')}
                          </button>
                        )}
                      </div>
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
              <h3>{modalMode === 'add' ? t('users.addModalTitle') : t('users.editModalTitle')}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                {modalMode === 'add' && (
                  <div className="field">
                    <label>{t('users.emailLabel')} *</label>
                    <input
                      type="email"
                      value={form.email}
                      onChange={(e) => handleChange('email', e.target.value)}
                      required
                    />
                  </div>
                )}
                <div className="field">
                  <label>{t('users.fullNameLabel')} *</label>
                  <input
                    value={form.fullName}
                    onChange={(e) => handleChange('fullName', e.target.value)}
                    required
                  />
                </div>
                <div className="field">
                  <label>{modalMode === 'add' ? `${t('users.passwordNewLabel')} *` : t('users.passwordEditLabel')}</label>
                  <input
                    type="password"
                    value={form.newPassword}
                    onChange={(e) => handleChange('newPassword', e.target.value)}
                  />
                </div>
                <div className="field">
                  <label>{t('common.role')}</label>
                  <select value={form.roleId} onChange={(e) => handleChange('roleId', e.target.value)}>
                    <option value="">{t('common.selectRolePlaceholder')}</option>
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
                  <label>{t('common.client')}</label>
                  <select
                    value={form.clientId}
                    onChange={(e) => handleChange('clientId', e.target.value)}
                    disabled={!isSuperAdmin}
                  >
                    {isSuperAdmin ? (
                      <>
                        <option value="">{t('common.selectClientPlaceholder')}</option>
                        {clients.map((c) => (
                          <option key={c.clientId} value={c.clientId}>{c.name}</option>
                        ))}
                      </>
                    ) : (
                      <option value={currentUser?.clientId || ''}>{currentUser?.clientName || t('users.myClient')}</option>
                    )}
                  </select>
                </div>
                <div className="field" style={{ gridColumn: '1 / -1' }}>
                  <label>{t('users.organizationAccess')}</label>
                  {clientOrgs.length > 0 ? (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '8px', marginTop: '6px' }}>
                      {clientOrgs.map((org) => (
                        <label key={org.orgId} style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                          <input
                            type="checkbox"
                            checked={form.orgIds.includes(org.orgId)}
                            onChange={() => {
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
                    <p className="muted" style={{ fontSize: '0.85rem', marginTop: '4px' }}>{t('users.noOrgsForClient')}</p>
                  )}
                </div>
                <div className="field">
                  <label>{t('users.subscriptionStatus')}</label>
                  <select value={form.subscriptionStatus} onChange={(e) => handleChange('subscriptionStatus', e.target.value)}>
                    {SUBSCRIPTION_STATUSES.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>{t('users.subscriptionStart')}</label>
                  <input
                    type="date"
                    value={form.subscriptionStartAt}
                    onChange={(e) => handleChange('subscriptionStartAt', e.target.value)}
                  />
                </div>
                <div className="field">
                  <label>{t('users.subscriptionExpires')}</label>
                  <input
                    type="date"
                    value={form.subscriptionExpiresAt}
                    onChange={(e) => handleChange('subscriptionExpiresAt', e.target.value)}
                  />
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
                  {saving ? t('common.saving') : modalMode === 'add' ? t('users.createUser') : t('common.saveChanges')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deleteUserModalOpen && userToDelete && (
        <div className="modal-overlay" onClick={() => !deletingUser && setDeleteUserModalOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{t('users.deleteTitle', { name: userToDelete.fullName })}</h3>
              <button type="button" className="modal-close" onClick={() => setDeleteUserModalOpen(false)} disabled={deletingUser}>✕</button>
            </div>
            <div className="modal-body">
              <p>
                <Trans
                  i18nKey="users.deleteConfirm"
                  values={{ name: userToDelete.fullName, email: userToDelete.email }}
                  components={{ strong: <strong /> }}
                />
              </p>
            </div>
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={() => setDeleteUserModalOpen(false)} disabled={deletingUser}>
                {t('common.cancel')}
              </button>
              <button type="button" className="primary-button danger" onClick={handleConfirmDelete} disabled={deletingUser}>
                {deletingUser ? t('common.deleting') : t('common.confirmDelete')}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  )
}
