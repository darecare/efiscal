import React, { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { fiscalBillApi, orgsApi, taxApi, taxCategoryApi } from '../services/api'
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
  const { t } = useTranslation()
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [activeTab, setActiveTab] = useState('taxes')
  const [taxes, setTaxes] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [orgs, setOrgs] = useState([])
  const [importModalOpen, setImportModalOpen] = useState(false)
  const [importOrgId, setImportOrgId] = useState('')
  const [importing, setImporting] = useState(false)
  const [importError, setImportError] = useState(null)
  const [importSummary, setImportSummary] = useState(null)

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
    if (!isSuperAdmin) return
    orgsApi.list()
      .then(setOrgs)
      .catch(() => setOrgs([]))
  }, [isSuperAdmin])

  useEffect(() => {
    if (!success) return
    const timer = setTimeout(() => setSuccess(null), 3500)
    return () => clearTimeout(timer)
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
      setError(err?.response?.data?.message || err?.response?.data || t('taxes.loadFailed'))
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

  function openImportTaxes() {
    setImportModalOpen(true)
    setImportOrgId('')
    setImportError(null)
    setImportSummary(null)
  }

  function closeImportTaxes() {
    setImportModalOpen(false)
    setImportError(null)
    setImportSummary(null)
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
      setTaxFormError(t('taxes.taxCategoryRequired'))
      return
    }
    if (!taxForm.label.trim()) {
      setTaxFormError(t('taxes.labelRequired'))
      return
    }
    if (taxForm.rate === '' || Number.isNaN(Number(taxForm.rate))) {
      setTaxFormError(t('taxes.rateInvalid'))
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
        setSuccess(t('taxes.taxCreated'))
      } else {
        await taxApi.update(editTaxId, payload)
        setSuccess(t('taxes.taxUpdated'))
      }
      setTaxModal(false)
      setTaxes(await taxApi.list())
    } catch (err) {
      setTaxFormError(err?.response?.data?.message || err?.response?.data || t('common.operationFailed'))
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
      setCategoryFormError(t('taxes.categoryNameRequired'))
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
        setSuccess(t('taxes.categoryCreated'))
      } else {
        await taxCategoryApi.update(editCategoryId, payload)
        setSuccess(t('taxes.categoryUpdated'))
      }
      setCategoryModal(false)
      const updatedCategories = await taxCategoryApi.list()
      setCategories(updatedCategories)
      setTaxes(await taxApi.list())
    } catch (err) {
      setCategoryFormError(err?.response?.data?.message || err?.response?.data || t('common.operationFailed'))
    } finally {
      setSavingCategory(false)
    }
  }

  function normalizeText(value) {
    return (value ?? '').toString().trim()
  }

  function normalizeKey(value) {
    return normalizeText(value).toUpperCase()
  }

  async function submitImportTaxes() {
    setImportError(null)
    setImportSummary(null)

    if (!importOrgId) {
      setImportError(t('taxes.importOrgRequired'))
      return
    }

    try {
      setImporting(true)

      const statusResponse = await fiscalBillApi.getStatus(Number(importOrgId))
      const taxCategoriesFromStatus = statusResponse?.currentTaxRates?.taxCategories || []

      if (taxCategoriesFromStatus.length === 0) {
        setImportSummary({ createdCategories: 0, createdTaxes: 0, skippedTaxes: 0 })
        return
      }

      const [existingCategories, existingTaxes] = await Promise.all([
        taxCategoryApi.list(),
        taxApi.list(),
      ])

      const categoriesByCode = new Map(
        existingCategories
          .filter((category) => normalizeText(category.taxcategoryCode))
          .map((category) => [normalizeKey(category.taxcategoryCode), category])
      )
      const existingTaxLabels = new Set(existingTaxes.map((tax) => normalizeKey(tax.label)))

      let createdCategories = 0
      let createdTaxes = 0
      let skippedTaxes = 0

      for (const statusCategory of taxCategoriesFromStatus) {
        const categoryName = normalizeText(statusCategory?.name || statusCategory?.categoryName)
        if (!categoryName) continue

        const categoryCode = normalizeKey(categoryName).slice(0, 10)
        let category = categoriesByCode.get(categoryCode)

        if (!category) {
          category = await taxCategoryApi.create({
            name: categoryName,
            taxcategoryCode: categoryCode,
            isActive: true,
          })
          categoriesByCode.set(categoryCode, category)
          createdCategories += 1
        }

        for (const statusTax of statusCategory?.taxRates || []) {
          const label = normalizeText(statusTax?.label)
          if (!label) continue

          const normalizedLabel = normalizeKey(label)
          if (existingTaxLabels.has(normalizedLabel)) {
            skippedTaxes += 1
            continue
          }

          const rate = Number(statusTax?.rate)
          if (Number.isNaN(rate)) continue

          await taxApi.create({
            taxCategoryId: category.taxCategoryId,
            label: normalizedLabel,
            rate,
            isActive: true,
            efiscalTaxname: null,
            efiscalAdvanceprefix: null,
            efiscalAdvancename: null,
          })
          existingTaxLabels.add(normalizedLabel)
          createdTaxes += 1
        }
      }

      setImportSummary({ createdCategories, createdTaxes, skippedTaxes })
      setSuccess(t('taxes.importCompleted', { createdCategories, createdTaxes, skippedTaxes }))
      await loadAll()
    } catch (err) {
      setImportError(err?.response?.data?.message || err?.response?.data || t('taxes.importFailed'))
    } finally {
      setImporting(false)
    }
  }

  return (
    <AppShell
      title={t('taxes.title')}
      subtitle={t('taxes.subtitle')}
      actions={
        isSuperAdmin ? (
          activeTab === 'taxes'
            ? (
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="secondary-button" onClick={openImportTaxes} type="button">
                  {t('taxes.importTaxes')}
                </button>
                <button className="primary-button" onClick={openAddTax} type="button">
                  {t('taxes.addTax')}
                </button>
              </div>
            )
            : <button className="primary-button" onClick={openAddCategory}>{t('taxes.addTaxCategory')}</button>
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
            {t('taxes.taxRatesTab')}
          </button>
          <button
            className={activeTab === 'categories' ? 'primary-button' : 'secondary-button'}
            onClick={() => setActiveTab('categories')}
            type="button"
          >
            {t('taxes.taxCategoriesTab')}
          </button>
        </div>
      </section>

      {loading ? <p style={{ opacity: 0.6 }}>{t('common.loading')}</p> : null}

      {!loading && activeTab === 'taxes' && (
        <section className="table-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h3 style={{ margin: 0 }}>{t('taxes.taxRatesTitle')}</h3>
            <span style={{ fontSize: 13, opacity: 0.6 }}>{t('common.counts.records', { count: taxes.length })}</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>{t('taxes.labelLabel')}</th>
                <th>{t('taxes.ratePercent')}</th>
                <th>{t('taxes.category')}</th>
                <th>{t('taxes.taxName')}</th>
                <th>{t('taxes.advancePrefix')}</th>
                <th>{t('taxes.advanceName')}</th>
                <th>{t('common.status')}</th>
                {isSuperAdmin ? <th>{t('common.actions')}</th> : null}
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
                      {tax.isActive ? t('common.active') : t('common.inactive')}
                    </span>
                  </td>
                  {isSuperAdmin ? (
                    <td>
                      <button className="secondary-button" onClick={() => openEditTax(tax)}>{t('common.edit')}</button>
                    </td>
                  ) : null}
                </tr>
              ))}
              {taxes.length === 0 ? (
                <tr>
                  <td colSpan={isSuperAdmin ? 8 : 7} style={{ textAlign: 'center', opacity: 0.5, padding: '24px 0' }}>
                    {t('taxes.noTaxRates')}
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
            <h3 style={{ margin: 0 }}>{t('taxes.taxCategoriesTitle')}</h3>
            <span style={{ fontSize: 13, opacity: 0.6 }}>{t('common.counts.records', { count: categories.length })}</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>{t('common.name')}</th>
                <th>{t('taxes.code')}</th>
                <th>{t('common.status')}</th>
                {isSuperAdmin ? <th>{t('common.actions')}</th> : null}
              </tr>
            </thead>
            <tbody>
              {categories.map((category) => (
                <tr key={category.taxCategoryId}>
                  <td>{category.name}</td>
                  <td>{category.taxcategoryCode ?? '-'}</td>
                  <td>
                    <span className={`status-chip ${category.isActive ? 'active' : 'inactive'}`}>
                      {category.isActive ? t('common.active') : t('common.inactive')}
                    </span>
                  </td>
                  {isSuperAdmin ? (
                    <td>
                      <button className="secondary-button" onClick={() => openEditCategory(category)}>{t('common.edit')}</button>
                    </td>
                  ) : null}
                </tr>
              ))}
              {categories.length === 0 ? (
                <tr>
                  <td colSpan={isSuperAdmin ? 4 : 3} style={{ textAlign: 'center', opacity: 0.5, padding: '24px 0' }}>
                    {t('taxes.noTaxCategories')}
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
              <h3>{taxMode === 'add' ? t('taxes.addTaxTitle') : t('taxes.editTaxTitle')}</h3>
              <button className="modal-close" onClick={() => setTaxModal(false)} aria-label={t('common.close')}>×</button>
            </div>
            <form onSubmit={submitTax}>
              <div className="form-grid">
                <div className="field">
                  <label>{t('taxes.categoryLabel')} *</label>
                  <select value={taxForm.taxCategoryId} onChange={(e) => setTaxForm((p) => ({ ...p, taxCategoryId: e.target.value }))}>
                    <option value="">{t('taxes.selectCategory')}</option>
                    {categories.map((c) => (
                      <option key={c.taxCategoryId} value={c.taxCategoryId}>{c.name}</option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>{t('taxes.labelLabel')} *</label>
                  <input value={taxForm.label} onChange={(e) => setTaxForm((p) => ({ ...p, label: e.target.value }))} placeholder="A" maxLength={20} />
                </div>
                <div className="field">
                  <label>{t('taxes.rateLabel')} *</label>
                  <input type="number" step="0.0001" value={taxForm.rate} onChange={(e) => setTaxForm((p) => ({ ...p, rate: e.target.value }))} />
                </div>
                <div className="field">
                  <label>{t('taxes.taxName')}</label>
                  <input value={taxForm.efiscalTaxname} maxLength={22} onChange={(e) => setTaxForm((p) => ({ ...p, efiscalTaxname: e.target.value }))} placeholder={t('taxes.taxNamePlaceholder')} />
                </div>
                <div className="field">
                  <label>{t('taxes.advancePrefix')}</label>
                  <input
                    value={taxForm.efiscalAdvanceprefix}
                    maxLength={50}
                    onChange={(e) => setTaxForm((p) => ({ ...p, efiscalAdvanceprefix: e.target.value }))}
                    placeholder={t('taxes.advancePrefixPlaceholder')}
                  />
                </div>
                <div className="field">
                  <label>{t('taxes.advanceName')}</label>
                  <input
                    value={taxForm.efiscalAdvancename}
                    maxLength={50}
                    onChange={(e) => setTaxForm((p) => ({ ...p, efiscalAdvancename: e.target.value }))}
                    placeholder={t('taxes.advanceNamePlaceholder')}
                  />
                </div>
                {taxMode === 'edit' ? (
                  <div className="field">
                    <label>{t('common.status')}</label>
                    <select
                      value={taxForm.isActive ? 'true' : 'false'}
                      onChange={(e) => setTaxForm((p) => ({ ...p, isActive: e.target.value === 'true' }))}
                    >
                      <option value="true">{t('common.active')}</option>
                      <option value="false">{t('common.inactive')}</option>
                    </select>
                  </div>
                ) : null}
              </div>
              {taxFormError ? <p style={{ color: 'var(--error, #c0392b)', marginTop: 12 }}>{taxFormError}</p> : null}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={() => setTaxModal(false)}>{t('common.cancel')}</button>
                <button type="submit" className="primary-button" disabled={savingTax}>
                  {savingTax ? t('common.savingDots') : taxMode === 'add' ? t('common.create') : t('common.saveChanges')}
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
              <h3>{categoryMode === 'add' ? t('taxes.addCategoryTitle') : t('taxes.editCategoryTitle')}</h3>
              <button className="modal-close" onClick={() => setCategoryModal(false)} aria-label={t('common.close')}>×</button>
            </div>
            <form onSubmit={submitCategory}>
              <div className="form-grid">
                <div className="field">
                  <label>{t('taxes.nameLabel')} *</label>
                  <input value={categoryForm.name} onChange={(e) => setCategoryForm((p) => ({ ...p, name: e.target.value }))} maxLength={120} />
                </div>
                <div className="field">
                  <label>{t('taxes.codeLabel')}</label>
                  <input value={categoryForm.taxcategoryCode} maxLength={10} onChange={(e) => setCategoryForm((p) => ({ ...p, taxcategoryCode: e.target.value.toUpperCase() }))} placeholder={t('taxes.codePlaceholder')} />
                </div>
                {categoryMode === 'edit' ? (
                  <div className="field">
                    <label>{t('common.status')}</label>
                    <select
                      value={categoryForm.isActive ? 'true' : 'false'}
                      onChange={(e) => setCategoryForm((p) => ({ ...p, isActive: e.target.value === 'true' }))}
                    >
                      <option value="true">{t('common.active')}</option>
                      <option value="false">{t('common.inactive')}</option>
                    </select>
                  </div>
                ) : null}
              </div>
              {categoryFormError ? <p style={{ color: 'var(--error, #c0392b)', marginTop: 12 }}>{categoryFormError}</p> : null}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={() => setCategoryModal(false)}>{t('common.cancel')}</button>
                <button type="submit" className="primary-button" disabled={savingCategory}>
                  {savingCategory ? t('common.savingDots') : categoryMode === 'add' ? t('common.create') : t('common.saveChanges')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {importModalOpen && (
        <div className="modal-overlay" onClick={closeImportTaxes}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{t('taxes.importTaxesTitle')}</h3>
              <button className="modal-close" onClick={closeImportTaxes} aria-label={t('common.close')}>×</button>
            </div>

            <div style={{ padding: '20px' }}>
              <div className="field">
                <label>{t('taxes.importOrgLabel')} *</label>
                <select value={importOrgId} onChange={(e) => setImportOrgId(e.target.value)}>
                  <option value="">{t('taxes.selectImportOrg')}</option>
                  {orgs.map((org) => (
                    <option key={org.orgId} value={org.orgId}>{org.name}</option>
                  ))}
                </select>
              </div>

              <p className="muted" style={{ marginTop: 12 }}>
                {t('taxes.importTaxesHint')}
              </p>

              {importSummary && (
                <div className="success-banner" style={{ marginTop: 12 }}>
                  {t('taxes.importSummary')}
                </div>
              )}

              {importError ? <p className="error-text" style={{ marginTop: 12 }}>{importError}</p> : null}
            </div>

            <div className="modal-actions">
              {importSummary ? (
                <button type="button" className="primary-button" onClick={closeImportTaxes}>
                  {t('common.ok')}
                </button>
              ) : (
                <>
                  <button type="button" className="secondary-button" onClick={closeImportTaxes}>{t('common.cancel')}</button>
                  <button type="button" className="primary-button" disabled={importing} onClick={submitImportTaxes}>
                    {importing ? t('common.processing') : t('taxes.importTaxes')}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </AppShell>
  )
}
