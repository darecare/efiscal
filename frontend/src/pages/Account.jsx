import React from 'react'
import AppShell from '../components/AppShell'
import { useAuth } from '../contexts/AuthContext'

export default function Account() {
  const { user } = useAuth()

  return (
    <AppShell title="Account" subtitle="Personal session profile and access footprint.">
      <section className="card">
        <div className="form-grid">
          <div>
            <h3>Identity</h3>
            <p><strong>Name:</strong> {user?.fullName}</p>
            <p><strong>Email:</strong> {user?.email}</p>
            <p><strong>Role:</strong> {user?.roleName}</p>
          </div>
          <div>
            <h3>Subscription</h3>
            <p><strong>Status:</strong> {user?.subscriptionStatus}</p>
            <p><strong>Client:</strong> {user?.clientName || 'Global'}</p>
            <p><strong>Expires:</strong> {user?.subscriptionExpiresAt || 'No expiry'}</p>
          </div>
        </div>
      </section>
    </AppShell>
  )
}
