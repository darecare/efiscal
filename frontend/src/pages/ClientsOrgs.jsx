import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { clientsOrgsApi } from '../services/api'

export default function ClientsOrgs() {
  const { t } = useTranslation()
  const [items, setItems] = useState([])

  useEffect(() => {
    clientsOrgsApi.list().then(setItems)
  }, [])

  return (
    <AppShell title={t('clientsOrgs.title')} subtitle={t('clientsOrgs.subtitle')}>
      <section className="table-card">
        <table>
          <thead>
            <tr>
              <th>{t('common.client')}</th>
              <th>{t('clientsOrgs.organization')}</th>
              <th>{t('common.status')}</th>
              <th>{t('clientsOrgs.defaultCurrency')}</th>
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
