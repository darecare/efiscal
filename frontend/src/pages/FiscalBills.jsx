import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import i18n from 'i18next'
import AppShell from '../components/AppShell'
import { fiscalBillApi } from '../services/api'
import { useOrg } from '../contexts/OrgContext'
import { DateRangePicker } from 'react-date-range'
import { enUS, srLatn } from 'date-fns/locale'
import 'react-date-range/dist/styles.css'
import 'react-date-range/dist/theme/default.css'

const INVOICE_TYPE_VALUES = [0, 2, 4]
const TRANSACTION_TYPE_VALUES = [0, 1]
const PAYMENT_TYPE_VALUES = [0, 1, 2, 3, 4, 5, 6]

export default function FiscalBills() {
  const { t } = useTranslation()
  const { activeOrgId } = useOrg()

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [fiscalBills, setFiscalBills] = useState([])
  const [hasFetched, setHasFetched] = useState(false)
  const [selectedFiscalBillId, setSelectedFiscalBillId] = useState('')
  const [detailsLoading, setDetailsLoading] = useState(false)
  const [detailsError, setDetailsError] = useState(null)
  const [details, setDetails] = useState(null)
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false)
  const [activeTab, setActiveTab] = useState('tax')
  const [downloadingPdfFormat, setDownloadingPdfFormat] = useState('')
  const [copyingBillIds, setCopyingBillIds] = useState({})
  const [refundingBillIds, setRefundingBillIds] = useState({})
  const [copyConfirmBill, setCopyConfirmBill] = useState(null)
  const [refundConfirmBill, setRefundConfirmBill] = useState(null)
  const [refundAlreadyExistsBill, setRefundAlreadyExistsBill] = useState(null)
  const [successMsg, setSuccessMsg] = useState('')

  const PAGE_SIZE = 50
  const [tablePage, setTablePage] = useState(1)
  const datePickerLocale = useMemo(
    () => (i18n.language?.toLowerCase().startsWith('sr') ? srLatn : enUS),
    [i18n.language],
  )

  // Filter states
  const [dateRange, setDateRange] = useState([
    {
      startDate: null,
      endDate: null,
      key: 'selection',
    },
  ])
  const [isDatePickerOpen, setIsDatePickerOpen] = useState(false)
  const [filterInvoiceNo, setFilterInvoiceNo] = useState('')
  const [filterOrderId, setFilterOrderId] = useState('')
  const [filterInvoiceType, setFilterInvoiceType] = useState('')
  const [filterTransactionType, setFilterTransactionType] = useState('')
  const [filterCustomerName, setFilterCustomerName] = useState('')
  const dateRangeFieldRef = useRef(null)

  useEffect(() => {
    function onDocumentClick(event) {
      if (!dateRangeFieldRef.current?.contains(event.target)) {
        setIsDatePickerOpen(false)
      }

      // Close Actions dropdowns when clicking outside any dropdown area.
      if (!event.target.closest('.action-dropdown')) {
        document.querySelectorAll('.action-dropdown[open]').forEach((el) => {
          el.removeAttribute('open')
        })
      }
    }
    document.addEventListener('mousedown', onDocumentClick)
    return () => document.removeEventListener('mousedown', onDocumentClick)
  }, [])

  useEffect(() => {
    setFiscalBills([])
    setHasFetched(false)
    setTablePage(1)
    setDetails(null)
    setDetailsError(null)
    setSelectedFiscalBillId('')
    setIsDetailsModalOpen(false)
    setError(null)
    setSuccessMsg('')
  }, [activeOrgId])

  async function handleLoadFiscalBills(e) {
    if (e) e.preventDefault()
    if (!activeOrgId) {
      setError(t('fiscalBills.selectOrgRequired'))
      return
    }

    setLoading(true)
    setSuccessMsg('')
    setError(null)
    setDetails(null)
    setDetailsError(null)
    setSelectedFiscalBillId('')
    setHasFetched(true)
    try {
      const data = await fiscalBillApi.list(Number(activeOrgId))
      setFiscalBills(data)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || t('fiscalBills.loadFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
      setFiscalBills([])
    } finally {
      setLoading(false)
    }
  }

  // Client-side filtering applied on top of loaded data
  const filteredBills = useMemo(() => {
    let result = fiscalBills
    if (dateRange[0].startDate) {
      const from = new Date(dateRange[0].startDate)
      from.setHours(0, 0, 0, 0)
      result = result.filter((b) => {
        if (!b.sdcDateTime) return false
        const d = new Date(b.sdcDateTime)
        return !isNaN(d.getTime()) && d >= from
      })
    }
    if (dateRange[0].endDate) {
      const to = new Date(dateRange[0].endDate)
      to.setHours(23, 59, 59, 999)
      result = result.filter((b) => {
        if (!b.sdcDateTime) return false
        const d = new Date(b.sdcDateTime)
        return !isNaN(d.getTime()) && d <= to
      })
    }
    if (filterInvoiceNo.trim()) {
      const q = filterInvoiceNo.trim().toLowerCase()
      result = result.filter((b) => b.sdcInvoiceNumber?.toLowerCase().includes(q))
    }
    if (filterOrderId.trim()) {
      const q = filterOrderId.trim().toLowerCase()
      result = result.filter((b) => String(b.orderId || '').toLowerCase().includes(q))
    }
    if (filterInvoiceType !== '') {
      const t = Number(filterInvoiceType)
      result = result.filter((b) => b.invoiceType === t)
    }
    if (filterTransactionType !== '') {
      const t = Number(filterTransactionType)
      result = result.filter((b) => b.transactionType === t)
    }
    if (filterCustomerName.trim()) {
      const q = filterCustomerName.trim().toLowerCase()
      result = result.filter((b) => b.customerName?.toLowerCase().includes(q))
    }
    return result
  }, [dateRange, fiscalBills, filterCustomerName, filterInvoiceNo, filterInvoiceType, filterOrderId, filterTransactionType])

  // Reset to first page whenever the filtered result set changes
  const prevFilteredLengthRef = React.useRef(filteredBills.length)
  if (prevFilteredLengthRef.current !== filteredBills.length) {
    prevFilteredLengthRef.current = filteredBills.length
    if (tablePage !== 1) setTablePage(1)
  }

  const tablePageCount = Math.max(1, Math.ceil(filteredBills.length / PAGE_SIZE))
  const pagedBills = filteredBills.slice((tablePage - 1) * PAGE_SIZE, tablePage * PAGE_SIZE)

  const selectedDateRangeLabel = formatDateRangeLabel(dateRange[0].startDate, dateRange[0].endDate, t)

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

  async function handleSelectFiscalBill(fiscalbillId) {
    setSelectedFiscalBillId(fiscalbillId)
    setDetailsLoading(true)
    setDetailsError(null)
    try {
      const data = await fiscalBillApi.details(fiscalbillId)
      setDetails(data)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || t('fiscalBills.loadDetailsFailed')
      setDetailsError(typeof msg === 'string' ? msg : JSON.stringify(msg))
      setDetails(null)
    } finally {
      setDetailsLoading(false)
    }
  }

  async function openDetailsModal(fiscalbillId) {
    closeAllActionDropdowns()
    setActiveTab('tax')
    setIsDetailsModalOpen(true)
    await handleSelectFiscalBill(fiscalbillId)
  }

  function closeDetailsModal() {
    setIsDetailsModalOpen(false)
    setDetailsError(null)
    setDownloadingPdfFormat('')
  }

  async function handleDownloadPdf(format) {
    const fiscalBillId = details?.fiscalBill?.fiscalbillId
    if (!fiscalBillId) return

    setDetailsError(null)
    setDownloadingPdfFormat(format)
    try {
      const blob = await fiscalBillApi.downloadPdf(fiscalBillId, format)
      const url = window.URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `fiscal-bill-${fiscalBillId}-${format}.pdf`
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || t('fiscalBills.downloadPdfFailed')
      setDetailsError(typeof msg === 'string' ? msg : t('fiscalBills.downloadPdfFailed'))
    } finally {
      setDownloadingPdfFormat('')
    }
  }

  function createIdempotencyKey() {
    if (window.crypto && window.crypto.randomUUID) return window.crypto.randomUUID()
    return `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`
  }

  async function handleCreateCopy(fiscalBill) {
    const fiscalBillId = fiscalBill.fiscalbillId
    setCopyingBillIds((prev) => ({ ...prev, [fiscalBillId]: true }))
    setSuccessMsg('')
    setError(null)
    try {
      const idempotencyKey = createIdempotencyKey()
      const created = await fiscalBillApi.createCopy(fiscalBillId, idempotencyKey)
      setSuccessMsg(t('fiscalBills.copyCreatedToast', { id: created.fiscalbillId }))
      await handleLoadFiscalBills()
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data?.lastError || err?.response?.data || err?.message || t('fiscalBills.copyFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setCopyingBillIds((prev) => ({ ...prev, [fiscalBillId]: false }))
    }
  }

  async function handleCreateRefund(fiscalBill) {
    const fiscalBillId = fiscalBill.fiscalbillId
    setRefundingBillIds((prev) => ({ ...prev, [fiscalBillId]: true }))
    setSuccessMsg('')
    setError(null)
    try {
      const idempotencyKey = createIdempotencyKey()
      const created = await fiscalBillApi.createRefund(fiscalBillId, idempotencyKey)
      setSuccessMsg(t('fiscalBills.refundCreatedToast', { id: created.fiscalbillId }))
      await handleLoadFiscalBills()
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data?.lastError || err?.response?.data || err?.message || t('fiscalBills.refundFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setRefundingBillIds((prev) => ({ ...prev, [fiscalBillId]: false }))
    }
  }

  function openCopyConfirm(fiscalBill) {
    closeAllActionDropdowns()
    setCopyConfirmBill(fiscalBill)
  }

  function closeCopyConfirm() {
    setCopyConfirmBill(null)
  }

  function hasExistingRefund(bill) {
    if (!bill.orderId) return false
    return fiscalBills.some(
      (b) =>
        b.orderId === bill.orderId &&
        b.invoiceType === bill.invoiceType &&
        b.transactionType === 1 &&
        b.status === 'SUCCESS'
    )
  }

  function openRefundConfirm(fiscalBill) {
    closeAllActionDropdowns()
    if (hasExistingRefund(fiscalBill)) {
      setRefundAlreadyExistsBill(fiscalBill)
    } else {
      setRefundConfirmBill(fiscalBill)
    }
  }

  function closeRefundConfirm() {
    setRefundConfirmBill(null)
  }

  async function confirmCreateCopy() {
    if (!copyConfirmBill) return
    const bill = copyConfirmBill
    closeCopyConfirm()
    await handleCreateCopy(bill)
  }

  async function confirmCreateRefund() {
    if (!refundConfirmBill) return
    const bill = refundConfirmBill
    closeRefundConfirm()
    await handleCreateRefund(bill)
  }

  function canCreateRefund(bill) {
    return bill.transactionType === 0 && bill.invoiceType !== 2
  }

  function closeAllActionDropdowns() {
    document.querySelectorAll('.action-dropdown[open]').forEach((el) => {
      el.removeAttribute('open')
    })
  }

  return (
    <AppShell title={t('fiscalBills.title')} subtitle={t('fiscalBills.subtitle')}>
      {successMsg ? <div className="success-banner" style={{ marginBottom: '1rem' }}>{successMsg}</div> : null}
      {error ? <div className="error-banner" style={{ marginBottom: '1rem' }}>{error}</div> : null}

      <form className="filters-panel" onSubmit={handleLoadFiscalBills} style={{ marginBottom: '1rem' }}>
        <div className="filter-grid">
          <label className="field fiscal-date-range-field" ref={dateRangeFieldRef}>
            <span>{t('fiscalBills.taDateRange')}</span>
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
                  moveRangeOnFirstSelection={false}
                  direction="horizontal"
                  showDateDisplay={false}
                  months={1}
                />
                <div className="fiscal-date-range-actions">
                  <button type="button" className="secondary-button" onClick={clearDateRange}>{t('common.clear')}</button>
                  <button type="button" className="primary-button" onClick={() => setIsDatePickerOpen(false)}>{t('common.apply')}</button>
                </div>
              </div>
            )}
          </label>
          <label className="field">
            <span>{t('fiscalBills.taInvoiceNo')}</span>
            <input
              type="text"
              placeholder={t('fiscalBills.taInvoiceNoPlaceholder')}
              value={filterInvoiceNo}
              onChange={(e) => setFilterInvoiceNo(e.target.value)}
            />
          </label>
          <label className="field">
            <span>{t('fiscalBills.orderId')}</span>
            <input
              type="text"
              placeholder={t('fiscalBills.orderIdPlaceholder')}
              value={filterOrderId}
              onChange={(e) => setFilterOrderId(e.target.value)}
            />
          </label>
          <label className="field">
            <span>{t('fiscalBills.invoiceType')}</span>
            <select
              value={filterInvoiceType}
              onChange={(e) => setFilterInvoiceType(e.target.value)}
            >
              <option value="">{t('common.selectAllTypesPlaceholder')}</option>
              {INVOICE_TYPE_VALUES.map((v) => (
                <option key={v} value={v}>{t(`fiscalBills.invoiceTypes.${v}`)}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>{t('fiscalBills.transactionType')}</span>
            <select
              value={filterTransactionType}
              onChange={(e) => setFilterTransactionType(e.target.value)}
            >
              <option value="">{t('common.selectAllPlaceholder')}</option>
              {TRANSACTION_TYPE_VALUES.map((v) => (
                <option key={v} value={v}>{t(`fiscalBills.transactionTypes.${v}`)}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>{t('fiscalBills.customerName')}</span>
            <input
              type="text"
              placeholder={t('fiscalBills.customerNamePlaceholder')}
              value={filterCustomerName}
              onChange={(e) => setFilterCustomerName(e.target.value)}
            />
          </label>
        </div>
        <div className="inline-actions">
          <button className="primary-button" type="submit" disabled={loading || !activeOrgId}>
            {loading ? t('common.loading') : t('fiscalBills.loadFiscalBills')}
          </button>
          {hasFetched && (
            <span className="badge">
              {filteredBills.length !== fiscalBills.length
                ? t('fiscalBills.recordsBadgeFiltered', { shown: filteredBills.length, total: fiscalBills.length })
                : t('common.counts.records', { count: filteredBills.length })}
            </span>
          )}
        </div>
      </form>

      {!activeOrgId && (
        <p className="muted org-scope-hint">{t('orgSwitcher.selectPrompt')}</p>
      )}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginBottom: '0.75rem' }}>{t('fiscalBills.listTitle')}</h3>
        {!activeOrgId ? null : !hasFetched ? (
          <div className="empty-state">
            <p>{t('fiscalBills.selectOrgPrompt')}</p>
          </div>
        ) : filteredBills.length === 0 ? (
          <div className="empty-state">
            <p>{fiscalBills.length === 0 ? t('fiscalBills.noBillsForOrg') : t('fiscalBills.noBillsMatchFilters')}</p>
          </div>
        ) : (
          <>
          {tablePageCount > 1 && (
            <div className="pagination" style={{ marginBottom: '0.75rem', justifyContent: 'flex-end' }}>
              <button
                type="button"
                className="secondary-button"
                onClick={() => setTablePage((p) => Math.max(1, p - 1))}
                disabled={tablePage === 1}
              >
                {t('common.prev')}
              </button>
              <span className="pagination-info">
                {t('common.paginationInfo', { current: tablePage, total: tablePageCount, records: filteredBills.length })}
              </span>
              <button
                type="button"
                className="secondary-button"
                onClick={() => setTablePage((p) => Math.min(tablePageCount, p + 1))}
                disabled={tablePage >= tablePageCount}
              >
                {t('common.next')}
              </button>
            </div>
          )}
          <table className="data-table fiscal-bills-table">
            <thead>
              <tr>
                <th>{t('fiscalBills.orderId')}</th>
                <th>{t('common.status')}</th>
                <th>{t('orders.customer')}</th>
                <th>{t('fiscalBills.invoiceType')}</th>
                <th>{t('fiscalBills.transactionType')}</th>
                <th>{t('fiscalBills.taInvoiceNo')}</th>
                <th>{t('orders.total')}</th>
                <th>{t('common.created')}</th>
                <th>{t('fiscalBills.action')}</th>
              </tr>
            </thead>
            <tbody>
              {pagedBills.map((bill) => (
                <tr key={bill.fiscalbillId}>
                  <td>{bill.orderId || t('common.dash')}</td>
                  <td>{bill.status}</td>
                  <td>{bill.customerName || t('common.dash')}</td>
                  <td>{t(`fiscalBills.invoiceTypes.${bill.invoiceType}`, { defaultValue: bill.invoiceType ?? t('common.dash') })}</td>
                  <td>{t(`fiscalBills.transactionTypes.${bill.transactionType}`, { defaultValue: bill.transactionType ?? t('common.dash') })}</td>
                  <td>{bill.sdcInvoiceNumber || t('common.dash')}</td>
                  <td className="cell-right">{bill.totalAmount ?? t('common.dash')}</td>
                  <td>{formatDateTime(bill.createdAt)}</td>
                  <td>
                    <details className="action-dropdown">
                      <summary className="secondary-button">{t('common.actions')}</summary>
                      <div className="action-dropdown-menu">
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={() => openDetailsModal(bill.fiscalbillId)}
                        >
                          {t('fiscalBills.viewDetails')}
                        </button>
                        {(bill.invoiceType === 0 || bill.invoiceType === 4) && (
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => openCopyConfirm(bill)}
                            disabled={Boolean(copyingBillIds[bill.fiscalbillId])}
                          >
                            {copyingBillIds[bill.fiscalbillId] ? t('fiscalBills.creatingCopy') : t('fiscalBills.createCopy')}
                          </button>
                        )}
                        {canCreateRefund(bill) && (
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => openRefundConfirm(bill)}
                            disabled={Boolean(refundingBillIds[bill.fiscalbillId])}
                          >
                            {refundingBillIds[bill.fiscalbillId] ? t('fiscalBills.creatingRefund') : t('fiscalBills.createRefund')}
                          </button>
                        )}
                      </div>
                    </details>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {tablePageCount > 1 && (
            <div className="pagination" style={{ marginTop: '0.75rem', justifyContent: 'flex-end' }}>
              <button
                type="button"
                className="secondary-button"
                onClick={() => setTablePage((p) => Math.max(1, p - 1))}
                disabled={tablePage === 1}
              >
                {t('common.prev')}
              </button>
              <span className="pagination-info">
                {t('common.paginationInfo', { current: tablePage, total: tablePageCount, records: filteredBills.length })}
              </span>
              <button
                type="button"
                className="secondary-button"
                onClick={() => setTablePage((p) => Math.min(tablePageCount, p + 1))}
                disabled={tablePage >= tablePageCount}
              >
                {t('common.next')}
              </button>
            </div>
          )}
          </>
        )}
      </div>

      {isDetailsModalOpen && (
        <div className="fiscalbills-modal-overlay" onClick={closeDetailsModal}>
          <div className="fiscalbills-modal" onClick={(e) => e.stopPropagation()}>
            <div className="fiscalbills-modal-header">
              <h3 style={{ margin: 0 }}>{t('fiscalBills.detailsTitle')}</h3>
              <button type="button" className="secondary-button" onClick={closeDetailsModal}>{t('common.close')}</button>
            </div>

            {detailsError ? <div className="error-banner" style={{ marginBottom: '1rem' }}>{detailsError}</div> : null}
            {detailsLoading ? <p>{t('fiscalBills.loadingDetails')}</p> : null}

            {!detailsLoading && details?.fiscalBill && (
              <>
                <div className="fiscalbills-summary-grid">
                  <div><strong>{t('fiscalBills.fiscalBillId')}</strong> {details.fiscalBill.fiscalbillId}</div>
                  <div><strong>{t('account.statusLabel')}:</strong> {details.fiscalBill.status}</div>
                  <div><strong>{t('fiscalBills.orderId')}</strong> {details.fiscalBill.orderId || t('common.dash')}</div>
                  <div><strong>{t('fiscalBills.taInvoiceNoLabel')}:</strong> {details.fiscalBill.sdcInvoiceNumber || t('common.dash')}</div>
                  <div><strong>{t('common.created')}</strong> {formatDateTime(details.fiscalBill.createdAt)}</div>
                  <div><strong>{t('fiscalBills.updated')}</strong> {formatDateTime(details.fiscalBill.updatedAt)}</div>
                  <div className="fiscalbills-link-row">
                    <strong>{t('fiscalBills.verificationLink')}</strong>
                    {details.fiscalBill.efiscalLink ? (
                      <a
                        href={details.fiscalBill.efiscalLink}
                        target="_blank"
                        rel="noreferrer"
                        className="icon-link"
                        title={t('fiscalBills.openVerificationLink')}
                      >
                        <span className="external-link-icon" aria-hidden="true"></span>
                      </a>
                    ) : ` ${t('common.dash')}`}
                  </div>
                </div>

                {details.fiscalBill.efiscalQr || details.fiscalBill.efiscalLink ? (
                  <div className="fiscalbills-qr-wrap">
                    <img
                      src={toQrImageSrc(details.fiscalBill.efiscalQr, details.fiscalBill.efiscalLink)}
                      alt={t('fiscalBills.qrAlt')}
                      className="fiscalbills-qr-image"
                    />
                  </div>
                ) : null}

                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => handleDownloadPdf('a4')}
                    disabled={downloadingPdfFormat !== ''}
                  >
                    {downloadingPdfFormat === 'a4' ? t('fiscalBills.downloadingPdf') : t('fiscalBills.downloadPdfA4')}
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => handleDownloadPdf('roll80')}
                    disabled={downloadingPdfFormat !== ''}
                  >
                    {downloadingPdfFormat === 'roll80' ? t('fiscalBills.downloadingPdf') : t('fiscalBills.downloadPdfRoll80')}
                  </button>
                </div>

                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
                  <button
                    className={activeTab === 'tax' ? 'primary-button' : 'secondary-button'}
                    onClick={() => setActiveTab('tax')}
                    type="button"
                  >
                    {t('fiscalBills.taxItems')}
                  </button>
                  <button
                    className={activeTab === 'lines' ? 'primary-button' : 'secondary-button'}
                    onClick={() => setActiveTab('lines')}
                    type="button"
                  >
                    {t('fiscalBills.lineItems')}
                  </button>
                  <button
                    className={activeTab === 'payment' ? 'primary-button' : 'secondary-button'}
                    onClick={() => setActiveTab('payment')}
                    type="button"
                  >
                    {t('fiscalBills.paymentItems')}
                  </button>
                </div>

                {activeTab === 'tax' ? (
                  <table className="data-table fiscal-bills-table">
                    <thead>
                      <tr>
                        <th>{t('fiscalBills.taxLabel')}</th>
                        <th>{t('fiscalBills.category')}</th>
                        <th>{t('fiscalBills.categoryType')}</th>
                        <th>{t('fiscalBills.rate')}</th>
                        <th>{t('fiscalBills.amount')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {details.taxItems.length === 0 ? (
                        <tr>
                          <td colSpan="5">{t('fiscalBills.noTaxItems')}</td>
                        </tr>
                      ) : details.taxItems.map((item) => (
                        <tr key={item.fiscalbilltaxId}>
                          <td>{item.taxLabel || t('common.dash')}</td>
                          <td>{item.categoryName || t('common.dash')}</td>
                          <td>{item.categoryType ?? t('common.dash')}</td>
                          <td className="cell-right">{item.rate ?? t('common.dash')}</td>
                          <td className="cell-right">{item.amount ?? t('common.dash')}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : activeTab === 'lines' ? (
                  <table className="data-table fiscal-bills-table">
                    <thead>
                      <tr>
                        <th>{t('common.name')}</th>
                        <th>{t('orders.qty')}</th>
                        <th>{t('orders.unitPrice')}</th>
                        <th>{t('orders.total')}</th>
                        <th>{t('fiscalBills.taxLabel')}</th>
                        <th>{t('fiscalBills.gtin')}</th>
                        <th>{t('fiscalBills.productId')}</th>
                        <th>{t('orders.sku')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {details.lineItems.length === 0 ? (
                        <tr>
                          <td colSpan="8">{t('fiscalBills.noLineItems')}</td>
                        </tr>
                      ) : details.lineItems.map((item) => (
                        <tr key={item.fiscalbilllineId}>
                          <td>{item.name || t('common.dash')}</td>
                          <td className="cell-right">{item.quantity ?? t('common.dash')}</td>
                          <td className="cell-right">{item.unitPrice ?? t('common.dash')}</td>
                          <td className="cell-right">{item.totalAmount ?? t('common.dash')}</td>
                          <td>{item.taxLabel || t('common.dash')}</td>
                          <td>{item.gtin || t('common.dash')}</td>
                          <td>{item.productId || t('common.dash')}</td>
                          <td>{item.sku || t('common.dash')}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <table className="data-table fiscal-bills-table">
                    <thead>
                      <tr>
                        <th>{t('fiscalBills.paymentType')}</th>
                        <th>{t('fiscalBills.amount')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {details.payments.length === 0 ? (
                        <tr>
                          <td colSpan="2">{t('fiscalBills.noPaymentRows')}</td>
                        </tr>
                      ) : details.payments.map((payment) => (
                        <tr key={payment.fiscalbillpayId}>
                          <td>{t(`fiscalBills.paymentTypes.${payment.paymentType}`, { defaultValue: payment.paymentType })}</td>
                          <td className="cell-right">{payment.amount ?? t('common.dash')}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </>
            )}
          </div>
        </div>
      )}

      {copyConfirmBill && (
        <div className="modal-overlay" onClick={closeCopyConfirm}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{t('fiscalBills.confirmCopyTitle')}</h3>
              <button type="button" className="modal-close" onClick={closeCopyConfirm} aria-label={t('common.close')}>×</button>
            </div>
            <p style={{ marginTop: 0 }}>
              {t('fiscalBills.confirmCopyBody')}
            </p>
            <p>
              <strong>{t('fiscalBills.orderId')}</strong> {copyConfirmBill.orderId || t('common.dash')}<br />
              <strong>{t('fiscalBills.taInvoiceNoLabel')}:</strong> {copyConfirmBill.sdcInvoiceNumber || t('common.dash')}<br />
              <strong>{t('fiscalBills.transactionType')}</strong> {t(`fiscalBills.transactionTypes.${copyConfirmBill.transactionType}`, { defaultValue: copyConfirmBill.transactionType ?? t('common.dash') })}
            </p>
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={closeCopyConfirm}>{t('common.cancel')}</button>
              <button
                type="button"
                className="primary-button"
                onClick={confirmCreateCopy}
                disabled={Boolean(copyingBillIds[copyConfirmBill.fiscalbillId])}
              >
                {t('fiscalBills.confirmCreateCopy')}
              </button>
            </div>
          </div>
        </div>
      )}

      {refundAlreadyExistsBill && (
        <div className="modal-overlay" onClick={() => setRefundAlreadyExistsBill(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{t('fiscalBills.cannotRefundTitle')}</h3>
              <button type="button" className="modal-close" onClick={() => setRefundAlreadyExistsBill(null)} aria-label={t('common.close')}>×</button>
            </div>
            <p style={{ marginTop: 0 }}>{t('fiscalBills.refundAlreadyExists')}</p>
            <p>
              <strong>{t('fiscalBills.orderId')}</strong> {refundAlreadyExistsBill.orderId || t('common.dash')}<br />
              <strong>{t('fiscalBills.taInvoiceNoLabel')}:</strong> {refundAlreadyExistsBill.sdcInvoiceNumber || t('common.dash')}
            </p>
            <div className="modal-actions">
              <button type="button" className="primary-button" onClick={() => setRefundAlreadyExistsBill(null)}>{t('common.ok')}</button>
            </div>
          </div>
        </div>
      )}

      {refundConfirmBill && (
        <div className="modal-overlay" onClick={closeRefundConfirm}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{t('fiscalBills.confirmRefundTitle')}</h3>
              <button type="button" className="modal-close" onClick={closeRefundConfirm} aria-label={t('common.close')}>×</button>
            </div>
            <p style={{ marginTop: 0 }}>
              {t('fiscalBills.confirmRefundBody')}
            </p>
            <p>
              <strong>{t('fiscalBills.orderId')}</strong> {refundConfirmBill.orderId || t('common.dash')}<br />
              <strong>{t('fiscalBills.taInvoiceNoLabel')}:</strong> {refundConfirmBill.sdcInvoiceNumber || t('common.dash')}<br />
              <strong>{t('fiscalBills.invoiceType')}</strong> {t(`fiscalBills.invoiceTypes.${refundConfirmBill.invoiceType}`, { defaultValue: refundConfirmBill.invoiceType ?? t('common.dash') })}<br />
              <strong>{t('fiscalBills.transactionType')}</strong> {t(`fiscalBills.transactionTypes.${refundConfirmBill.transactionType}`, { defaultValue: refundConfirmBill.transactionType ?? t('common.dash') })}
            </p>
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={closeRefundConfirm}>{t('common.cancel')}</button>
              <button
                type="button"
                className="primary-button"
                onClick={confirmCreateRefund}
                disabled={Boolean(refundingBillIds[refundConfirmBill.fiscalbillId])}
              >
                {t('fiscalBills.confirmCreateRefund')}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  )
}

function formatDateRangeLabel(startDate, endDate, t) {
  if (!startDate && !endDate) return t('fiscalBills.dateRange.empty')
  if (startDate && !endDate) return t('fiscalBills.dateRange.openEnd', { start: formatDateOnly(startDate) })
  if (!startDate && endDate) return t('fiscalBills.dateRange.openStart', { end: formatDateOnly(endDate) })
  return t('fiscalBills.dateRange.range', { start: formatDateOnly(startDate), end: formatDateOnly(endDate) })
}

function formatDateOnly(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`
}

function formatDateTime(value) {
  if (!value) return i18n.t('common.dash')
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function toQrImageSrc(qrValue, fallbackValue) {
  const sourceValue = qrValue || fallbackValue
  if (!sourceValue) return ''
  if (/^data:image\//i.test(sourceValue)) return sourceValue
  if (/^https?:\/\//i.test(sourceValue)) return sourceValue
  if (looksLikeBase64Image(sourceValue)) return `data:image/gif;base64,${sourceValue}`
  return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(sourceValue)}`
}

function looksLikeBase64Image(value) {
  return typeof value === 'string'
    && value.length > 100
    && /^[A-Za-z0-9+/=\r\n]+$/.test(value)
}