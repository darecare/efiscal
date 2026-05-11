import React, { useEffect, useMemo, useRef, useState } from 'react'
import AppShell from '../components/AppShell'
import { fiscalBillApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { DateRangePicker } from 'react-date-range'
import 'react-date-range/dist/styles.css'
import 'react-date-range/dist/theme/default.css'

const PAYMENT_TYPE_LABELS = {
  0: 'Other',
  1: 'Cash',
  2: 'Card',
  3: 'Check',
  4: 'Wire Transfer',
  5: 'Voucher',
  6: 'Mobile Money',
}

const INVOICE_TYPE_OPTIONS = [
  { value: 0, label: 'Normal' },
  { value: 2, label: 'Copy' },
  { value: 4, label: 'Advance' },
]

const INVOICE_TYPE_LABELS = {
  0: 'Normal',
  2: 'Copy',
  4: 'Advance',
}

const TRANSACTION_TYPE_OPTIONS = [
  { value: 0, label: 'Sale' },
  { value: 1, label: 'Refund' },
]

const TRANSACTION_TYPE_LABELS = {
  0: 'Sale',
  1: 'Refund',
}

export default function FiscalBills() {
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [orgs, setOrgs] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState('')
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
  const [copyingBillIds, setCopyingBillIds] = useState({})
  const [refundingBillIds, setRefundingBillIds] = useState({})
  const [copyConfirmBill, setCopyConfirmBill] = useState(null)
  const [refundConfirmBill, setRefundConfirmBill] = useState(null)
  const [refundAlreadyExistsBill, setRefundAlreadyExistsBill] = useState(null)
  const [successMsg, setSuccessMsg] = useState('')

  const PAGE_SIZE = 50
  const [tablePage, setTablePage] = useState(1)

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
    const loadOrgs = isSuperAdmin ? orgsApi.list() : orgsApi.myAccess()
    loadOrgs
      .then((list) => {
        setOrgs(list)
        if (list.length === 1) {
          setSelectedOrgId(String(list[0].orgId))
        }
      })
      .catch(() => setOrgs([]))
  }, [isSuperAdmin])

  async function handleLoadFiscalBills(e) {
    if (e) e.preventDefault()
    if (!selectedOrgId) {
      setError('Please select an organization.')
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
      const data = await fiscalBillApi.list(Number(selectedOrgId))
      setFiscalBills(data)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || 'Failed to load fiscal bills.'
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

  const selectedDateRangeLabel = formatDateRangeLabel(dateRange[0].startDate, dateRange[0].endDate)

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
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || 'Failed to load fiscal bill details.'
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
      setSuccessMsg(`Copy created successfully (ID: ${created.fiscalbillId})`)
      await handleLoadFiscalBills()
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data?.lastError || err?.response?.data || err?.message || 'Failed to create copy fiscal bill.'
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
      setSuccessMsg(`Refund created successfully (ID: ${created.fiscalbillId})`)
      await handleLoadFiscalBills()
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data?.lastError || err?.response?.data || err?.message || 'Failed to create refund fiscal bill.'
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
    <AppShell title="Fiscal Bills" subtitle="Browse fiscal bills, tax rows, and payment rows">
      {successMsg ? <div className="success-banner" style={{ marginBottom: '1rem' }}>{successMsg}</div> : null}
      {error ? <div className="error-banner" style={{ marginBottom: '1rem' }}>{error}</div> : null}

      <form className="filters-panel" onSubmit={handleLoadFiscalBills} style={{ marginBottom: '1rem' }}>
        <div className="filter-grid">
          <label className="field">
            <span>Organization</span>
            <select
              value={selectedOrgId}
              onChange={(e) => setSelectedOrgId(e.target.value)}
              required
            >
              <option value="">— Select organization —</option>
              {orgs.map((org) => (
                <option key={org.orgId} value={org.orgId}>{org.name}</option>
              ))}
            </select>
          </label>
          <label className="field fiscal-date-range-field" ref={dateRangeFieldRef}>
            <span>TA Date Range</span>
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
                  moveRangeOnFirstSelection={false}
                  direction="horizontal"
                  showDateDisplay={false}
                  months={1}
                />
                <div className="fiscal-date-range-actions">
                  <button type="button" className="secondary-button" onClick={clearDateRange}>Clear</button>
                  <button type="button" className="primary-button" onClick={() => setIsDatePickerOpen(false)}>Apply</button>
                </div>
              </div>
            )}
          </label>
          <label className="field">
            <span>TA Invoice No</span>
            <input
              type="text"
              placeholder="e.g. A9ZA2WRE-..."
              value={filterInvoiceNo}
              onChange={(e) => setFilterInvoiceNo(e.target.value)}
            />
          </label>
          <label className="field">
            <span>Order ID</span>
            <input
              type="text"
              placeholder="Search by order ID..."
              value={filterOrderId}
              onChange={(e) => setFilterOrderId(e.target.value)}
            />
          </label>
          <label className="field">
            <span>Invoice Type</span>
            <select
              value={filterInvoiceType}
              onChange={(e) => setFilterInvoiceType(e.target.value)}
            >
              <option value="">— All types —</option>
              {INVOICE_TYPE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Transaction Type</span>
            <select
              value={filterTransactionType}
              onChange={(e) => setFilterTransactionType(e.target.value)}
            >
              <option value="">— All —</option>
              {TRANSACTION_TYPE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Customer Name</span>
            <input
              type="text"
              placeholder="Search by name..."
              value={filterCustomerName}
              onChange={(e) => setFilterCustomerName(e.target.value)}
            />
          </label>
        </div>
        <div className="inline-actions">
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? 'Loading...' : 'Load Fiscal Bills'}
          </button>
          {hasFetched && (
            <span className="badge">
              {filteredBills.length}{filteredBills.length !== fiscalBills.length ? ` of ${fiscalBills.length}` : ''} records
            </span>
          )}
        </div>
      </form>

      <div className="card" style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginBottom: '0.75rem' }}>Fiscal Bill List</h3>
        {!hasFetched ? (
          <div className="empty-state">
            <p>Select an organization and load fiscal bills.</p>
          </div>
        ) : filteredBills.length === 0 ? (
          <div className="empty-state">
            <p>{fiscalBills.length === 0 ? 'No fiscal bills found for the selected organization.' : 'No fiscal bills match the current filters.'}</p>
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
                &lsaquo; Prev
              </button>
              <span className="pagination-info">
                Page {tablePage} of {tablePageCount} &mdash; {filteredBills.length} records
              </span>
              <button
                type="button"
                className="secondary-button"
                onClick={() => setTablePage((p) => Math.min(tablePageCount, p + 1))}
                disabled={tablePage >= tablePageCount}
              >
                Next &rsaquo;
              </button>
            </div>
          )}
          <table className="data-table fiscal-bills-table">
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Status</th>
                <th>Customer</th>
                <th>Invoice Type</th>
                <th>Transaction Type</th>
                <th>TA Invoice No</th>
                <th>Total</th>
                <th>Created</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {pagedBills.map((bill) => (
                <tr key={bill.fiscalbillId}>
                  <td>{bill.orderId || '—'}</td>
                  <td>{bill.status}</td>
                  <td>{bill.customerName || '—'}</td>
                  <td>{INVOICE_TYPE_LABELS[bill.invoiceType] || bill.invoiceType || '—'}</td>
                  <td>{TRANSACTION_TYPE_LABELS[bill.transactionType] || bill.transactionType || '—'}</td>
                  <td>{bill.sdcInvoiceNumber || '—'}</td>
                  <td className="cell-right">{bill.totalAmount ?? '—'}</td>
                  <td>{formatDateTime(bill.createdAt)}</td>
                  <td>
                    <details className="action-dropdown">
                      <summary className="secondary-button">Actions</summary>
                      <div className="action-dropdown-menu">
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={() => openDetailsModal(bill.fiscalbillId)}
                        >
                          View details
                        </button>
                        {(bill.invoiceType === 0 || bill.invoiceType === 4) && (
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => openCopyConfirm(bill)}
                            disabled={Boolean(copyingBillIds[bill.fiscalbillId])}
                          >
                            {copyingBillIds[bill.fiscalbillId] ? 'Creating Copy...' : 'Create Copy'}
                          </button>
                        )}
                        {canCreateRefund(bill) && (
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => openRefundConfirm(bill)}
                            disabled={Boolean(refundingBillIds[bill.fiscalbillId])}
                          >
                            {refundingBillIds[bill.fiscalbillId] ? 'Creating Refund...' : 'Create Refund'}
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
                &lsaquo; Prev
              </button>
              <span className="pagination-info">
                Page {tablePage} of {tablePageCount} &mdash; {filteredBills.length} records
              </span>
              <button
                type="button"
                className="secondary-button"
                onClick={() => setTablePage((p) => Math.min(tablePageCount, p + 1))}
                disabled={tablePage >= tablePageCount}
              >
                Next &rsaquo;
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
              <h3 style={{ margin: 0 }}>Fiscal Bill Details</h3>
              <button type="button" className="secondary-button" onClick={closeDetailsModal}>Close</button>
            </div>

            {detailsError ? <div className="error-banner" style={{ marginBottom: '1rem' }}>{detailsError}</div> : null}
            {detailsLoading ? <p>Loading details...</p> : null}

            {!detailsLoading && details?.fiscalBill && (
              <>
                <div className="fiscalbills-summary-grid">
                  <div><strong>Fiscal Bill ID:</strong> {details.fiscalBill.fiscalbillId}</div>
                  <div><strong>Status:</strong> {details.fiscalBill.status}</div>
                  <div><strong>Order ID:</strong> {details.fiscalBill.orderId || '—'}</div>
                  <div><strong>TA Invoice No:</strong> {details.fiscalBill.sdcInvoiceNumber || '—'}</div>
                  <div><strong>Created:</strong> {formatDateTime(details.fiscalBill.createdAt)}</div>
                  <div><strong>Updated:</strong> {formatDateTime(details.fiscalBill.updatedAt)}</div>
                  <div className="fiscalbills-link-row">
                    <strong>Verification Link:</strong>
                    {details.fiscalBill.efiscalLink ? (
                      <a
                        href={details.fiscalBill.efiscalLink}
                        target="_blank"
                        rel="noreferrer"
                        className="icon-link"
                        title="Open verification link"
                      >
                        <span className="external-link-icon" aria-hidden="true"></span>
                      </a>
                    ) : ' —'}
                  </div>
                </div>

                {details.fiscalBill.efiscalQr || details.fiscalBill.efiscalLink ? (
                  <div className="fiscalbills-qr-wrap">
                    <img
                      src={toQrImageSrc(details.fiscalBill.efiscalQr, details.fiscalBill.efiscalLink)}
                      alt="Fiscal bill QR"
                      className="fiscalbills-qr-image"
                    />
                  </div>
                ) : null}

                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
                  <button
                    className={activeTab === 'tax' ? 'primary-button' : 'secondary-button'}
                    onClick={() => setActiveTab('tax')}
                    type="button"
                  >
                    Tax Items
                  </button>
                  <button
                    className={activeTab === 'lines' ? 'primary-button' : 'secondary-button'}
                    onClick={() => setActiveTab('lines')}
                    type="button"
                  >
                    Line Items
                  </button>
                  <button
                    className={activeTab === 'payment' ? 'primary-button' : 'secondary-button'}
                    onClick={() => setActiveTab('payment')}
                    type="button"
                  >
                    Payment Items
                  </button>
                </div>

                {activeTab === 'tax' ? (
                  <table className="data-table fiscal-bills-table">
                    <thead>
                      <tr>
                        <th>Tax Label</th>
                        <th>Category</th>
                        <th>Category Type</th>
                        <th>Rate</th>
                        <th>Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {details.taxItems.length === 0 ? (
                        <tr>
                          <td colSpan="5">No tax items stored for this fiscal bill.</td>
                        </tr>
                      ) : details.taxItems.map((item) => (
                        <tr key={item.fiscalbilltaxId}>
                          <td>{item.taxLabel || '—'}</td>
                          <td>{item.categoryName || '—'}</td>
                          <td>{item.categoryType ?? '—'}</td>
                          <td className="cell-right">{item.rate ?? '—'}</td>
                          <td className="cell-right">{item.amount ?? '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : activeTab === 'lines' ? (
                  <table className="data-table fiscal-bills-table">
                    <thead>
                      <tr>
                        <th>Name</th>
                        <th>Qty</th>
                        <th>Unit Price</th>
                        <th>Total</th>
                        <th>Tax Label</th>
                        <th>GTIN</th>
                        <th>Product ID</th>
                        <th>SKU</th>
                      </tr>
                    </thead>
                    <tbody>
                      {details.lineItems.length === 0 ? (
                        <tr>
                          <td colSpan="8">No line items stored for this fiscal bill.</td>
                        </tr>
                      ) : details.lineItems.map((item) => (
                        <tr key={item.fiscalbilllineId}>
                          <td>{item.name || '—'}</td>
                          <td className="cell-right">{item.quantity ?? '—'}</td>
                          <td className="cell-right">{item.unitPrice ?? '—'}</td>
                          <td className="cell-right">{item.totalAmount ?? '—'}</td>
                          <td>{item.taxLabel || '—'}</td>
                          <td>{item.gtin || '—'}</td>
                          <td>{item.productId || '—'}</td>
                          <td>{item.sku || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <table className="data-table fiscal-bills-table">
                    <thead>
                      <tr>
                        <th>Payment Type</th>
                        <th>Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {details.payments.length === 0 ? (
                        <tr>
                          <td colSpan="2">No payment rows stored for this fiscal bill.</td>
                        </tr>
                      ) : details.payments.map((payment) => (
                        <tr key={payment.fiscalbillpayId}>
                          <td>{PAYMENT_TYPE_LABELS[payment.paymentType] || payment.paymentType}</td>
                          <td className="cell-right">{payment.amount ?? '—'}</td>
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
              <h3>Confirm Create Copy</h3>
              <button type="button" className="modal-close" onClick={closeCopyConfirm}>×</button>
            </div>
            <p style={{ marginTop: 0 }}>
              Create a Copy fiscal bill for this document?
            </p>
            <p>
              <strong>Order ID:</strong> {copyConfirmBill.orderId || '—'}<br />
              <strong>TA Invoice No:</strong> {copyConfirmBill.sdcInvoiceNumber || '—'}<br />
              <strong>Transaction Type:</strong> {TRANSACTION_TYPE_LABELS[copyConfirmBill.transactionType] || copyConfirmBill.transactionType || '—'}
            </p>
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={closeCopyConfirm}>Cancel</button>
              <button
                type="button"
                className="primary-button"
                onClick={confirmCreateCopy}
                disabled={Boolean(copyingBillIds[copyConfirmBill.fiscalbillId])}
              >
                Confirm Create Copy
              </button>
            </div>
          </div>
        </div>
      )}

      {refundAlreadyExistsBill && (
        <div className="modal-overlay" onClick={() => setRefundAlreadyExistsBill(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Cannot Create Refund</h3>
              <button type="button" className="modal-close" onClick={() => setRefundAlreadyExistsBill(null)}>×</button>
            </div>
            <p style={{ marginTop: 0 }}>Fiscal bill already has issued refund.</p>
            <p>
              <strong>Order ID:</strong> {refundAlreadyExistsBill.orderId || '—'}<br />
              <strong>TA Invoice No:</strong> {refundAlreadyExistsBill.sdcInvoiceNumber || '—'}
            </p>
            <div className="modal-actions">
              <button type="button" className="primary-button" onClick={() => setRefundAlreadyExistsBill(null)}>OK</button>
            </div>
          </div>
        </div>
      )}

      {refundConfirmBill && (
        <div className="modal-overlay" onClick={closeRefundConfirm}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Confirm Create Refund</h3>
              <button type="button" className="modal-close" onClick={closeRefundConfirm}>×</button>
            </div>
            <p style={{ marginTop: 0 }}>
              Create a Refund fiscal bill for this document?
            </p>
            <p>
              <strong>Order ID:</strong> {refundConfirmBill.orderId || '—'}<br />
              <strong>TA Invoice No:</strong> {refundConfirmBill.sdcInvoiceNumber || '—'}<br />
              <strong>Invoice Type:</strong> {INVOICE_TYPE_LABELS[refundConfirmBill.invoiceType] || refundConfirmBill.invoiceType || '—'}<br />
              <strong>Transaction Type:</strong> {TRANSACTION_TYPE_LABELS[refundConfirmBill.transactionType] || refundConfirmBill.transactionType || '—'}
            </p>
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={closeRefundConfirm}>Cancel</button>
              <button
                type="button"
                className="primary-button"
                onClick={confirmCreateRefund}
                disabled={Boolean(refundingBillIds[refundConfirmBill.fiscalbillId])}
              >
                Confirm Create Refund
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  )
}

function formatDateRangeLabel(startDate, endDate) {
  if (!startDate && !endDate) return 'Select date range'
  if (startDate && !endDate) return `${formatDateOnly(startDate)} - ...`
  if (!startDate && endDate) return `... - ${formatDateOnly(endDate)}`
  return `${formatDateOnly(startDate)} - ${formatDateOnly(endDate)}`
}

function formatDateOnly(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`
}

function formatDateTime(value) {
  if (!value) return '—'
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