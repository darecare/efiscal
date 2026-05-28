import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { fiscalBillApi, ordersApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const INVOICE_TYPE_VALUES = [0, 4]
const PAGE_SIZE_OPTIONS = [20, 50, 100]
const SHIPPING_STATUS_VALUES = ['awaiting', 'in_process', 'shipped', 'delivered', 'cancelled']

export default function Orders() {
  const { t } = useTranslation()
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
  const [selectedOrderIds, setSelectedOrderIds] = useState(new Set())

  const totalPages = Math.ceil(totalRecords / limit) || 1

  const [fiscalByOrderId, setFiscalByOrderId] = useState({})
  const [busyOrderIds, setBusyOrderIds] = useState({})

  // Fiscalize modal state
  const [fiscalModal, setFiscalModal] = useState(null) // { orders } or null
  const [fiscalInvoiceType, setFiscalInvoiceType] = useState(0)
  const [fiscalError, setFiscalError] = useState(null)
  const [fiscalSubmitting, setFiscalSubmitting] = useState(false)

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

  function toggleOrderSelection(orderId) {
    setSelectedOrderIds((prev) => {
      const next = new Set(prev)
      if (next.has(orderId)) next.delete(orderId)
      else next.add(orderId)
      return next
    })
  }

  function toggleSelectAllVisible() {
    const visibleIds = orders.map((o) => o.id)
    const allSelected = visibleIds.length > 0 && visibleIds.every((id) => selectedOrderIds.has(id))
    setSelectedOrderIds((prev) => {
      const next = new Set(prev)
      if (allSelected) {
        visibleIds.forEach((id) => next.delete(id))
      } else {
        visibleIds.forEach((id) => next.add(id))
      }
      return next
    })
  }

  async function fetchPage(page) {
    if (!selectedOrgId) { setError(t('orders.selectOrgFirst')); return }
    setError(null)
    setLoading(true)
    setHasFetched(true)
    setExpandedOrderIds(new Set())
    setSelectedOrderIds(new Set())
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
        setError(t('orders.noApiConfig'))
      } else {
        setError(msg || err.response?.data?.error || t('orders.fetchFailed'))
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

  function openFiscalModalForOrders(ordersToSubmit) {
    if (!ordersToSubmit || ordersToSubmit.length === 0) return
    setFiscalModal({ orders: ordersToSubmit })
    setFiscalInvoiceType(0)
    setFiscalError(null)
  }

  function openFiscalModal(order) {
    openFiscalModalForOrders([order])
  }

  function closeFiscalModal() {
    setFiscalModal(null)
    setFiscalError(null)
  }

  async function submitFiscalBill() {
    const ordersToSubmit = fiscalModal?.orders || []
    const selectedOrg = orgs.find(o => String(o.orgId) === String(selectedOrgId))
    if (!selectedOrg || !selectedOrg.clientId) {
      setFiscalError(t('orders.noOrgClient'))
      return
    }
    const clientId = selectedOrg.clientId

    setFiscalSubmitting(true)
    setFiscalError(null)

    const nextFiscalState = { ...fiscalByOrderId }
    const failures = []

    for (const order of ordersToSubmit) {
      const lines = order.orderLines || []
      const items = lines.length > 0 ? lines.map(line => {
        const parsedTaxValue = parseFloat(line.taxValue)
        const hasTaxValue = line.taxValue !== undefined
          && line.taxValue !== null
          && String(line.taxValue).trim() !== ''
          && Number.isFinite(parsedTaxValue)
        const taxPrefix = hasTaxValue
          ? String(Math.trunc(parsedTaxValue)).padStart(2, '0')
          : null
        return {
          name: line.productName || `Product ${line.productId}`,
          quantity: parseFloat(line.quantity) || 1,
          unitPrice: parseFloat(line.unitPrice) || 0,
          totalAmount: (parseFloat(line.quantity) || 1) * (parseFloat(line.unitPrice) || 0),
          taxLabel: null,
          labels: null,
          taxValue: hasTaxValue ? parsedTaxValue : null,
          taxCategoryName: line.taxCategoryName || null,
          taxPrefix,
          gtin: line.ean || null,
          productId: line.productId ? String(line.productId) : null,
          sku: line.sku || null,
        }
      }) : [{
        name: `Order ${order.externalOrderNo}`,
        quantity: 1,
        unitPrice: parseFloat(order.totalAmount) || 0,
        totalAmount: parseFloat(order.totalAmount) || 0,
        taxLabel: null,
        labels: null,
        taxValue: null,
        taxCategoryName: null,
        taxPrefix: null,
        gtin: null,
        productId: null,
        sku: null,
      }]

      const payload = {
        orderId: String(order.id),
        customerName: order.customerName || null,
        invoiceType: parseInt(fiscalInvoiceType),
        transactionType: 0,
        billingType: order.billingType || null,
        billingCompanyVat: order.billingCompanyVat || null,
        paymentMethodCode: order.paymentMethodCode || null,
        items,
      }

      try {
        const created = await fiscalBillApi.createFromOrder(
          payload, createIdempotencyKey(),
          Number(selectedOrgId), Number(clientId)
        )
        nextFiscalState[order.id] = {
          fiscalbillId: created.fiscalbillId,
          status: created.status,
          sdcInvoiceNumber: created.sdcInvoiceNumber,
          lastError: created.lastError,
        }
      } catch (err) {
        const msg = err?.response?.data?.message || err?.response?.data || err?.message || t('orders.fiscalFailed')
        nextFiscalState[order.id] = {
          status: 'ERROR',
          lastError: typeof msg === 'string' ? msg : JSON.stringify(msg),
        }
        failures.push(order.externalOrderNo || order.id)
      }
    }

    setFiscalByOrderId(nextFiscalState)
    setFiscalSubmitting(false)

    if (failures.length > 0) {
      setFiscalError(t('orders.fiscalFailedSummary', { count: failures.length, list: failures.join(', ') }))
      return
    }

    setSelectedOrderIds(new Set())
    closeFiscalModal()
  }

  async function retryFiscalBill(order) {
    const fiscal = fiscalByOrderId[order.id]
    if (!fiscal?.fiscalbillId) return
    setBusyOrderIds((current) => ({ ...current, [order.id]: true }))
    try {
      const retryResponse = await fiscalBillApi.retry(fiscal.fiscalbillId, createIdempotencyKey())
      setFiscalByOrderId((current) => ({
        ...current,
        [order.id]: { ...current[order.id], status: retryResponse.status, lastError: null },
      }))
    } catch (err) {
      setFiscalByOrderId((current) => ({
        ...current,
        [order.id]: { ...current[order.id], status: 'ERROR', lastError: err.response?.data?.message || t('orders.retryFailed') },
      }))
    } finally {
      setBusyOrderIds((current) => ({ ...current, [order.id]: false }))
    }
  }

  return (
    <AppShell
      title={t('orders.title')}
      subtitle={t('orders.subtitle')}
    >
      <form className="filters-panel" onSubmit={handleFetch}>
        <div className="filter-grid">
          <label className="field">
            <span>{t('orders.organization')}</span>
            <select
              value={selectedOrgId}
              onChange={(e) => setSelectedOrgId(e.target.value)}
              required
            >
              <option value="">{t('common.selectOrgPlaceholder')}</option>
              {orgs.map((o) => (
                <option key={o.orgId} value={o.orgId}>{o.name}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>{t('orders.dateFrom')}</span>
            <input
              type="date"
              value={createdAfter}
              onChange={(e) => setCreatedAfter(e.target.value)}
            />
          </label>
          <label className="field">
            <span>{t('orders.shippingStatus')}</span>
            <select
              value={shippingStatus}
              onChange={(e) => setShippingStatus(e.target.value)}
            >
              <option value="">{t('common.selectAllStatusesPlaceholder')}</option>
              {SHIPPING_STATUS_VALUES.map((s) => (
                <option key={s} value={s}>{t(`orders.shippingStatusLabels.${s}`)}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>{t('orders.limit')}</span>
            <select
              value={limit}
              onChange={(e) => setLimit(Number(e.target.value))}
            >
              {PAGE_SIZE_OPTIONS.map((n) => (
                <option key={n} value={n}>{t('common.perPage', { count: n })}</option>
              ))}
            </select>
          </label>
        </div>
        <div className="inline-actions">
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? t('orders.fetching') : t('orders.fetchOrders')}
          </button>
          {selectedOrderIds.size > 0 && (
            <button
              type="button"
              className="primary-button"
              onClick={() => openFiscalModalForOrders(orders.filter((o) => selectedOrderIds.has(o.id)))}
            >
              {t('orders.fiscalizeSelected', { count: selectedOrderIds.size })}
            </button>
          )}
          {hasFetched && <span className="badge">{t('common.counts.records', { count: totalRecords })}</span>}
        </div>
      </form>

      {error && <div className="error-banner">{error}</div>}

      {hasFetched && !loading && (
        <section className="table-card">
          <table>
            <thead>
              <tr>
                <th style={{ width: 32 }}>
                  <input
                    type="checkbox"
                    checked={orders.length > 0 && orders.every((o) => selectedOrderIds.has(o.id))}
                    onChange={toggleSelectAllVisible}
                    aria-label={t('orders.selectAllAria')}
                  />
                </th>
                <th className="col-expand"></th>
                <th>{t('orders.orderNo')}</th>
                <th>{t('orders.customer')}</th>
                <th>{t('common.status')}</th>
                <th>{t('orders.total')}</th>
                <th>{t('orders.lines')}</th>
                <th>{t('orders.createdAt')}</th>
                <th>{t('orders.fiscalStatus')}</th>
                <th>{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {orders.length === 0 ? (
                <tr>
                  <td colSpan={10} style={{ textAlign: 'center', opacity: 0.45, padding: '24px 0' }}>
                    {t('orders.noOrdersFound')}
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
                      <td onClick={(e) => e.stopPropagation()}>
                        <input
                          type="checkbox"
                          checked={selectedOrderIds.has(order.id)}
                          onChange={() => toggleOrderSelection(order.id)}
                          aria-label={t('orders.selectOrderAria', { orderNo: order.externalOrderNo })}
                        />
                      </td>
                      <td className="col-expand">
                        {lines.length > 0 && (
                          <button
                            type="button"
                            className="expand-toggle"
                            onClick={(e) => { e.stopPropagation(); toggleExpand(order.id) }}
                            aria-expanded={isExpanded}
                            aria-label={isExpanded ? t('orders.collapseLines') : t('orders.expandLines')}
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
                          ? <span className="lines-count">{t('common.counts.items', { count: lines.length })}</span>
                          : <span className="muted">{t('common.dash')}</span>}
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
                            onClick={() => openFiscalModal(order)}
                            disabled={busyOrderIds[order.id]}
                          >
                            {busyOrderIds[order.id] ? t('common.processing') : t('orders.issueFiscalBill')}
                          </button>
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => retryFiscalBill(order)}
                            disabled={busyOrderIds[order.id] || fiscalByOrderId[order.id]?.status !== 'FAILED'}
                          >
                            {t('common.retry')}
                          </button>
                        </div>
                      </td>
                    </tr>
                    {isExpanded && lines.length > 0 && (
                      <tr className="order-lines-row">
                        <td colSpan={10} className="order-lines-cell">
                          <table className="order-lines-table">
                            <colgroup>
                              <col className="col-product" />
                              <col className="col-sku" />
                              <col className="col-qty" />
                              <col className="col-price" />
                            </colgroup>
                            <thead>
                              <tr>
                                <th>{t('orders.product')}</th>
                                <th>{t('orders.sku')}</th>
                                <th>{t('orders.qty')}</th>
                                <th>{t('orders.unitPrice')}</th>
                              </tr>
                            </thead>
                            <tbody>
                              {lines.map((line, idx) => (
                                <tr key={line.productId || idx}>
                                  <td>{line.productName || t('common.dash')}</td>
                                  <td className="muted">{line.sku || t('common.dash')}</td>
                                  <td>{line.quantity || t('common.dash')}</td>
                                  <td>{line.unitPrice ? `${line.unitPrice} RSD` : t('common.dash')}</td>
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
                {t('common.prev')}
              </button>
              <span className="pagination-info">{t('common.paginationInfo', { current: currentPage, total: totalPages, records: totalRecords })}</span>
              <button
                type="button"
                className="secondary-button"
                onClick={() => fetchPage(currentPage + 1)}
                disabled={currentPage >= totalPages || loading}
              >
                {t('common.next')}
              </button>
            </div>
          )}
        </section>
      )}

      {fiscalModal && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.4)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
        }}>
          <div style={{ background: '#fff', borderRadius: 8, padding: '2rem', minWidth: 360, maxWidth: 480, width: '100%' }}>
            <h3 style={{ marginTop: 0 }}>{t('orders.issueFiscalBillTitle')}</h3>
            <p style={{ color: '#64748b', marginTop: 0 }}>
              {t('orders.ordersSelected')} <strong>{fiscalModal.orders.length}</strong>
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <div className="form-group">
                <label className="form-label">{t('orders.invoiceType')}</label>
                <select className="form-input" value={fiscalInvoiceType} onChange={(e) => setFiscalInvoiceType(e.target.value)}>
                  {INVOICE_TYPE_VALUES.map((v) => <option key={v} value={v}>{t(`orders.invoiceTypes.${v}`)}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">{t('orders.transactionType')}</label>
                <input className="form-input" value={t('orders.transactionTypeSale')} disabled readOnly />
              </div>
            </div>
            {fiscalError && <p style={{ color: 'red', marginTop: '0.75rem' }}>{fiscalError}</p>}
            <div style={{ marginTop: '1.25rem', display: 'flex', gap: '0.75rem' }}>
              <button className="primary-button" onClick={submitFiscalBill} disabled={fiscalSubmitting}>
                {fiscalSubmitting ? t('common.submitting') : t('common.submit')}
              </button>
              <button className="secondary-button" onClick={closeFiscalModal} disabled={fiscalSubmitting}>{t('common.cancel')}</button>
            </div>
          </div>
        </div>
      )}

    </AppShell>
  )
}

