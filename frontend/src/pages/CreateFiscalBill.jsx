import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { fiscalBillApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const INVOICE_TYPE_VALUES = [0, 4]
const TRANSACTION_TYPE_VALUES = [0, 1]
const PAYMENT_TYPE_VALUES = [0, 1, 2, 3, 4, 5, 6]
const TAX_LABELS = ['A', 'E', 'G', 'Đ', 'N']
const BUYER_ID_TYPE_VALUES = ['10', '11', '12', '13', '14', '15', '16', '20', '21', '22', '23', '30', '31', '32', '33', '34', '35', '36', '40']

function emptyItem() {
  return {
    id: crypto.randomUUID(),
    name: '',
    quantity: '',
    unitPrice: '',
    totalAmount: '',
    taxLabel: 'A',
    taxPrefix: '20',
    gtin: '',
    productId: '',
    sku: '',
  }
}

function emptyPayment() {
  return { id: crypto.randomUUID(), paymentType: 1, amount: '' }
}

export default function CreateFiscalBill() {
  const { t } = useTranslation()
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [orgs, setOrgs] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState('')
  const [allowedPaymentTypes, setAllowedPaymentTypes] = useState([])

  // Header fields
  const [invoiceType, setInvoiceType] = useState(0)
  const [transactionType, setTransactionType] = useState(0)
  const [orderId, setOrderId] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [buyerType, setBuyerType] = useState('')
  const [buyerIdValue, setBuyerIdValue] = useState('')

  // Items
  const [items, setItems] = useState([emptyItem()])

  // Payments
  const [payments, setPayments] = useState([emptyPayment()])

  const [activeTab, setActiveTab] = useState('items')
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadOrgs = isSuperAdmin ? orgsApi.list() : orgsApi.myAccess()
    loadOrgs.then(setOrgs).catch(() => setOrgs([]))
  }, [isSuperAdmin])

  useEffect(() => {
    if (selectedOrgId) {
      orgsApi.getPaymentTypes(selectedOrgId)
        .then(types => {
          setAllowedPaymentTypes(types.length > 0 ? types : PAYMENT_TYPE_VALUES)
        })
        .catch(() => {
          setAllowedPaymentTypes(PAYMENT_TYPE_VALUES)
        })
    }
  }, [selectedOrgId])

  const selectedOrg = orgs.find(org => String(org.orgId) === String(selectedOrgId))
  const selectedClientId = selectedOrg?.clientId != null ? String(selectedOrg.clientId) : ''

  function setItemField(id, field, value) {
    setItems(prev => prev.map(item => item.id === id ? { ...item, [field]: value } : item))
  }

  function addItem() {
    setItems(prev => [...prev, emptyItem()])
  }

  function removeItem(id) {
    setItems(prev => prev.filter(item => item.id !== id))
  }

  function setPaymentField(id, field, value) {
    setPayments(prev => prev.map(p => p.id === id ? { ...p, [field]: value } : p))
  }

  function addPayment() {
    setPayments(prev => [...prev, emptyPayment()])
  }

  function removePayment(id) {
    setPayments(prev => prev.filter(p => p.id !== id))
  }

  function itemsTotal() {
    return items.reduce((sum, i) => sum + (parseFloat(i.totalAmount) || 0), 0).toFixed(2)
  }

  function paymentsTotal() {
    return payments.reduce((sum, p) => sum + (parseFloat(p.amount) || 0), 0).toFixed(2)
  }

  async function handleSubmit() {
    setError(null)
    setResult(null)

    if (!selectedOrgId) {
      setError(t('createFiscalBill.selectOrgRequired'))
      return
    }

    if (!selectedClientId) {
      setError(t('createFiscalBill.orgNotMapped'))
      return
    }

    if (buyerIdValue && !buyerType) {
      setError(t('createFiscalBill.buyerTypeRequired'))
      return
    }

    const payloadItems = items.map(i => ({
      name: i.name,
      quantity: parseFloat(i.quantity),
      unitPrice: parseFloat(i.unitPrice),
      totalAmount: parseFloat(i.totalAmount),
      taxLabel: i.taxLabel,
      labels: [i.taxLabel],
      taxPrefix: i.taxPrefix,
      gtin: i.gtin || null,
      productId: i.productId || null,
      sku: i.sku || null,
    }))

    const payloadPayments = payments.map(p => ({
      paymentType: parseInt(p.paymentType),
      amount: parseFloat(p.amount),
    }))

    const payload = {
      orderId: orderId || null,
      customerName: customerName || null,
      invoiceType: parseInt(invoiceType),
      transactionType: parseInt(transactionType),
      buyerType: buyerIdValue && buyerType ? buyerType : null,
      buyerVat: buyerIdValue || null,
      items: payloadItems,
      payments: payloadPayments,
    }

    const idempotencyKey = crypto.randomUUID()
    setSubmitting(true)
    try {
      const data = await fiscalBillApi.createManual(
        payload, idempotencyKey,
        Number(selectedOrgId), Number(selectedClientId)
      )
      setResult(data)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || t('createFiscalBill.requestFailed')
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setSubmitting(false)
    }
  }

  const tabs = ['items', 'payments']

  return (
    <AppShell title={t('createFiscalBill.title')} subtitle={t('createFiscalBill.subtitle')}>
      <div className="card fiscal-bill-workspace">
        <section className="fiscal-header-area">
          <h3 className="fiscal-area-title">{t('createFiscalBill.header')}</h3>
          <div className="fiscal-header-grid">
            <div className="fiscal-field">
              <label className="fiscal-field-label">{t('common.organization')}</label>
              <select
                className="fiscal-input fiscal-input--select"
                value={selectedOrgId}
                onChange={e => setSelectedOrgId(e.target.value)}
              >
                <option value="">{t('createFiscalBill.selectOrg')}</option>
                {orgs.map(org => (
                  <option key={org.orgId} value={org.orgId}>{org.name}</option>
                ))}
              </select>
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">{t('createFiscalBill.invoiceType')}</label>
              <select className="fiscal-input fiscal-input--select" value={invoiceType} onChange={e => setInvoiceType(e.target.value)}>
                {INVOICE_TYPE_VALUES.map(v => <option key={v} value={v}>{t(`createFiscalBill.invoiceTypes.${v}`)}</option>)}
              </select>
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">{t('createFiscalBill.transactionType')}</label>
              <select className="fiscal-input fiscal-input--select" value={transactionType} onChange={e => setTransactionType(e.target.value)}>
                {TRANSACTION_TYPE_VALUES.map(v => <option key={v} value={v}>{t(`createFiscalBill.transactionTypes.${v}`)}</option>)}
              </select>
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">{t('createFiscalBill.orderIdOptional')}</label>
              <input className="fiscal-input fiscal-input--text" value={orderId} onChange={e => setOrderId(e.target.value)} placeholder={t('createFiscalBill.orderIdPlaceholder')} />
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">{t('createFiscalBill.customerNameOptional')}</label>
              <input className="fiscal-input fiscal-input--text" value={customerName} onChange={e => setCustomerName(e.target.value)} placeholder={t('createFiscalBill.customerNamePlaceholder')} />
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">{t('createFiscalBill.buyerIdTypeOptional')}</label>
              <select className="fiscal-input fiscal-input--select" value={buyerType} onChange={e => setBuyerType(e.target.value)}>
                <option value="">{t('createFiscalBill.selectBuyerIdType')}</option>
                {BUYER_ID_TYPE_VALUES.map(v => <option key={v} value={v}>{t(`createFiscalBill.buyerIdTypes.${v}`)}</option>)}
              </select>
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">{t('createFiscalBill.buyerIdValueOptional')}</label>
              <input className="fiscal-input fiscal-input--text" value={buyerIdValue} onChange={e => setBuyerIdValue(e.target.value)} placeholder={t('createFiscalBill.buyerIdValuePlaceholder')} />
            </div>
          </div>

          {selectedOrgId && selectedOrg && (
            <p className="muted fiscal-client-hint">{t('createFiscalBill.clientHint', { name: selectedOrg.clientName || selectedOrg.clientId })}</p>
          )}
          {selectedOrgId && !selectedOrg?.clientId && (
            <p className="error-text fiscal-error">{t('createFiscalBill.noClientMapping')}</p>
          )}
        </section>

        <section className="fiscal-detail-area">
          <div className="fiscal-tabs" role="tablist" aria-label={t('createFiscalBill.title')}>
            {tabs.map(tab => (
              <button
                key={tab}
                type="button"
                className={`fiscal-tab ${activeTab === tab ? 'active' : ''}`}
                onClick={() => setActiveTab(tab)}
              >
                {tab === 'items' ? t('createFiscalBill.itemsTab', { count: items.length }) : t('createFiscalBill.paymentsTab', { count: payments.length })}
              </button>
            ))}
          </div>

          {activeTab === 'items' && (
            <div className="fiscal-row-list">
              {items.map((item, idx) => (
                <div key={item.id} className="fiscal-row-card">
                  <div className="fiscal-row-card-head">
                    <h4>{t('createFiscalBill.itemNumber', { n: idx + 1 })}</h4>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => removeItem(item.id)}
                      disabled={items.length === 1}
                    >
                      {t('common.remove')}
                    </button>
                  </div>
                  <div className="fiscal-item-grid">
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('common.name')}</label>
                      <input className="fiscal-input fiscal-input--text" value={item.name} onChange={e => setItemField(item.id, 'name', e.target.value)} placeholder={t('createFiscalBill.productName')} />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.quantity')}</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={item.quantity} onChange={e => setItemField(item.id, 'quantity', e.target.value)} placeholder="1" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.unitPrice')}</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={item.unitPrice} onChange={e => setItemField(item.id, 'unitPrice', e.target.value)} placeholder="0.00" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.total')}</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={item.totalAmount} onChange={e => setItemField(item.id, 'totalAmount', e.target.value)} placeholder="0.00" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.taxLabel')}</label>
                      <select className="fiscal-input fiscal-input--select" value={item.taxLabel} onChange={e => setItemField(item.id, 'taxLabel', e.target.value)}>
                        {TAX_LABELS.map(l => <option key={l} value={l}>{l}</option>)}
                      </select>
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.taxPrefix')}</label>
                      <input className="fiscal-input fiscal-input--text" value={item.taxPrefix} onChange={e => setItemField(item.id, 'taxPrefix', e.target.value)} placeholder="20" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.gtin')}</label>
                      <input className="fiscal-input fiscal-input--text" value={item.gtin} onChange={e => setItemField(item.id, 'gtin', e.target.value)} placeholder={t('common.optional')} />
                    </div>
                  </div>
                </div>
              ))}

              <div className="fiscal-tab-actions">
                <button type="button" className="secondary-button" onClick={addItem}>{t('createFiscalBill.addItem')}</button>
                <span className="fiscal-total">{t('createFiscalBill.itemsTotal', { total: itemsTotal() })}</span>
              </div>
            </div>
          )}

          {activeTab === 'payments' && (
            <div className="fiscal-row-list">
              {payments.map((payment, idx) => (
                <div key={payment.id} className="fiscal-row-card">
                  <div className="fiscal-row-card-head">
                    <h4>{t('createFiscalBill.paymentNumber', { n: idx + 1 })}</h4>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => removePayment(payment.id)}
                      disabled={payments.length === 1}
                    >
                      {t('common.remove')}
                    </button>
                  </div>
                  <div className="fiscal-payment-grid">
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.paymentType')}</label>
                      <select className="fiscal-input fiscal-input--select" value={payment.paymentType} onChange={e => setPaymentField(payment.id, 'paymentType', e.target.value)}>
                        {PAYMENT_TYPE_VALUES.filter(v => allowedPaymentTypes.includes(v)).map(v => <option key={v} value={v}>{t(`createFiscalBill.paymentTypes.${v}`)}</option>)}
                      </select>
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('fiscalBills.amount')}</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={payment.amount} onChange={e => setPaymentField(payment.id, 'amount', e.target.value)} placeholder="0.00" />
                    </div>
                  </div>
                </div>
              ))}

              <div className="fiscal-tab-actions">
                <button type="button" className="secondary-button" onClick={addPayment}>{t('createFiscalBill.addPayment')}</button>
                <span className="fiscal-total">
                  {t('createFiscalBill.paymentsTotal', { total: paymentsTotal() })}
                  {paymentsTotal() !== itemsTotal() && (
                    <span className="fiscal-total-warning">{t('createFiscalBill.paymentTotalMismatch', { total: itemsTotal() })}</span>
                  )}
                </span>
              </div>
            </div>
          )}
        </section>
      </div>

      <div className="fiscal-submit-row">
        <button className="primary-button" onClick={handleSubmit} disabled={submitting}>
          {submitting ? t('createFiscalBill.submitting') : t('createFiscalBill.createFiscalBill')}
        </button>
      </div>

      {error && (
        <div className="card" style={{ marginTop: '1rem', borderLeft: '4px solid red', background: '#fff5f5' }}>
          <strong>{t('createFiscalBill.errorLabel')}:</strong> {error}
        </div>
      )}

      {result && (
        <div className="card" style={{ marginTop: '1rem', borderLeft: '4px solid green', background: '#f0fff4' }}>
          <p><strong>{t('createFiscalBill.statusLabel')}:</strong> {result.status}</p>
          {result.sdcInvoiceNumber && <p><strong>{t('createFiscalBill.invoiceNumberLabel')}:</strong> {result.sdcInvoiceNumber}</p>}
          {result.fiscalbillId && <p><strong>{t('createFiscalBill.fiscalBillIdLabel')}:</strong> {result.fiscalbillId}</p>}
          {result.lastError && <p style={{ color: 'red' }}><strong>{t('createFiscalBill.errorLabel')}:</strong> {result.lastError}</p>}
        </div>
      )}
    </AppShell>
  )
}
