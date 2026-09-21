import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { fiscalBillApi, ordersApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { useOrg } from '../contexts/OrgContext'
import {
  ADVANCE_PAYMENT_MAX_DAYS_IN_PAST,
  advancePaymentDateTimeBounds,
  BUYER_COST_CENTER_TYPE_VALUES,
  composeBuyerCostCenterId,
  isAdvanceSale,
  validateAdvancePaymentDateTime,
} from './createFiscalBillUtils'

const INVOICE_TYPE_VALUES = [0, 1, 3, 4]
const TRANSACTION_TYPE_VALUES = [0, 1]
const PAGE_SIZE_OPTIONS = [20, 50, 100]
const SHIPPING_STATUS_VALUES = ['awaiting', 'in_process', 'shipped', 'delivered', 'cancelled']

function emailStatusLabel(status, t) {
  switch (status) {
    case 'SENT':
      return t('orders.emailSent')
    case 'FAILED':
      return t('orders.emailFailed')
    case 'SKIPPED':
      return t('orders.emailSkipped')
    case 'NOT_REQUESTED':
      return t('orders.emailNotRequested')
    default:
      return null
  }
}

function buildFiscalCreatedNotification(successes, t) {
  if (successes.length === 1) {
    const emailPart = emailStatusLabel(successes[0].emailStatus, t)
    return emailPart
      ? `${t('orders.fiscalCreatedSuccess')} ${emailPart}`
      : t('orders.fiscalCreatedSuccess')
  }

  const emailCounts = { SENT: 0, FAILED: 0, SKIPPED: 0, NOT_REQUESTED: 0 }
  successes.forEach((s) => {
    if (s.emailStatus && emailCounts[s.emailStatus] !== undefined) {
      emailCounts[s.emailStatus] += 1
    }
  })
  const emailParts = []
  if (emailCounts.SENT > 0) emailParts.push(t('orders.emailSentCount', { count: emailCounts.SENT }))
  if (emailCounts.FAILED > 0) emailParts.push(t('orders.emailFailedCount', { count: emailCounts.FAILED }))
  if (emailCounts.SKIPPED > 0) emailParts.push(t('orders.emailSkippedCount', { count: emailCounts.SKIPPED }))
  if (emailCounts.NOT_REQUESTED > 0) {
    emailParts.push(t('orders.emailNotRequestedCount', { count: emailCounts.NOT_REQUESTED }))
  }

  const base = t('orders.fiscalCreatedSuccessCount', { count: successes.length })
  return emailParts.length > 0 ? `${base} ${emailParts.join(' ')}` : base
}

export default function Orders() {
  const { t } = useTranslation()
  const { showNotification } = useAuth()
  const { activeOrgId, activeOrg } = useOrg()

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

  // Fiscalize modal state
  const [fiscalModal, setFiscalModal] = useState(null) // { orders } or null
  const [fiscalInvoiceType, setFiscalInvoiceType] = useState(0)
  const [fiscalTransactionType, setFiscalTransactionType] = useState(0)
  const [sendEmail, setSendEmail] = useState(true)
  const [optionalBuyerFieldEnabled, setOptionalBuyerFieldEnabled] = useState(false)
  const [buyerCostCenterType, setBuyerCostCenterType] = useState('')
  const [buyerCostCenterValue, setBuyerCostCenterValue] = useState('')
  const [advancePaymentDateEnabled, setAdvancePaymentDateEnabled] = useState(false)
  const [advancePaymentDateValue, setAdvancePaymentDateValue] = useState('')
  const [fiscalError, setFiscalError] = useState(null)
  const [fiscalSubmitting, setFiscalSubmitting] = useState(false)

  const showAdvancePaymentDate = isAdvanceSale(fiscalInvoiceType, fiscalTransactionType)
  const advancePaymentBounds = advancePaymentDateTimeBounds()

  useEffect(() => {
    setOrders([])
    setTotalRecords(0)
    setHasFetched(false)
    setCurrentPage(1)
    setExpandedOrderIds(new Set())
    setSelectedOrderIds(new Set())
    setFiscalByOrderId({})
    setError(null)
  }, [activeOrgId])

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
    if (!activeOrgId) { setError(t('orders.selectOrgFirst')); return }
    setError(null)
    setLoading(true)
    setHasFetched(true)
    setExpandedOrderIds(new Set())
    setSelectedOrderIds(new Set())
    const start = (page - 1) * limit
    try {
      const result = await ordersApi.fetch({
        orgId: Number(activeOrgId),
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
    setFiscalTransactionType(0)
    setSendEmail(true)
    setOptionalBuyerFieldEnabled(false)
    setBuyerCostCenterType('')
    setBuyerCostCenterValue('')
    resetAdvancePaymentDate()
    setFiscalError(null)
  }

  function openFiscalModal(order) {
    openFiscalModalForOrders([order])
  }

  function closeFiscalModal() {
    setFiscalModal(null)
    setFiscalError(null)
    setOptionalBuyerFieldEnabled(false)
    setBuyerCostCenterType('')
    setBuyerCostCenterValue('')
    resetAdvancePaymentDate()
  }

  function resetAdvancePaymentDate() {
    setAdvancePaymentDateEnabled(false)
    setAdvancePaymentDateValue('')
  }

  function changeFiscalInvoiceType(value) {
    setFiscalInvoiceType(value)
    if (!isAdvanceSale(value, fiscalTransactionType)) {
      resetAdvancePaymentDate()
    }
  }

  function changeFiscalTransactionType(value) {
    setFiscalTransactionType(value)
    if (!isAdvanceSale(fiscalInvoiceType, value)) {
      resetAdvancePaymentDate()
    }
  }

  async function submitFiscalBill() {
    const ordersToSubmit = fiscalModal?.orders || []
    const selectedOrg = activeOrg
    if (!selectedOrg || !selectedOrg.clientId) {
      setFiscalError(t('orders.noOrgClient'))
      return
    }

    let composedBuyerCostCenterId = null
    if (optionalBuyerFieldEnabled) {
      const trimmedCostCenterValue = buyerCostCenterValue.trim()
      if (!buyerCostCenterType) {
        setFiscalError(t('createFiscalBill.buyerCostCenterTypeRequired'))
        return
      }
      if (!trimmedCostCenterValue) {
        setFiscalError(t('createFiscalBill.buyerCostCenterValueRequired'))
        return
      }
      composedBuyerCostCenterId = composeBuyerCostCenterId(buyerCostCenterType, buyerCostCenterValue)
      if (!composedBuyerCostCenterId) {
        setFiscalError(t('createFiscalBill.buyerCostCenterValueRequired'))
        return
      }
    }

    const sendAdvancePaymentDate = showAdvancePaymentDate && advancePaymentDateEnabled
    if (sendAdvancePaymentDate) {
      const errorCode = validateAdvancePaymentDateTime(advancePaymentDateValue)
      if (errorCode) {
        setFiscalError(t(`createFiscalBill.advancePaymentDateErrors.${errorCode}`, {
          days: ADVANCE_PAYMENT_MAX_DAYS_IN_PAST,
        }))
        return
      }
    }

    const clientId = selectedOrg.clientId

    setFiscalSubmitting(true)
    setFiscalError(null)

    const nextFiscalState = { ...fiscalByOrderId }
    const failures = []
    const successes = []

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
        customerEmail: order.customerEmail || null,
        sendEmail,
        invoiceType: parseInt(fiscalInvoiceType),
        transactionType: parseInt(fiscalTransactionType),
        billingType: order.billingType || null,
        billingCompanyVat: order.billingCompanyVat || null,
        paymentMethodCode: order.paymentMethodCode || null,
        items,
      }
      if (composedBuyerCostCenterId) {
        payload.buyerCostCenterId = composedBuyerCostCenterId
      }
      if (sendAdvancePaymentDate) {
        payload.dateAndTimeOfIssue = advancePaymentDateValue
      }

      try {
        const created = await fiscalBillApi.createFromOrder(
          payload, createIdempotencyKey(),
          Number(activeOrgId), Number(clientId)
        )
        nextFiscalState[order.id] = {
          fiscalbillId: created.fiscalbillId,
          status: created.status,
          sdcInvoiceNumber: created.sdcInvoiceNumber,
          lastError: created.lastError,
        }
        successes.push({ emailStatus: created.emailStatus })
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

    if (successes.length > 0) {
      showNotification(buildFiscalCreatedNotification(successes, t), 'success')
    }

    setSelectedOrderIds(new Set())
    closeFiscalModal()
  }

  return (
    <AppShell
      title={t('orders.title')}
      subtitle={t('orders.subtitle')}
    >
      <form className="filters-panel" onSubmit={handleFetch}>
        <div className="filter-grid">
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
          <button className="primary-button" type="submit" disabled={loading || !activeOrgId}>
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

      {!activeOrgId && (
        <p className="muted org-scope-hint">{t('orgSwitcher.selectPrompt')}</p>
      )}

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
                      <td>{t(`orders.shippingStatusLabels.${order.shippingStatus}`, { defaultValue: order.shippingStatus })}</td>
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
                          >
                            {t('orders.issueFiscalBill')}
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
              <label className="form-group" style={{ display: 'flex', flexDirection: 'row', gap: '0.75rem', alignItems: 'center' }}>
                <input
                  type="checkbox"
                  checked={sendEmail}
                  onChange={(e) => setSendEmail(e.target.checked)}
                />
                <span className="form-label" style={{ marginBottom: 0 }}>{t('orders.sendEmail')}</span>
              </label>
              <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px 20px' }}>
                <label className="form-label" style={{ marginBottom: 0, minWidth: 140 }}>{t('orders.invoiceType')}</label>
                <select className="form-input" style={{ marginBottom: 0, flex: 1 }} value={fiscalInvoiceType} onChange={(e) => changeFiscalInvoiceType(e.target.value)}>
                  {INVOICE_TYPE_VALUES.map((v) => <option key={v} value={v}>{t(`orders.invoiceTypes.${v}`)}</option>)}
                </select>
              </div>
              <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px 20px' }}>
                <label className="form-label" style={{ marginBottom: 0, minWidth: 140 }}>{t('orders.transactionType')}</label>
                <select className="form-input" style={{ marginBottom: 0, flex: 1 }} value={fiscalTransactionType} onChange={(e) => changeFiscalTransactionType(e.target.value)}>
                  {TRANSACTION_TYPE_VALUES.map((v) => <option key={v} value={v}>{t(`orders.transactionTypes.${v}`)}</option>)}
                </select>
              </div>
              {showAdvancePaymentDate && (
                <>
                  <label className="form-group" style={{ display: 'flex', flexDirection: 'row', gap: '0.75rem', alignItems: 'center' }}>
                    <input
                      type="checkbox"
                      checked={advancePaymentDateEnabled}
                      onChange={(e) => {
                        const enabled = e.target.checked
                        setAdvancePaymentDateEnabled(enabled)
                        if (!enabled) setAdvancePaymentDateValue('')
                      }}
                    />
                    <span className="form-label" style={{ marginBottom: 0 }}>{t('createFiscalBill.advancePaymentDate')}</span>
                  </label>
                  {advancePaymentDateEnabled && (
                    <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px 20px' }}>
                      <label className="form-label" style={{ marginBottom: 0, minWidth: 140 }}>{t('createFiscalBill.advancePaymentDateTime')}</label>
                      <input
                        className="form-input"
                        style={{ marginBottom: 0, flex: 1 }}
                        type="datetime-local"
                        value={advancePaymentDateValue}
                        min={advancePaymentBounds.min}
                        max={advancePaymentBounds.max}
                        onChange={(e) => setAdvancePaymentDateValue(e.target.value)}
                      />
                    </div>
                  )}
                </>
              )}
              <label className="form-group" style={{ display: 'flex', flexDirection: 'row', gap: '0.75rem', alignItems: 'center' }}>
                <input
                  type="checkbox"
                  checked={optionalBuyerFieldEnabled}
                  onChange={(e) => {
                    const enabled = e.target.checked
                    setOptionalBuyerFieldEnabled(enabled)
                    if (!enabled) {
                      setBuyerCostCenterType('')
                      setBuyerCostCenterValue('')
                    }
                  }}
                />
                <span className="form-label" style={{ marginBottom: 0 }}>{t('createFiscalBill.optionalBuyerField')}</span>
              </label>
              {optionalBuyerFieldEnabled && (
                <>
                  <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px 20px' }}>
                    <label className="form-label" style={{ marginBottom: 0, minWidth: 140 }}>{t('createFiscalBill.buyerCostCenterType')}</label>
                    <select
                      className="form-input"
                      style={{ marginBottom: 0, flex: 1 }}
                      value={buyerCostCenterType}
                      onChange={(e) => setBuyerCostCenterType(e.target.value)}
                    >
                      <option value="">{t('createFiscalBill.selectBuyerCostCenterType')}</option>
                      {BUYER_COST_CENTER_TYPE_VALUES.map((v) => (
                        <option key={v} value={v}>{t(`createFiscalBill.buyerCostCenterTypes.${v}`)}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '10px 20px' }}>
                    <label className="form-label" style={{ marginBottom: 0, minWidth: 140 }}>{t('createFiscalBill.buyerCostCenterValue')}</label>
                    <input
                      className="form-input"
                      style={{ marginBottom: 0, flex: 1 }}
                      value={buyerCostCenterValue}
                      onChange={(e) => setBuyerCostCenterValue(e.target.value)}
                      placeholder={
                        buyerCostCenterType === '60'
                          ? t('createFiscalBill.buyerCostCenterValuePlaceholder60')
                          : t('createFiscalBill.buyerCostCenterValuePlaceholder')
                      }
                    />
                  </div>
                </>
              )}
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

