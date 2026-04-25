import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { usersApi } from '../services/api'

export default function Users() {
  const [users, setUsers] = useState([])

  useEffect(() => {
    usersApi.list().then(setUsers)
  }, [])

  return (
    <AppShell
      title="Users"
      subtitle="Client-scoped users with role and subscription overview."
      actions={<button className="primary-button">Add User</button>}
    >
      <section className="action-bar card">
        <span className="badge">{users.length} users</span>
        <span className="muted">Summary view patterned for later inline filters and row actions.</span>
      </section>
      <section className="table-card">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Client</th>
              <th>Role</th>
              <th>Subscription</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.fullName}</td>
                <td>{user.email}</td>
                <td>{user.clientName}</td>
                <td>{user.roleName}</td>
                <td>{user.subscriptionStatus}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}
