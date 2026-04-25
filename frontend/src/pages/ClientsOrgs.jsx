import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { clientsOrgsApi } from '../services/api'

export default function ClientsOrgs() {
  const [items, setItems] = useState([])

  useEffect(() => {
    clientsOrgsApi.list().then(setItems)
  }, [])

  return (
    <AppShell title="Clients and Organizations" subtitle="Tenant structure and organization access boundaries.">
      <section className="table-card">
        <table>
          <thead>
            <tr>
              <th>Client</th>
              <th>Organization</th>
              <th>Status</th>
              <th>Default Currency</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.orgId}>
                <td>{item.clientName}</td>
                <td>{item.orgName}</td>
                <td>{item.status}</td>
                <td>{item.currency}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}
