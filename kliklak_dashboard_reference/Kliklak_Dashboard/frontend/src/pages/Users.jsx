import React, { useState, useEffect } from 'react'
import { useNavigate, Link, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { userService } from '../services/api'
import { IconDashboard, IconOrders, IconAccount, IconUsers, IconProfile, IconLogOut } from '../components/Icons'

const ROLE_OPTIONS = [
  { value: 'user', label: 'Korisnik' },
  { value: 'superuser', label: 'Superkorisnik' },
  { value: 'dobavljac', label: 'Dobavljač' },
]

const ROLE_LABELS = {
  superuser: 'Superkorisnik',
  user: 'Korisnik',
  dobavljac: 'Dobavljač',
}

const emptyForm = { username: '', email: '', password: '', role: 'user', vendor_name: '' }

const Users = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)

  // Modal state
  const [modalOpen, setModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState('add') // 'add' | 'edit'
  const [formData, setFormData] = useState(emptyForm)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [editUserId, setEditUserId] = useState(null)

  // Delete confirmation
  const [deleteModalOpen, setDeleteModalOpen] = useState(false)
  const [userToDelete, setUserToDelete] = useState(null)

  useEffect(() => {
    loadUsers()
  }, [])

  useEffect(() => {
    if (successMsg) {
      const timer = setTimeout(() => setSuccessMsg(null), 4000)
      return () => clearTimeout(timer)
    }
  }, [successMsg])

  const loadUsers = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await userService.getUsers()
      setUsers(data)
    } catch (err) {
      setError(err.response?.data?.detail || 'Neuspešno učitavanje korisnika')
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  // --- Modal handlers ---
  const openAddModal = () => {
    setFormData(emptyForm)
    setFormError(null)
    setModalMode('add')
    setEditUserId(null)
    setModalOpen(true)
  }

  const openEditModal = (usr) => {
    setFormData({
      username: usr.username,
      email: usr.email,
      password: '',
      role: usr.role || 'user',
      vendor_name: usr.vendor_name || '',
    })
    setFormError(null)
    setModalMode('edit')
    setEditUserId(usr.id)
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setFormError(null)
  }

  const handleFormChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setFormError(null)

    if (!formData.username.trim() || !formData.email.trim()) {
      setFormError('Korisničko ime i email su obavezni')
      return
    }
    if (modalMode === 'add' && !formData.password) {
      setFormError('Lozinka je obavezna')
      return
    }
    if (formData.role === 'dobavljac' && !formData.vendor_name.trim()) {
      setFormError('Naziv dobavljača je obavezan za dobavljač korisnike')
      return
    }

    try {
      setSaving(true)

      if (modalMode === 'add') {
        const payload = {
          username: formData.username.trim(),
          email: formData.email.trim(),
          password: formData.password,
          role: formData.role,
          vendor_name: formData.role === 'dobavljac' ? formData.vendor_name.trim() : null,
        }
        await userService.createUser(payload)
        setSuccessMsg('Korisnik uspešno kreiran')
      } else {
        const payload = {}
        if (formData.username.trim()) payload.username = formData.username.trim()
        if (formData.email.trim()) payload.email = formData.email.trim()
        if (formData.password) payload.password = formData.password
        payload.role = formData.role
        payload.vendor_name = formData.role === 'dobavljac' ? formData.vendor_name.trim() : null
        await userService.updateUser(editUserId, payload)
        setSuccessMsg('Korisnik uspešno ažuriran')
      }

      closeModal()
      await loadUsers()
    } catch (err) {
      setFormError(err.response?.data?.detail || 'Operacija nije uspela')
    } finally {
      setSaving(false)
    }
  }

  // --- Delete handlers ---
  const confirmDelete = (usr) => {
    setUserToDelete(usr)
    setDeleteModalOpen(true)
  }

  const handleDelete = async () => {
    if (!userToDelete) return
    try {
      await userService.deleteUser(userToDelete.id)
      setDeleteModalOpen(false)
      setUserToDelete(null)
      setSuccessMsg('Korisnik uspešno obrisan')
      await loadUsers()
    } catch (err) {
      setError(err.response?.data?.detail || 'Brisanje korisnika nije uspelo')
      setDeleteModalOpen(false)
      setUserToDelete(null)
    }
  }

  if (user?.role !== 'superuser') {
    return (
      <div className="error-message">
        Nemate dozvolu da pristupite ovoj stranici.
      </div>
    )
  }

  return (
    <div className="dashboard-layout">
      <nav className="navbar">
        <h1><span className="title-brand">Kliklak</span> <span className="title-product">Dashboard</span></h1>
        <div className="navbar-actions">
          <span className="navbar-welcome">Dobrodošli, {user?.username}</span>
          <Link to="/account" className="account-link" aria-label="Nalog">
            <IconProfile />
          </Link>
          <button onClick={handleLogout} className="btn btn-nav">
            <IconLogOut /> Izlogujte se
          </button>
        </div>
      </nav>

      <div className="main-layout">
        <aside className="sidebar">
          <ul className="sidebar-menu">
            <li>
              <Link
                to="/dashboard"
                className={location.pathname === '/dashboard' ? 'active' : ''}
              >
                <IconDashboard /> Kontrolni panel
              </Link>
            </li>
            <li>
              <Link
                to="/orders"
                className={location.pathname === '/orders' ? 'active' : ''}
              >
                <IconOrders /> Obrada porudžbina
              </Link>
            </li>
            <li>
              <Link
                to="/account"
                className={location.pathname === '/account' ? 'active' : ''}
              >
                <IconAccount /> Nalog
              </Link>
            </li>
            {user?.role === 'superuser' && (
              <li>
                <Link
                  to="/users"
                  className={location.pathname === '/users' ? 'active' : ''}
                >
                  <IconUsers /> Korisnici
                </Link>
              </li>
            )}
          </ul>
        </aside>

        <main className="dashboard-content">
          <div className="users-header">
            <div className="users-header-row">
              <div>
                <h2 className="page-title-with-icon"><IconUsers /> Upravljanje korisnicima</h2>
                <p>Pregledajte i upravljajte svim korisnicima sistema</p>
              </div>
              <button onClick={openAddModal} className="btn btn-primary btn-add-user">
                + Dodajte korisnika
              </button>
            </div>
          </div>

          {successMsg && (
            <div className="success-message">{successMsg}</div>
          )}

          {loading ? (
            <div className="loading">Učitavanje korisnika...</div>
          ) : error ? (
            <div className="error-message">{error}</div>
          ) : (
            <div className="card">
              <div className="table-container">
                <table className="users-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Korisničko ime</th>
                      <th>Email</th>
                      <th>Uloga</th>
                      <th>Dobavljač</th>
                      <th>Aktivan</th>
                      <th>Datum kreiranja</th>
                      <th>Akcije</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((usr) => (
                      <tr key={usr.id}>
                        <td>{usr.id}</td>
                        <td>{usr.username}</td>
                        <td>{usr.email || 'N/A'}</td>
                        <td>
                          <span className={`status-badge role-${usr.role || 'user'}`}>
                            {ROLE_LABELS[usr.role] || usr.role || 'Korisnik'}
                          </span>
                        </td>
                        <td>{usr.vendor_name || '—'}</td>
                        <td>
                          <span className={`status-badge ${usr.is_active ? 'active' : 'inactive'}`}>
                            {usr.is_active ? 'Da' : 'Ne'}
                          </span>
                        </td>
                        <td>{usr.created_at ? new Date(usr.created_at).toLocaleDateString('sr-RS') : 'N/A'}</td>
                        <td className="actions-cell">
                          <button
                            className="btn-icon btn-edit"
                            onClick={() => openEditModal(usr)}
                            title="Izmenite"
                          >
                            ✏️
                          </button>
                          {usr.id !== user.id && (
                            <button
                              className="btn-icon btn-delete"
                              onClick={() => confirmDelete(usr)}
                              title="Obrišite"
                            >
                              🗑️
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {users.length === 0 && (
                  <div className="empty-state">Nema pronađenih korisnika</div>
                )}
              </div>
            </div>
          )}
        </main>
      </div>

      {/* Add / Edit User Modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content modal-user-form" onClick={(e) => e.stopPropagation()}>
            <h2>{modalMode === 'add' ? 'Dodajte korisnika' : 'Izmenite korisnika'}</h2>

            {formError && <div className="error-message">{formError}</div>}

            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="username">Korisničko ime</label>
                <input
                  id="username"
                  type="text"
                  value={formData.username}
                  onChange={(e) => handleFormChange('username', e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="email">Email</label>
                <input
                  id="email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => handleFormChange('email', e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="password">
                  Lozinka {modalMode === 'edit' && <span className="hint">(ostavite prazno da zadržite trenutnu)</span>}
                </label>
                <input
                  id="password"
                  type="password"
                  value={formData.password}
                  onChange={(e) => handleFormChange('password', e.target.value)}
                  {...(modalMode === 'add' ? { required: true } : {})}
                />
              </div>

              <div className="form-group">
                <label htmlFor="role">Uloga</label>
                <select
                  id="role"
                  value={formData.role}
                  onChange={(e) => handleFormChange('role', e.target.value)}
                >
                  {ROLE_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </div>

              {formData.role === 'dobavljac' && (
                <div className="form-group">
                  <label htmlFor="vendor_name">Naziv dobavljača</label>
                  <input
                    id="vendor_name"
                    type="text"
                    value={formData.vendor_name}
                    onChange={(e) => handleFormChange('vendor_name', e.target.value)}
                    placeholder="Unesite tačan naziv dobavljača"
                    required
                  />
                </div>
              )}

              <div className="modal-actions">
                <button type="button" onClick={closeModal} className="btn btn-secondary">
                  Otkažite
                </button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? 'Čuvanje...' : modalMode === 'add' ? 'Kreirajte' : 'Sačuvajte'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteModalOpen && userToDelete && (
        <div className="modal-overlay" onClick={() => setDeleteModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Potvrda brisanja</h2>
            <p>
              Da li ste sigurni da želite da obrišete korisnika <strong>{userToDelete.username}</strong>?
              Ova akcija je nepovratna.
            </p>
            <div className="modal-actions">
              <button onClick={() => setDeleteModalOpen(false)} className="btn btn-secondary">
                Otkažite
              </button>
              <button onClick={handleDelete} className="btn btn-danger">
                Obrišite
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Users
