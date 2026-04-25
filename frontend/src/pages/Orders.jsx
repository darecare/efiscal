import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { fiscalBillApi, ordersApi } from '../services/api'

const initialFilters = {
  startDate: '2026-04-01',
  endDate: '2026-04-25',
  shippingStatus: 'ready_to_ship',
}

export default function Orders() {
  const [filters, setFilters] = useState(initialFilters)
  const [orders, setOrders] = useState([])
  const [fiscalByOrderId, setFiscalByOrderId] = useState({})
  const [busyOrderIds, setBusyOrderIds] = useState({})

  useEffect(() => {
    ordersApi.fetch(initialFilters).then(setOrders)
  }, [])

  async function handleFetch(event) {
    event.preventDefault()
    const nextOrders = await ordersApi.fetch(filters)
    setOrders(nextOrders)
  }

  function createIdempotencyKey() {
    if (window.crypto && window.crypto.randomUUID) {
      return window.crypto.randomUUID()
    }
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
      subtitle="Fetch parameters stay explicit so the adapter can later be mapped from apitemplate metadata."
      actions={<button className="secondary-button">Sync Template</button>}
    >
      <form className="filters-panel" onSubmit={handleFetch}>
        <div className="filter-grid">
          <label className="field">
            <span>Start Date</span>
            <input type="date" value={filters.startDate} onChange={(event) => setFilters({ ...filters, startDate: event.target.value })} />
          </label>
          <label className="field">
            <span>End Date</span>
            <input type="date" value={filters.endDate} onChange={(event) => setFilters({ ...filters, endDate: event.target.value })} />
          </label>
          <label className="field">
            <span>Shipping Status</span>
            <select value={filters.shippingStatus} onChange={(event) => setFilters({ ...filters, shippingStatus: event.target.value })}>
              <option value="ready_to_ship">Ready to ship</option>
              <option value="processing">Processing</option>
              <option value="completed">Completed</option>
            </select>
          </label>
        </div>
        <div className="inline-actions">
          <button className="primary-button" type="submit">Fetch Orders</button>
          <span className="badge">{orders.length} records</span>
        </div>
      </form>
      <section className="table-card">
        <table>
          <thead>
            <tr>
              <th>Order No</th>
              <th>Customer</th>
              <th>Status</th>
              <th>Total</th>
              <th>Created At</th>
              <th>Fiscal Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id}>
                <td>{order.externalOrderNo}</td>
                <td>{order.customerName}</td>
                <td>{order.shippingStatus}</td>
                <td>{order.totalAmount} RSD</td>
                <td>{order.createdAt}</td>
                <td>
                  <span className="badge">{fiscalByOrderId[order.id]?.status || 'NOT_SUBMITTED'}</span>
                  {fiscalByOrderId[order.id]?.lastError ? <p className="error-text fiscal-error">{fiscalByOrderId[order.id].lastError}</p> : null}
                </td>
                <td>
                  <div className="inline-actions">
                    <button type="button" className="primary-button" onClick={() => issueFiscalBill(order)} disabled={busyOrderIds[order.id]}>
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
            ))}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}
