import React, { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { rolesApi, actionsApi, clientsApi } from '../services/api'
import AppShell from '../components/AppShell'
import { useAuth } from '../contexts/AuthContext'

const IMMUTABLE_ROLE_CODES = ['SUPERADMIN', 'CLIENT_ADMIN', 'OPERATOR']

export default function Roles() {
  const { t } = useTranslation()
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

  const [deleteRoleModalOpen, setDeleteRoleModalOpen] = useState(false)
  const [roleToDelete, setRoleToDelete] = useState(null)
  const [reassignToRoleId, setReassignToRoleId] = useState('')
  const [deletingRole, setDeletingRole] = useState(false)
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
    } catch {
      setError(t('roles.loadFailed'))
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
      setError(err.response?.data?.message || t('roles.saveFailed'))
    }
  }

  function handleDeleteClick(role) {
    if (IMMUTABLE_ROLE_CODES.includes(role.roleCode)) {
      setError(t('roles.cannotDeleteBuiltIn', { code: role.roleCode }))
      return
    }
    if (!role.clientId && !isSuperAdmin) {
      setError(t('roles.onlySuperAdminDeleteGlobal'))
      return
    }
    setRoleToDelete(role)
    setReassignToRoleId('')
    setDeleteRoleModalOpen(true)
  }

  async function handleConfirmDelete() {
    try {
      setDeletingRole(true)
      setError('')
      await rolesApi.remove(roleToDelete.roleId, reassignToRoleId ? Number(reassignToRoleId) : undefined)
      setDeleteRoleModalOpen(false)
      fetchData(showInactive)
    } catch (err) {
      setError(err.response?.data?.message || t('roles.deleteFailed'))
    } finally {
      setDeletingRole(false)
    }
  }

  const groupedActions = actions.reduce((acc, action) => {
    if (!acc[action.moduleCode]) acc[action.moduleCode] = []
    acc[action.moduleCode].push(action)
    return acc
  }, {})

  return (
    <AppShell
      title={t('roles.title')}
      subtitle={t('roles.subtitle')}
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
            {t('roles.createRole')}
          </button>
        )
      }
    >
      {error && <div className="error-banner">{error}</div>}

      <section className="action-bar card" style={{ display: 'flex', alignItems: 'center' }}>
        <span className="badge">{t('common.counts.roles', { count: roles.length })}</span>
        <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', marginLeft: 'auto' }}>
          <input
            type="checkbox"
            checked={showInactive}
            onChange={handleToggleShowInactive}
          />
          <span style={{ fontSize: '0.9rem' }}>{t('roles.showInactive')}</span>
        </label>
      </section>

      <section className="table-card">
        {loading ? (
          <p className="muted">{t('common.loadingDots')}</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>{t('roles.code')}</th>
                <th>{t('common.name')}</th>
                <th>{t('common.description')}</th>
                <th>{t('roles.clientScope')}</th>
                <th>{t('roles.permissions')}</th>
                <th>{t('common.status')}</th>
                {canManageRoles && <th>{t('common.actions')}</th>}
              </tr>
            </thead>
            <tbody>
              {roles.map((r) => (
                <tr key={r.roleId}>
                  <td><span className="badge">{r.roleCode}</span></td>
                  <td>{r.name}</td>
                  <td>{r.description}</td>
                  <td>{r.clientId ? clients.find((c) => c.clientId === r.clientId)?.name || r.clientId : t('common.global')}</td>
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
                        <span className="muted" style={{ fontSize: '0.85rem' }}>{t('common.none')}</span>
                      )}
                    </div>
                  </td>
                  <td>
                    <span className={`status-chip ${r.isActive !== false ? 'active' : 'inactive'}`}>
                      {r.isActive !== false ? t('common.active') : t('common.inactive')}
                    </span>
                  </td>
                  {canManageRoles && (
                    <td>
                      <div className="table-row-actions">
                        <button type="button" className="secondary-button" onClick={() => openEditModal(r)}>
                          {t('common.edit')}
                        </button>
                        <button type="button" className="secondary-button danger" onClick={() => handleDeleteClick(r)}>
                          {t('common.delete')}
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
              {roles.length === 0 && (
                <tr>
                  <td colSpan={canManageRoles ? "7" : "6"} className="muted">{t('roles.noRoles')}</td>
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
              <h3>{modalMode === 'add' ? t('roles.addModalTitle') : t('roles.editModalTitle')}</h3>
              <button type="button" className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="field">
                  <label>{t('roles.roleCodeLabel')}</label>
                  <input
                    type="text"
                    required
                    disabled={modalMode === 'edit'}
                    value={form.roleCode}
                    onChange={(e) => setForm({ ...form, roleCode: e.target.value })}
                    placeholder={t('roles.roleCodePlaceholder')}
                  />
                </div>
                <div className="field">
                  <label>{t('roles.displayName')}</label>
                  <input
                    type="text"
                    required
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                  />
                </div>
                <div className="field" style={{ gridColumn: '1 / -1' }}>
                  <label>{t('common.description')}</label>
                  <input
                    type="text"
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                  />
                </div>
                {isSuperAdmin && modalMode === 'add' && (
                  <div className="field">
                    <label>{t('roles.assignToClient')}</label>
                    <select
                      value={form.clientId}
                      onChange={(e) => setForm({ ...form, clientId: e.target.value })}
                    >
                      <option value="">{t('roles.globalAllClients')}</option>
                      {clients.map((c) => (
                        <option key={c.clientId} value={c.clientId}>{c.name}</option>
                      ))}
                    </select>
                  </div>
                )}
                {modalMode === 'edit' && (
                  <div className="field">
                    <label>{t('common.status')}</label>
                    <select
                      value={form.isActive ? 'true' : 'false'}
                      onChange={(e) => setForm({ ...form, isActive: e.target.value === 'true' })}
                    >
                      <option value="true">{t('common.active')}</option>
                      <option value="false">{t('common.inactive')}</option>
                    </select>
                  </div>
                )}
                <div className="field" style={{ gridColumn: '1 / -1' }}>
                  <label>{t('roles.actionPermissions')}</label>
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
                              title={userHasPermission ? '' : t('roles.noPermissionHint')}
                            >
                              <input
                                type="checkbox"
                                checked={form.actionIds.includes(action.actionId)}
                                disabled={!userHasPermission}
                                onChange={() => handleActionToggle(action.actionId)}
                              />
                              <span style={{ fontSize: '0.9rem', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                                {action.name} {!userHasPermission && <span title={t('roles.noPermissionHint')} style={{ cursor: 'help' }}>🔒</span>}
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
                  {t('common.cancel')}
                </button>
                <button type="submit" className="primary-button">
                  {t('roles.saveRole')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deleteRoleModalOpen && roleToDelete && (
        <div className="modal-overlay" onClick={() => !deletingRole && setDeleteRoleModalOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{t('roles.deleteTitle', { name: roleToDelete.name })}</h3>
              <button type="button" className="modal-close" onClick={() => setDeleteRoleModalOpen(false)} disabled={deletingRole}>✕</button>
            </div>
            <div className="modal-body">
              <p>{t('roles.deleteConfirm')}</p>
              <div className="field" style={{ marginTop: '16px' }}>
                <label>{t('roles.reassignLabel')}:</label>
                <select value={reassignToRoleId} onChange={(e) => setReassignToRoleId(e.target.value)}>
                  <option value="">{t('roles.skipReassign')}</option>
                  {roles
                    .filter(r =>
                      r.roleId !== roleToDelete.roleId
                      && r.isActive !== false
                      && (r.clientId === roleToDelete.clientId || !r.clientId)
                      && (roleToDelete.clientId != null || r.clientId == null)
                    )
                    .map(r => (
                      <option key={r.roleId} value={r.roleId}>{r.name}</option>
                  ))}
                </select>
                <p className="muted" style={{ fontSize: '0.8rem', marginTop: '4px' }}>
                  {t('roles.reassignHint')}
                </p>
              </div>
            </div>
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={() => setDeleteRoleModalOpen(false)} disabled={deletingRole}>{t('common.cancel')}</button>
              <button type="button" className="primary-button danger" onClick={handleConfirmDelete} disabled={deletingRole}>
                {deletingRole ? t('common.deleting') : t('common.confirmDelete')}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  )
}
