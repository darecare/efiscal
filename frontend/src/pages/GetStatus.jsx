import React, { useEffect, useState } from 'react'
import { useTranslation, Trans } from 'react-i18next'
import AppShell from '../components/AppShell'
import { fiscalBillApi } from '../services/api'
import { useOrg } from '../contexts/OrgContext'

export default function GetStatus() {
  const { t } = useTranslation()
  const { activeOrgId } = useOrg()

  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    setResult(null)
    setError(null)
  }, [activeOrgId])

  async function handleGetStatus() {
    if (!activeOrgId) {
      setError(t('getStatus.selectOrgRequired'))
      return
    }
    setLoading(true)
    setResult(null)
    setError(null)
    try {
      const data = await fiscalBillApi.getStatus(Number(activeOrgId))
      setResult(data)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || t('getStatus.loadFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setLoading(false)
    }
  }

  const topActions = (
    <button className="primary-button" onClick={handleGetStatus} disabled={loading || !activeOrgId}>
      {loading ? t('common.loading') : t('getStatus.getStatus')}
    </button>
  )

  return (
    <AppShell title={t('getStatus.title')} subtitle={t('getStatus.subtitle')} actions={topActions}>
      {!activeOrgId && (
        <p className="muted org-scope-hint">{t('orgSwitcher.selectPrompt')}</p>
      )}

      {error && (
        <div className="error-banner" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {result && (
        <div>
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <h3 style={{ marginBottom: '0.75rem' }}>{t('getStatus.responseStatus')}</h3>
            <table className="data-table">
              <tbody>
                {Object.entries(result)
                  .filter(([key]) => !['currentTaxRates', 'allTaxRates', 'supportedLanguages'].includes(key))
                  .map(([key, value]) => (
                    <tr key={key}>
                      <td style={{ fontWeight: 500, width: '200px' }}>{key}</td>
                      <td>{value !== null && value !== undefined ? String(value) : t('common.dash')}</td>
                    </tr>
                  ))}
                {result.supportedLanguages && (
                  <tr>
                    {/* eslint-disable-next-line i18next/no-literal-string */}
                    <td style={{ fontWeight: 500 }}>supportedLanguages</td>
                    <td>{result.supportedLanguages.join(', ')}</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {result.currentTaxRates && (
            <div className="card" style={{ marginBottom: '1.5rem' }}>
              <h3 style={{ marginBottom: '0.5rem' }}>
                {t('getStatus.currentTaxRates')}
                {result.currentTaxRates.validFrom ? ` — ${t('getStatus.validFrom', { date: result.currentTaxRates.validFrom })}` : ''}
                {result.currentTaxRates.groupId !== undefined ? ` (${t('getStatus.group', { id: result.currentTaxRates.groupId })})` : ''}
              </h3>
              <TaxCategoriesTable categories={result.currentTaxRates.taxCategories} t={t} />
            </div>
          )}

          {result.allTaxRates && result.allTaxRates.length > 0 && (
            <div className="card">
              <h3 style={{ marginBottom: '0.75rem' }}>{t('getStatus.allTaxRateGroups')}</h3>
              {result.allTaxRates.map((group, idx) => (
                <div key={idx} style={{ marginBottom: '1.25rem' }}>
                  <h4 style={{ marginBottom: '0.4rem' }}>
                    {group.validFrom
                      ? t('getStatus.groupWithValidFrom', { id: group.groupId, date: group.validFrom })
                      : t('getStatus.group', { id: group.groupId })}
                  </h4>
                  <TaxCategoriesTable categories={group.taxCategories} t={t} />
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {!result && !error && !loading && activeOrgId && (
        <div className="empty-state">
          <p>
            <Trans i18nKey="getStatus.emptyHint" components={{ strong: <strong /> }} />
          </p>
        </div>
      )}
    </AppShell>
  )
}

function TaxCategoriesTable({ categories, t }) {
  if (!categories || categories.length === 0) return <p>{t('getStatus.noTaxCategories')}</p>
  return (
    <table className="data-table">
      <thead>
        <tr>
          <th>{t('getStatus.categoryName')}</th>
          <th>{t('getStatus.categoryType')}</th>
          <th>{t('getStatus.order')}</th>
          <th>{t('getStatus.taxRatesCol')}</th>
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
                      <th style={{ padding: '2px 8px', textAlign: 'left', fontSize: '0.8rem', fontWeight: 600 }}>{t('getStatus.label')}</th>
                      <th style={{ padding: '2px 8px', textAlign: 'right', fontSize: '0.8rem', fontWeight: 600 }}>{t('getStatus.ratePercent')}</th>
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
              ) : t('common.dash')}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
