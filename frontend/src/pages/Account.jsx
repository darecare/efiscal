import React from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { useAuth } from '../contexts/AuthContext'

export default function Account() {
  const { t } = useTranslation()
  const { user } = useAuth()

  return (
    <AppShell title={t('account.title')} subtitle={t('account.subtitle')}>
      <section className="card">
        <div className="form-grid">
          <div>
            <h3>{t('account.identity')}</h3>
            <p><strong>{t('account.nameLabel')}:</strong> {user?.fullName}</p>
            <p><strong>{t('account.emailLabel')}:</strong> {user?.email}</p>
            <p><strong>{t('account.roleLabel')}:</strong> {user?.roleName}</p>
          </div>
          <div>
            <h3>{t('account.subscription')}</h3>
            <p><strong>{t('account.statusLabel')}:</strong> {user?.subscriptionStatus}</p>
            <p><strong>{t('account.clientLabel')}:</strong> {user?.clientName || t('common.global')}</p>
            <p><strong>{t('account.expiresLabel')}:</strong> {user?.subscriptionExpiresAt || t('account.noExpiry')}</p>
          </div>
        </div>
      </section>
    </AppShell>
  )
}
