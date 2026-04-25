import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { fiscalBillApi, ordersApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'



const PAGE_SIZE_OPTIONS = [20, 50, 100]

const SHIPPING_STATUSES = [
  { value: 'awaiting', label: 'Awaiting' },
  { value: 'in_process', label: 'Processing' },
  { value: 'shipped', label: 'Shipped' },  
  { value: 'delivered', label: 'Delivered' },
  { value: 'cancelled', label: 'Cancelled' },
]

export default function Orders() {
  const { user: currentUser } = useAuth()

  const [orgs, setOrgs] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState('')
  const [createdAfter, setCreatedAfter] = useState('')
  const [shippingStatus, setShippingStatus] = useState('awaiting')

  const [orders, setOrders] = useState([])
  const [totalRecords, setTotalRecords] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [hasFetched, setHasFetched] = useState(false)

  const [limit, setLimit] = useState(100)
  const [currentPage, setCurrentPage] = useState(1)

  // Expandable order lines state
  const [expandedOrderIds, setExpandedOrderIds] = useState(new Set())

  const totalPages = Math.ceil(totalRecords / limit) || 1

  const [fiscalByOrderId, setFiscalByOrderId] = useState({})
  const [busyOrderIds, setBusyOrderIds] = useState({})

  // Load accessible orgs on mount
  useEffect(() => {
    orgsApi.myAccess()
      .then((list) => {
        setOrgs(list)
        if (list.length === 1) setSelectedOrgId(String(list[0].orgId))
      })
      .catch(() => setOrgs([]))
  }, [])

  function toggleExpand(orderId) {
    setExpandedOrderIds((prev) => {
      const next = new Set(prev)
      if (next.has(orderId)) next.delete(orderId)
      else next.add(orderId)
      return next
    })
  }

  async function fetchPage(page) {
    if (!selectedOrgId) { setError('Please select an organization first'); return }
    setError(null)
    setLoading(true)
    setHasFetched(true)
    setExpandedOrderIds(new Set())
    const start = (page - 1) * limit
    try {
      const result = await ordersApi.fetch({
        orgId: Number(selectedOrgId),
        createdAfter,
        shippingStatus,
        start,
        limit,
      })
      setOrders(result.data || [])
      setTotalRecords(result.meta?.total ?? (result.data?.length ?? 0))
      setCurrentPage(page)
    } catch (err) {
      const status = err.response?.status
      const msg = err.response?.data?.message
      if (status === 404 && msg) {
        setError(msg)
      } else if (status === 404) {
        setError('No API configuration found for this organization. Please set up a MerchantPro connection in API Config.')
      } else {
        setError(msg || err.response?.data?.error || 'Failed to fetch orders')
      }
      setOrders([])
      setTotalRecords(0)
    } finally {
      setLoading(false)
    }
  }

  async function handleFetch(event) {
    event.preventDefault()
    await fetchPage(1)
  }

  function createIdempotencyKey() {
    if (window.crypto && window.crypto.randomUUID) return window.crypto.randomUUID()
    return `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`
  }

  async function issueFiscalBill(order) {
    setBusyOrderIds((current) => ({ ...current, [order.id]: true }))
    try {
      const created = await fiscalBillApi.create(
        {
          OrderId: order.id,
          customer: {
            name: order.customerName,
          },
          items: [
            {
              sku: `SKU-${order.id}`,
              name: `Order ${order.externalOrderNo}`,
              quantity: 1,
              unitPrice: Number(order.totalAmount),
              taxRate: 20,
            },
          ],
          currency: 'RSD',
          paymentMethod: 'CARD',
        },
        createIdempotencyKey(),
      )

      const latestStatus = await fiscalBillApi.status(created.fiscalDocumentId)
      setFiscalByOrderId((current) => ({
        ...current,
        [order.id]: {
          fiscalDocumentId: created.fiscalDocumentId,
          status: latestStatus.status,
          lastError: latestStatus.lastError,
          attemptCount: latestStatus.attemptCount,
        },
      }))
    } catch (error) {
      setFiscalByOrderId((current) => ({
        ...current,
        [order.id]: {
          status: 'ERROR',
          lastError: error.response?.data?.message || 'Failed to issue fiscal bill',
        },
      }))
    } finally {
      setBusyOrderIds((current) => ({ ...current, [order.id]: false }))
    }
  }

  async function retryFiscalBill(order) {
    const fiscal = fiscalByOrderId[order.id]
    if (!fiscal?.fiscalDocumentId) {
      return
    }
    setBusyOrderIds((current) => ({ ...current, [order.id]: true }))
    try {
      const retryResponse = await fiscalBillApi.retry(fiscal.fiscalDocumentId, createIdempotencyKey())
      setFiscalByOrderId((current) => ({
        ...current,
        [order.id]: {
          ...current[order.id],
          status: retryResponse.status,
          lastError: null,
          attemptCount: (current[order.id]?.attemptCount || 1) + 1,
        },
      }))
    } catch (error) {
      setFiscalByOrderId((current) => ({
        ...current,
        [order.id]: {
          ...current[order.id],
          status: 'ERROR',
          lastError: error.response?.data?.message || 'Retry failed',
        },
      }))
    } finally {
      setBusyOrderIds((current) => ({ ...current, [order.id]: false }))
    }
  }

  return (
    <AppShell
      title="MerchantPro Orders"
      subtitle="Fetch orders via configured API connection and template."
    >
      <form className="filters-panel" onSubmit={handleFetch}>
        <div className="filter-grid">
          <label className="field">
            <span>Organization</span>
            <select
              value={selectedOrgId}
              onChange={(e) => setSelectedOrgId(e.target.value)}
              required
            >
              <option value="">— Select organization —</option>
              {orgs.map((o) => (
                <option key={o.orgId} value={o.orgId}>{o.name}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Date From</span>
            <input
              type="date"
              value={createdAfter}
              onChange={(e) => setCreatedAfter(e.target.value)}
            />
          </label>
          <label className="field">
            <span>Shipping Status</span>
            <select
              value={shippingStatus}
              onChange={(e) => setShippingStatus(e.target.value)}
            >
              <option value="">— All statuses —</option>
              {SHIPPING_STATUSES.map((s) => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Limit</span>
            <select
              value={limit}
              onChange={(e) => setLimit(Number(e.target.value))}
            >
              {PAGE_SIZE_OPTIONS.map((n) => (
                <option key={n} value={n}>{n} per page</option>
              ))}
            </select>
          </label>
        </div>
        <div className="inline-actions">
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? 'Fetching…' : 'Fetch Orders'}
          </button>
          {hasFetched && <span className="badge">{totalRecords} records</span>}
        </div>
      </form>

      {error && <div className="error-banner">{error}</div>}

      {hasFetched && !loading && (
        <section className="table-card">
          <table>
            <thead>
              <tr>
                <th className="col-expand"></th>
                <th>Order No</th>
                <th>Customer</th>
                <th>Status</th>
                <th>Total</th>
                <th>Lines</th>
                <th>Created At</th>
                <th>Fiscal Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {orders.length === 0 ? (
                <tr>
                  <td colSpan={9} style={{ textAlign: 'center', opacity: 0.45, padding: '24px 0' }}>
                    No orders found for the selected filters
                  </td>
                </tr>
              ) : orders.map((order) => {
                const isExpanded = expandedOrderIds.has(order.id)
                const lines = order.orderLines || []
                return (
                  <React.Fragment key={order.id}>
                    <tr
                      className={`order-summary-row${isExpanded ? ' expanded' : ''}`}
                      onClick={() => lines.length > 0 && toggleExpand(order.id)}
                      style={{ cursor: lines.length > 0 ? 'pointer' : 'default' }}
                    >
                      <td className="col-expand">
                        {lines.length > 0 && (
                          <button
                            type="button"
                            className="expand-toggle"
                            onClick={(e) => { e.stopPropagation(); toggleExpand(order.id) }}
                            aria-expanded={isExpanded}
                            aria-label={isExpanded ? 'Collapse order lines' : 'Expand order lines'}
                          >
                            {isExpanded ? '▼' : '▶'}
                          </button>
                        )}
                      </td>
                      <td>{order.externalOrderNo}</td>
                      <td>{order.customerName}</td>
                      <td>{order.shippingStatus}</td>
                      <td>{order.totalAmount} RSD</td>
                      <td>
                        {lines.length > 0
                          ? <span className="lines-count">{lines.length} item{lines.length !== 1 ? 's' : ''}</span>
                          : <span className="muted">—</span>}
                      </td>
                      <td>{order.createdAt}</td>
                      <td>
                        <span className="badge">{fiscalByOrderId[order.id]?.status || 'NOT_SUBMITTED'}</span>
                        {fiscalByOrderId[order.id]?.lastError
                          ? <p className="error-text fiscal-error">{fiscalByOrderId[order.id].lastError}</p>
                          : null}
                      </td>
                      <td>
                        <div className="inline-actions" onClick={(e) => e.stopPropagation()}>
                          <button
                            type="button"
                            className="primary-button"
                            onClick={() => issueFiscalBill(order)}
                            disabled={busyOrderIds[order.id]}
                          >
                            {busyOrderIds[order.id] ? 'Processing...' : 'Issue Fiscal Bill'}
                          </button>
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => retryFiscalBill(order)}
                            disabled={busyOrderIds[order.id] || fiscalByOrderId[order.id]?.status !== 'FAILED'}
                          >
                            Retry
                          </button>
                        </div>
                      </td>
                    </tr>
                    {isExpanded && lines.length > 0 && (
                      <tr className="order-lines-row">
                        <td colSpan={9} className="order-lines-cell">
                          <table className="order-lines-table">
                            <colgroup>
                              <col className="col-product" />
                              <col className="col-sku" />
                              <col className="col-qty" />
                              <col className="col-price" />
                            </colgroup>
                            <thead>
                              <tr>
                                <th>Product</th>
                                <th>SKU</th>
                                <th>Qty</th>
                                <th>Unit Price</th>
                              </tr>
                            </thead>
                            <tbody>
                              {lines.map((line, idx) => (
                                <tr key={line.productId || idx}>
                                  <td>{line.productName || '—'}</td>
                                  <td className="muted">{line.sku || '—'}</td>
                                  <td>{line.quantity || '—'}</td>
                                  <td>{line.unitPrice ? `${line.unitPrice} RSD` : '—'}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                )
              })}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="pagination">
              <button
                type="button"
                className="secondary-button"
                onClick={() => fetchPage(currentPage - 1)}
                disabled={currentPage === 1 || loading}
              >
                &lsaquo; Prev
              </button>
              <span className="pagination-info">Page {currentPage} of {totalPages} &mdash; {totalRecords} records</span>
              <button
                type="button"
                className="secondary-button"
                onClick={() => fetchPage(currentPage + 1)}
                disabled={currentPage >= totalPages || loading}
              >
                Next &rsaquo;
              </button>
            </div>
          )}
        </section>
      )}
    </AppShell>
  )
}

