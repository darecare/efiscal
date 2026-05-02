import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { fiscalBillApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const PAYMENT_TYPE_LABELS = {
  0: 'Other',
  1: 'Cash',
  2: 'Card',
  3: 'Check',
  4: 'Wire Transfer',
  5: 'Voucher',
  6: 'Mobile Money',
}

const INVOICE_TYPE_LABELS = {
  0: 'Normal',
  4: 'Advance',
}

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
  const [selectedFiscalBillId, setSelectedFiscalBillId] = useState('')
  const [detailsLoading, setDetailsLoading] = useState(false)
  const [detailsError, setDetailsError] = useState(null)
  const [details, setDetails] = useState(null)
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false)
  const [activeTab, setActiveTab] = useState('tax')

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

  async function handleLoadFiscalBills() {
    if (!selectedOrgId) {
      setError('Please select an organization.')
      return
    }

    setLoading(true)
    setError(null)
    setDetails(null)
    setDetailsError(null)
    setSelectedFiscalBillId('')
    try {
      const data = await fiscalBillApi.list(Number(selectedOrgId))
      setFiscalBills(data)
      if (data.length === 0) setDetails(null)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || 'Failed to load fiscal bills.'
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
      setFiscalBills([])
    } finally {
      setLoading(false)
    }
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
    setActiveTab('tax')
    setIsDetailsModalOpen(true)
    await handleSelectFiscalBill(fiscalbillId)
  }

  function closeDetailsModal() {
    setIsDetailsModalOpen(false)
    setDetailsError(null)
  }

  const topActions = (
    <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
      <select
        value={selectedOrgId}
        onChange={(e) => setSelectedOrgId(e.target.value)}
        className="form-input"
        style={{ minWidth: '220px' }}
      >
        <option value="">Select organization...</option>
        {orgs.map((org) => (
          <option key={org.orgId} value={org.orgId}>{org.name}</option>
        ))}
      </select>
      <button className="primary-button" onClick={handleLoadFiscalBills} disabled={loading}>
        {loading ? 'Loading...' : 'Load Fiscal Bills'}
      </button>
    </div>
  )

  return (
    <AppShell title="Fiscal Bills" subtitle="Browse fiscal bills, tax rows, and payment rows" actions={topActions}>
      {error ? <div className="error-banner" style={{ marginBottom: '1rem' }}>{error}</div> : null}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginBottom: '0.75rem' }}>Fiscal Bill List</h3>
        {fiscalBills.length === 0 ? (
          <div className="empty-state">
            <p>Select an organization and load fiscal bills.</p>
          </div>
        ) : (
          <table className="data-table fiscal-bills-table">
            <thead>
              <tr>
                <th>Fiscal Bill ID</th>
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
              {fiscalBills.map((bill) => (
                <tr key={bill.fiscalbillId}>
                  <td>{bill.fiscalbillId}</td>
                  <td>{bill.orderId || '—'}</td>
                  <td>{bill.status}</td>
                  <td>{bill.customerName || '—'}</td>
                  <td>{INVOICE_TYPE_LABELS[bill.invoiceType] || bill.invoiceType || '—'}</td>
                  <td>{TRANSACTION_TYPE_LABELS[bill.transactionType] || bill.transactionType || '—'}</td>
                  <td>{bill.sdcInvoiceNumber || '—'}</td>
                  <td className="cell-right">{bill.totalAmount ?? '—'}</td>
                  <td>{formatDateTime(bill.createdAt)}</td>
                  <td>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => openDetailsModal(bill.fiscalbillId)}
                    >
                      View details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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
                        <th>ID</th>
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
                          <td colSpan="6">No tax items stored for this fiscal bill.</td>
                        </tr>
                      ) : details.taxItems.map((item) => (
                        <tr key={item.fiscalbilltaxId}>
                          <td>{item.fiscalbilltaxId}</td>
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
                        <th>ID</th>
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
                          <td colSpan="9">No line items stored for this fiscal bill.</td>
                        </tr>
                      ) : details.lineItems.map((item) => (
                        <tr key={item.fiscalbilllineId}>
                          <td>{item.fiscalbilllineId}</td>
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
                        <th>ID</th>
                        <th>Payment Type</th>
                        <th>Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {details.payments.length === 0 ? (
                        <tr>
                          <td colSpan="3">No payment rows stored for this fiscal bill.</td>
                        </tr>
                      ) : details.payments.map((payment) => (
                        <tr key={payment.fiscalbillpayId}>
                          <td>{payment.fiscalbillpayId}</td>
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
    </AppShell>
  )
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