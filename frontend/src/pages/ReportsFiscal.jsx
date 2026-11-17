import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import i18n from 'i18next'
import AppShell from '../components/AppShell'
import { useOrg } from '../contexts/OrgContext'
import { reportsApi } from '../services/api'
import { DateRangePicker } from 'react-date-range'
import { enUS, srLatn } from 'date-fns/locale'
import { buildInputRanges, buildStaticRanges } from '../utils/dateRangePresets'
import 'react-date-range/dist/styles.css'
import 'react-date-range/dist/theme/default.css'

const PAGE_SIZE_OPTIONS = [25, 50, 100]
const DEFAULT_REPORT = 'products-sold-net-normal'

function startOfMonthDate() {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), 1)
}

function todayDate() {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

function toIsoDate(value) {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDateOnly(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`
}

function formatDateRangeLabel(startDate, endDate, t) {
  if (!startDate && !endDate) return t('fiscalBills.dateRange.empty')
  if (startDate && !endDate) return t('fiscalBills.dateRange.openEnd', { start: formatDateOnly(startDate) })
  if (!startDate && endDate) return t('fiscalBills.dateRange.openStart', { end: formatDateOnly(endDate) })
  return t('fiscalBills.dateRange.range', { start: formatDateOnly(startDate), end: formatDateOnly(endDate) })
}

function formatCell(value) {
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'number') return String(value)
  return String(value)
}

export default function ReportsFiscal() {
  const { t } = useTranslation()
  const { activeOrgId } = useOrg()

  const [catalog, setCatalog] = useState([])
  const [report, setReport] = useState(DEFAULT_REPORT)
  const [dateRange, setDateRange] = useState([
    {
      startDate: startOfMonthDate(),
      endDate: todayDate(),
      key: 'selection',
    },
  ])
  const [isDatePickerOpen, setIsDatePickerOpen] = useState(false)
  const dateRangeFieldRef = useRef(null)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(50)

  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const [hasRun, setHasRun] = useState(false)

  const datePickerLocale = useMemo(
    () => (i18n.language?.toLowerCase().startsWith('sr') ? srLatn : enUS),
    [i18n.language],
  )
  const staticRanges = useMemo(() => buildStaticRanges(t), [t, i18n.language])
  const inputRanges = useMemo(() => buildInputRanges(t), [t, i18n.language])
  const selectedDateRangeLabel = formatDateRangeLabel(dateRange[0].startDate, dateRange[0].endDate, t)
  const from = toIsoDate(dateRange[0].startDate)
  const to = toIsoDate(dateRange[0].endDate)

  useEffect(() => {
    function onDocumentClick(event) {
      if (!dateRangeFieldRef.current?.contains(event.target)) {
        setIsDatePickerOpen(false)
      }
    }
    document.addEventListener('mousedown', onDocumentClick)
    return () => document.removeEventListener('mousedown', onDocumentClick)
  }, [])

  useEffect(() => {
    let cancelled = false
    reportsApi.fiscalCatalog()
      .then((entries) => {
        if (cancelled) return
        const list = Array.isArray(entries) ? entries : []
        setCatalog(list)
        setReport((current) => (
          list.some((e) => e.code === current) || list.length === 0
            ? current
            : list[0].code
        ))
      })
      .catch(() => {
        if (!cancelled) {
          setCatalog([{
            code: DEFAULT_REPORT,
            labelKey: 'reports.catalog.productsSoldNetNormal',
            descriptionKey: 'reports.catalog.productsSoldNetNormalDescription',
          }])
        }
      })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    setResult(null)
    setHasRun(false)
    setPage(0)
    setError('')
  }, [activeOrgId])

  function handleDateRangeChange(ranges) {
    setDateRange([ranges.selection])
  }

  function clearDateRange() {
    setDateRange([
      {
        startDate: null,
        endDate: null,
        key: 'selection',
      },
    ])
  }

  async function runReport(nextPage = page, nextSize = size) {
    setError('')
    if (!activeOrgId) {
      setError(t('orgSwitcher.selectPrompt'))
      return
    }
    if (!from || !to) {
      setError(t('reports.dateRangeRequired'))
      return
    }
    if (from > to) {
      setError(t('reports.dateRangeInvalid'))
      return
    }
    setLoading(true)
    try {
      const data = await reportsApi.runFiscal({
        orgId: Number(activeOrgId),
        report,
        from,
        to,
        page: nextPage,
        size: nextSize,
      })
      setResult(data)
      setHasRun(true)
      setPage(data.page ?? nextPage)
      setSize(data.size ?? nextSize)
    } catch (err) {
      setResult(null)
      setHasRun(false)
      setError(err.response?.data?.message || t('reports.runFailed'))
    } finally {
      setLoading(false)
    }
  }

  function handleSubmit(e) {
    e.preventDefault()
    runReport(0, size)
  }

  async function handleExport() {
    setError('')
    if (!activeOrgId) {
      setError(t('orgSwitcher.selectPrompt'))
      return
    }
    if (!from || !to) {
      setError(t('reports.dateRangeRequired'))
      return
    }
    setExporting(true)
    try {
      const { blob, fileName } = await reportsApi.exportFiscalCsv({
        orgId: Number(activeOrgId),
        report,
        from,
        to,
        lang: i18n.language || 'en',
      })
      const url = window.URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = fileName
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      let message = t('reports.exportFailed')
      const data = err.response?.data
      if (data instanceof Blob) {
        try {
          const text = await data.text()
          const parsed = JSON.parse(text)
          if (parsed?.message) message = parsed.message
        } catch {
          /* keep fallback */
        }
      } else if (typeof data?.message === 'string') {
        message = data.message
      }
      setError(message)
    } finally {
      setExporting(false)
    }
  }

  const columns = result?.columns?.length
    ? result.columns
    : [
        { key: 'name', labelKey: 'reports.columns.name' },
        { key: 'sku', labelKey: 'reports.columns.sku' },
        { key: 'gtin', labelKey: 'reports.columns.gtin' },
        { key: 'qtySold', labelKey: 'reports.columns.qtySold' },
        { key: 'qtyRefunded', labelKey: 'reports.columns.qtyRefunded' },
        { key: 'qtyNet', labelKey: 'reports.columns.qtyNet' },
        { key: 'amountSold', labelKey: 'reports.columns.amountSold' },
        { key: 'amountRefunded', labelKey: 'reports.columns.amountRefunded' },
        { key: 'amountNet', labelKey: 'reports.columns.amountNet' },
        { key: 'billCount', labelKey: 'reports.columns.billCount' },
      ]

  const totalElements = result?.totalElements ?? 0
  const pageCount = Math.max(1, Math.ceil(totalElements / (size || 1)))
  const currentPage = (result?.page ?? page) + 1

  return (
    <AppShell title={t('reports.fiscalTitle')} subtitle={t('reports.fiscalSubtitle')}>
      <form className="filters-panel" onSubmit={handleSubmit}>
        <div className="filter-grid">
          <label className="field">
            <span>{t('reports.reportLabel')}</span>
            <select value={report} onChange={(e) => setReport(e.target.value)}>
              {(catalog.length > 0 ? catalog : [{ code: DEFAULT_REPORT, labelKey: 'reports.catalog.productsSoldNetNormal' }]).map((entry) => (
                <option key={entry.code} value={entry.code}>
                  {t(entry.labelKey, { defaultValue: entry.code })}
                </option>
              ))}
            </select>
          </label>
          <label className="field fiscal-date-range-field" ref={dateRangeFieldRef}>
            <span>{t('reports.sdcDateRange')}</span>
            <button
              type="button"
              className="secondary-button fiscal-date-range-trigger"
              onClick={() => setIsDatePickerOpen((prev) => !prev)}
            >
              {selectedDateRangeLabel}
            </button>
            {isDatePickerOpen && (
              <div className="fiscal-date-range-popover">
                <DateRangePicker
                  onChange={handleDateRangeChange}
                  ranges={dateRange}
                  locale={datePickerLocale}
                  staticRanges={staticRanges}
                  inputRanges={inputRanges}
                  moveRangeOnFirstSelection={false}
                  retainEndDateOnFirstSelection={false}
                  direction="horizontal"
                  showDateDisplay
                  editableDateInputs
                  dateDisplayFormat="dd.MM.yyyy"
                  showMonthAndYearPickers
                  months={2}
                />
                <div className="fiscal-date-range-actions">
                  <button type="button" className="secondary-button" onClick={clearDateRange}>{t('common.clear')}</button>
                  <button type="button" className="primary-button" onClick={() => setIsDatePickerOpen(false)}>{t('common.apply')}</button>
                </div>
              </div>
            )}
          </label>
          <label className="field">
            <span>{t('reports.pageSize')}</span>
            <select
              value={size}
              onChange={(e) => {
                const next = Number(e.target.value)
                setSize(next)
                if (hasRun) runReport(0, next)
              }}
            >
              {PAGE_SIZE_OPTIONS.map((n) => (
                <option key={n} value={n}>{t('common.perPage', { count: n })}</option>
              ))}
            </select>
          </label>
        </div>
        <div className="inline-actions">
          <button className="primary-button" type="submit" disabled={loading || !activeOrgId}>
            {loading ? t('reports.running') : t('reports.run')}
          </button>
          <button
            type="button"
            className="secondary-button"
            onClick={handleExport}
            disabled={exporting || !activeOrgId || !from || !to}
          >
            {exporting ? t('reports.exporting') : t('reports.exportCsv')}
          </button>
          {hasRun && (
            <span className="badge">{t('common.counts.records', { count: totalElements })}</span>
          )}
        </div>
        {catalog.find((e) => e.code === report)?.descriptionKey && (
          <p className="muted" style={{ marginTop: 8 }}>
            {t(catalog.find((e) => e.code === report).descriptionKey)}
          </p>
        )}
      </form>

      {!activeOrgId && (
        <p className="muted org-scope-hint">{t('orgSwitcher.selectPrompt')}</p>
      )}

      {error && <div className="error-banner">{error}</div>}

      {hasRun && !loading && (
        <section className="table-card">
          {(!result?.rows || result.rows.length === 0) ? (
            <p className="muted">{t('reports.noRows')}</p>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    {columns.map((col) => (
                      <th key={col.key}>{t(col.labelKey, { defaultValue: col.key })}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {result.rows.map((row, idx) => (
                    <tr key={row.productKey || idx}>
                      {columns.map((col) => (
                        <td key={col.key}>{formatCell(row[col.key])}</td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
              {pageCount > 1 && (
                <div className="pagination" style={{ marginTop: '0.75rem', justifyContent: 'flex-end' }}>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => runReport(Math.max(0, page - 1), size)}
                    disabled={page <= 0 || loading}
                  >
                    {t('common.prev')}
                  </button>
                  <span className="pagination-info">
                    {t('common.paginationInfo', {
                      current: currentPage,
                      total: pageCount,
                      records: totalElements,
                    })}
                  </span>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => runReport(page + 1, size)}
                    disabled={page + 1 >= pageCount || loading}
                  >
                    {t('common.next')}
                  </button>
                </div>
              )}
            </>
          )}
        </section>
      )}
    </AppShell>
  )
}
