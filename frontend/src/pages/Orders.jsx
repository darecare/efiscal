import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { ordersApi } from '../services/api'

const initialFilters = {
  startDate: '2026-04-01',
  endDate: '2026-04-25',
  shippingStatus: 'ready_to_ship',
}

export default function Orders() {
  const [filters, setFilters] = useState(initialFilters)
  const [orders, setOrders] = useState([])

  useEffect(() => {
    ordersApi.fetch(initialFilters).then(setOrders)
  }, [])

  async function handleFetch(event) {
    event.preventDefault()
    const nextOrders = await ordersApi.fetch(filters)
    setOrders(nextOrders)
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
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}
