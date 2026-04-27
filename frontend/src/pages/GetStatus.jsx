import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { fiscalBillApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

export default function GetStatus() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [orgs, setOrgs] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadOrgs = isSuperAdmin ? orgsApi.list() : orgsApi.myAccess()
    loadOrgs.then(setOrgs).catch(() => setOrgs([]))
  }, [isSuperAdmin])

  async function handleGetStatus() {
    if (!selectedOrgId) {
      setError('Please select an organization.')
      return
    }
    setLoading(true)
    setResult(null)
    setError(null)
    try {
      const data = await fiscalBillApi.getStatus(Number(selectedOrgId))
      setResult(data)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || 'Failed to get status.'
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setLoading(false)
    }
  }

  const topActions = (
    <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
      <select
        value={selectedOrgId}
        onChange={(e) => setSelectedOrgId(e.target.value)}
        className="form-input"
        style={{ minWidth: '200px' }}
      >
        <option value="">Select organization...</option>
        {orgs.map((org) => (
          <option key={org.orgId} value={org.orgId}>{org.name}</option>
        ))}
      </select>
      <button className="primary-button" onClick={handleGetStatus} disabled={loading}>
        {loading ? 'Loading...' : 'Get Status'}
      </button>
    </div>
  )

  return (
    <AppShell title="Get Status" subtitle="Tax Authority V-SDC status" actions={topActions}>
      {error && (
        <div className="error-banner" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {result && (
        <div>
          {/* Summary row */}
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <h3 style={{ marginBottom: '0.75rem' }}>Response Status: 200 OK</h3>
            <table className="data-table">
              <tbody>
                {Object.entries(result)
                  .filter(([key]) => !['currentTaxRates', 'allTaxRates', 'supportedLanguages'].includes(key))
                  .map(([key, value]) => (
                    <tr key={key}>
                      <td style={{ fontWeight: 500, width: '200px' }}>{key}</td>
                      <td>{value !== null && value !== undefined ? String(value) : '—'}</td>
                    </tr>
                  ))}
                {result.supportedLanguages && (
                  <tr>
                    <td style={{ fontWeight: 500 }}>supportedLanguages</td>
                    <td>{result.supportedLanguages.join(', ')}</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Current Tax Rates */}
          {result.currentTaxRates && (
            <div className="card" style={{ marginBottom: '1.5rem' }}>
              <h3 style={{ marginBottom: '0.5rem' }}>
                Current Tax Rates
                {result.currentTaxRates.validFrom ? ` — valid from ${result.currentTaxRates.validFrom}` : ''}
                {result.currentTaxRates.groupId !== undefined ? ` (Group ${result.currentTaxRates.groupId})` : ''}
              </h3>
              <TaxCategoriesTable categories={result.currentTaxRates.taxCategories} />
            </div>
          )}

          {/* All Tax Rates */}
          {result.allTaxRates && result.allTaxRates.length > 0 && (
            <div className="card">
              <h3 style={{ marginBottom: '0.75rem' }}>All Tax Rate Groups</h3>
              {result.allTaxRates.map((group, idx) => (
                <div key={idx} style={{ marginBottom: '1.25rem' }}>
                  <h4 style={{ marginBottom: '0.4rem' }}>
                    Group {group.groupId}
                    {group.validFrom ? ` — valid from ${group.validFrom}` : ''}
                  </h4>
                  <TaxCategoriesTable categories={group.taxCategories} />
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {!result && !error && !loading && (
        <div className="empty-state">
          <p>Select an organization and click <strong>Get Status</strong> to retrieve Tax Authority SDC status.</p>
        </div>
      )}
    </AppShell>
  )
}

function TaxCategoriesTable({ categories }) {
  if (!categories || categories.length === 0) return <p>No tax categories.</p>
  return (
    <table className="data-table">
      <thead>
        <tr>
          <th>Category Name</th>
          <th>Category Type</th>
          <th>Order</th>
          <th>Tax Rates (Label / Rate %)</th>
        </tr>
      </thead>
      <tbody>
        {categories.map((cat, i) => (
          <tr key={i}>
            <td>{cat.name}</td>
            <td>{cat.categoryType}</td>
            <td>{cat.orderId}</td>
            <td>
              {cat.taxRates && cat.taxRates.length > 0 ? (
                <table style={{ borderCollapse: 'collapse', width: '100%' }}>
                  <thead>
                    <tr>
                      <th style={{ padding: '2px 8px', textAlign: 'left', fontSize: '0.8rem', fontWeight: 600 }}>Label</th>
                      <th style={{ padding: '2px 8px', textAlign: 'right', fontSize: '0.8rem', fontWeight: 600 }}>Rate %</th>
                    </tr>
                  </thead>
                  <tbody>
                    {cat.taxRates.map((tr, j) => (
                      <tr key={j}>
                        <td style={{ padding: '2px 8px', fontSize: '0.85rem' }}>{tr.label}</td>
                        <td style={{ padding: '2px 8px', textAlign: 'right', fontSize: '0.85rem' }}>{tr.rate}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : '—'}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
