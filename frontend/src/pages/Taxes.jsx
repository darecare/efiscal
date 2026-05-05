import React, { useEffect, useMemo, useState } from 'react'
import AppShell from '../components/AppShell'
import { taxApi, taxCategoryApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const emptyTaxForm = {
  taxCategoryId: '',
  label: '',
  rate: '',
  isActive: true,
  efiscalTaxname: '',
  efiscalAdvanceprefix: '',
  efiscalAdvancename: '',
}

const emptyCategoryForm = {
  name: '',
  taxcategoryCode: '',
  isActive: true,
}

export default function Taxes() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [activeTab, setActiveTab] = useState('taxes')
  const [taxes, setTaxes] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)

  const [taxModal, setTaxModal] = useState(false)
  const [taxMode, setTaxMode] = useState('add')
  const [editTaxId, setEditTaxId] = useState(null)
  const [taxForm, setTaxForm] = useState(emptyTaxForm)
  const [taxFormError, setTaxFormError] = useState(null)
  const [savingTax, setSavingTax] = useState(false)

  const [categoryModal, setCategoryModal] = useState(false)
  const [categoryMode, setCategoryMode] = useState('add')
  const [editCategoryId, setEditCategoryId] = useState(null)
  const [categoryForm, setCategoryForm] = useState(emptyCategoryForm)
  const [categoryFormError, setCategoryFormError] = useState(null)
  const [savingCategory, setSavingCategory] = useState(false)

  useEffect(() => {
    loadAll()
  }, [])

  useEffect(() => {
    if (!success) return
    const t = setTimeout(() => setSuccess(null), 3500)
    return () => clearTimeout(t)
  }, [success])

  const categoryMap = useMemo(() => {
    const m = new Map()
    categories.forEach((c) => m.set(c.taxCategoryId, c))
    return m
  }, [categories])

  async function loadAll() {
    setLoading(true)
    setError(null)
    try {
      const [taxData, categoryData] = await Promise.all([taxApi.list(), taxCategoryApi.list()])
      setTaxes(taxData)
      setCategories(categoryData)
    } catch (err) {
      setError(err?.response?.data?.message || err?.response?.data || 'Failed to load taxes and categories')
    } finally {
      setLoading(false)
    }
  }

  function openAddTax() {
    setTaxMode('add')
    setEditTaxId(null)
    setTaxForm(emptyTaxForm)
    setTaxFormError(null)
    setTaxModal(true)
  }

  function openEditTax(tax) {
    setTaxMode('edit')
    setEditTaxId(tax.taxId)
    setTaxForm({
      taxCategoryId: String(tax.taxCategoryId),
      label: tax.label,
      rate: String(tax.rate),
      isActive: tax.isActive,
      efiscalTaxname: tax.efiscalTaxname ?? '',
      efiscalAdvanceprefix: tax.efiscalAdvanceprefix ?? '',
      efiscalAdvancename: tax.efiscalAdvancename ?? '',
    })
    setTaxFormError(null)
    setTaxModal(true)
  }

  async function submitTax(e) {
    e.preventDefault()
    setTaxFormError(null)

    if (!taxForm.taxCategoryId) {
      setTaxFormError('Tax category is required')
      return
    }
    if (!taxForm.label.trim()) {
      setTaxFormError('Label is required')
      return
    }
    if (taxForm.rate === '' || Number.isNaN(Number(taxForm.rate))) {
      setTaxFormError('Rate must be a valid number')
      return
    }

    try {
      setSavingTax(true)
      const payload = {
        taxCategoryId: Number(taxForm.taxCategoryId),
        label: taxForm.label.trim().toUpperCase(),
        rate: Number(taxForm.rate),
        isActive: taxForm.isActive,
        efiscalTaxname: taxForm.efiscalTaxname.trim() || null,
        efiscalAdvanceprefix: taxForm.efiscalAdvanceprefix.trim() || null,
        efiscalAdvancename: taxForm.efiscalAdvancename.trim() || null,
      }
      if (taxMode === 'add') {
        await taxApi.create(payload)
        setSuccess('Tax created')
      } else {
        await taxApi.update(editTaxId, payload)
        setSuccess('Tax updated')
      }
      setTaxModal(false)
      setTaxes(await taxApi.list())
    } catch (err) {
      setTaxFormError(err?.response?.data?.message || err?.response?.data || 'Operation failed')
    } finally {
      setSavingTax(false)
    }
  }

  function openAddCategory() {
    setCategoryMode('add')
    setEditCategoryId(null)
    setCategoryForm(emptyCategoryForm)
    setCategoryFormError(null)
    setCategoryModal(true)
  }

  function openEditCategory(category) {
    setCategoryMode('edit')
    setEditCategoryId(category.taxCategoryId)
    setCategoryForm({
      name: category.name,
      taxcategoryCode: category.taxcategoryCode ?? '',
      isActive: category.isActive,
    })
    setCategoryFormError(null)
    setCategoryModal(true)
  }

  async function submitCategory(e) {
    e.preventDefault()
    setCategoryFormError(null)

    if (!categoryForm.name.trim()) {
      setCategoryFormError('Category name is required')
      return
    }

    try {
      setSavingCategory(true)
      const payload = {
        name: categoryForm.name.trim(),
        taxcategoryCode: categoryForm.taxcategoryCode.trim() || null,
        isActive: categoryForm.isActive,
      }
      if (categoryMode === 'add') {
        await taxCategoryApi.create(payload)
        setSuccess('Tax category created')
      } else {
        await taxCategoryApi.update(editCategoryId, payload)
        setSuccess('Tax category updated')
      }
      setCategoryModal(false)
      const updatedCategories = await taxCategoryApi.list()
      setCategories(updatedCategories)
      setTaxes(await taxApi.list())
    } catch (err) {
      setCategoryFormError(err?.response?.data?.message || err?.response?.data || 'Operation failed')
    } finally {
      setSavingCategory(false)
    }
  }

  return (
    <AppShell
      title="Taxes"
      subtitle="Manage tax rates and tax categories"
      actions={
        isSuperAdmin ? (
          activeTab === 'taxes'
            ? <button className="primary-button" onClick={openAddTax}>Add Tax</button>
            : <button className="primary-button" onClick={openAddCategory}>Add Tax Category</button>
        ) : null
      }
    >
      {success ? <div className="success-banner">{success}</div> : null}
      {error ? <div className="error-banner">{error}</div> : null}

      <section className="table-card" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            className={activeTab === 'taxes' ? 'primary-button' : 'secondary-button'}
            onClick={() => setActiveTab('taxes')}
            type="button"
          >
            Tax Rates
          </button>
          <button
            className={activeTab === 'categories' ? 'primary-button' : 'secondary-button'}
            onClick={() => setActiveTab('categories')}
            type="button"
          >
            Tax Categories
          </button>
        </div>
      </section>

      {loading ? <p style={{ opacity: 0.6 }}>Loading...</p> : null}

      {!loading && activeTab === 'taxes' && (
        <section className="table-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h3 style={{ margin: 0 }}>Tax Rates</h3>
            <span style={{ fontSize: 13, opacity: 0.6 }}>{taxes.length} record{taxes.length !== 1 ? 's' : ''}</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>Label</th>
                <th>Rate (%)</th>
                <th>Category</th>
                <th>Tax Name</th>
                <th>Advance Prefix</th>
                <th>Advance Name</th>
                <th>Status</th>
                {isSuperAdmin ? <th>Actions</th> : null}
              </tr>
            </thead>
            <tbody>
              {taxes.map((tax) => (
                <tr key={tax.taxId}>
                  <td>{tax.label}</td>
                  <td>{tax.rate}</td>
                  <td>{tax.taxCategoryName || categoryMap.get(tax.taxCategoryId)?.name || '-'}</td>
                  <td>{tax.efiscalTaxname || '-'}</td>
                  <td>{tax.efiscalAdvanceprefix || '-'}</td>
                  <td>{tax.efiscalAdvancename || '-'}</td>
                  <td>
                    <span className={`status-chip ${tax.isActive ? 'active' : 'inactive'}`}>
                      {tax.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {isSuperAdmin ? (
                    <td>
                      <button className="secondary-button" onClick={() => openEditTax(tax)}>Edit</button>
                    </td>
                  ) : null}
                </tr>
              ))}
              {taxes.length === 0 ? (
                <tr>
                  <td colSpan={isSuperAdmin ? 8 : 7} style={{ textAlign: 'center', opacity: 0.5, padding: '24px 0' }}>
                    No tax rates found
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </section>
      )}

      {!loading && activeTab === 'categories' && (
        <section className="table-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h3 style={{ margin: 0 }}>Tax Categories</h3>
            <span style={{ fontSize: 13, opacity: 0.6 }}>{categories.length} record{categories.length !== 1 ? 's' : ''}</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Code</th>
                <th>Status</th>
                {isSuperAdmin ? <th>Actions</th> : null}
              </tr>
            </thead>
            <tbody>
              {categories.map((category) => (
                <tr key={category.taxCategoryId}>
                  <td>{category.name}</td>
                  <td>{category.taxcategoryCode ?? '-'}</td>
                  <td>
                    <span className={`status-chip ${category.isActive ? 'active' : 'inactive'}`}>
                      {category.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {isSuperAdmin ? (
                    <td>
                      <button className="secondary-button" onClick={() => openEditCategory(category)}>Edit</button>
                    </td>
                  ) : null}
                </tr>
              ))}
              {categories.length === 0 ? (
                <tr>
                  <td colSpan={isSuperAdmin ? 4 : 3} style={{ textAlign: 'center', opacity: 0.5, padding: '24px 0' }}>
                    No tax categories found
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </section>
      )}

      {taxModal && (
        <div className="modal-overlay" onClick={() => setTaxModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{taxMode === 'add' ? 'Add Tax' : 'Edit Tax'}</h3>
              <button className="modal-close" onClick={() => setTaxModal(false)}>x</button>
            </div>
            <form onSubmit={submitTax}>
              <div className="form-grid">
                <div className="field">
                  <label>Category *</label>
                  <select value={taxForm.taxCategoryId} onChange={(e) => setTaxForm((p) => ({ ...p, taxCategoryId: e.target.value }))}>
                    <option value="">Select category</option>
                    {categories.map((c) => (
                      <option key={c.taxCategoryId} value={c.taxCategoryId}>{c.name}</option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>Label *</label>
                  <input value={taxForm.label} onChange={(e) => setTaxForm((p) => ({ ...p, label: e.target.value }))} placeholder="A" maxLength={20} />
                </div>
                <div className="field">
                  <label>Rate (%) *</label>
                  <input type="number" step="0.0001" value={taxForm.rate} onChange={(e) => setTaxForm((p) => ({ ...p, rate: e.target.value }))} />
                </div>
                <div className="field">
                  <label>Tax Name</label>
                  <input value={taxForm.efiscalTaxname} maxLength={22} onChange={(e) => setTaxForm((p) => ({ ...p, efiscalTaxname: e.target.value }))} placeholder="e.g. PDV" />
                </div>
                <div className="field">
                  <label>Advance Prefix</label>
                  <input
                    value={taxForm.efiscalAdvanceprefix}
                    maxLength={50}
                    onChange={(e) => setTaxForm((p) => ({ ...p, efiscalAdvanceprefix: e.target.value }))}
                    placeholder="e.g. 20:"
                  />
                </div>
                <div className="field">
                  <label>Advance Name</label>
                  <input
                    value={taxForm.efiscalAdvancename}
                    maxLength={50}
                    onChange={(e) => setTaxForm((p) => ({ ...p, efiscalAdvancename: e.target.value }))}
                    placeholder="e.g. AvansA"
                  />
                </div>
                {taxMode === 'edit' ? (
                  <div className="field">
                    <label>Status</label>
                    <select
                      value={taxForm.isActive ? 'true' : 'false'}
                      onChange={(e) => setTaxForm((p) => ({ ...p, isActive: e.target.value === 'true' }))}
                    >
                      <option value="true">Active</option>
                      <option value="false">Inactive</option>
                    </select>
                  </div>
                ) : null}
              </div>
              {taxFormError ? <p style={{ color: 'var(--error, #c0392b)', marginTop: 12 }}>{taxFormError}</p> : null}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={() => setTaxModal(false)}>Cancel</button>
                <button type="submit" className="primary-button" disabled={savingTax}>
                  {savingTax ? 'Saving...' : taxMode === 'add' ? 'Create' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {categoryModal && (
        <div className="modal-overlay" onClick={() => setCategoryModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{categoryMode === 'add' ? 'Add Tax Category' : 'Edit Tax Category'}</h3>
              <button className="modal-close" onClick={() => setCategoryModal(false)}>x</button>
            </div>
            <form onSubmit={submitCategory}>
              <div className="form-grid">
                <div className="field">
                  <label>Name *</label>
                  <input value={categoryForm.name} onChange={(e) => setCategoryForm((p) => ({ ...p, name: e.target.value }))} maxLength={120} />
                </div>
                <div className="field">
                  <label>Code (2 chars)</label>
                  <input value={categoryForm.taxcategoryCode} maxLength={10} onChange={(e) => setCategoryForm((p) => ({ ...p, taxcategoryCode: e.target.value.toUpperCase() }))} placeholder="e.g. VA" />
                </div>
                {categoryMode === 'edit' ? (
                  <div className="field">
                    <label>Status</label>
                    <select
                      value={categoryForm.isActive ? 'true' : 'false'}
                      onChange={(e) => setCategoryForm((p) => ({ ...p, isActive: e.target.value === 'true' }))}
                    >
                      <option value="true">Active</option>
                      <option value="false">Inactive</option>
                    </select>
                  </div>
                ) : null}
              </div>
              {categoryFormError ? <p style={{ color: 'var(--error, #c0392b)', marginTop: 12 }}>{categoryFormError}</p> : null}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={() => setCategoryModal(false)}>Cancel</button>
                <button type="submit" className="primary-button" disabled={savingCategory}>
                  {savingCategory ? 'Saving...' : categoryMode === 'add' ? 'Create' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
