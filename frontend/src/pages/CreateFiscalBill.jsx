import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { fiscalBillApi, orgsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'

const INVOICE_TYPES = [
  { value: 0, label: '0 – Normal' },
  { value: 4, label: '4 – Advance' },
]

const TRANSACTION_TYPES = [
  { value: 0, label: '0 – Sale' },
  { value: 1, label: '1 – Refund' },
]

const PAYMENT_TYPES = [
  { value: 0, label: '0 – Other' },
  { value: 1, label: '1 – Cash' },
  { value: 2, label: '2 – Card' },
  { value: 3, label: '3 – Check' },
  { value: 4, label: '4 – Wire Transfer' },
  { value: 5, label: '5 – Voucher' },
  { value: 6, label: '6 – Mobile Money' },
]

const TAX_LABELS = ['A', 'E', 'G', 'Đ', 'N']

const BUYER_ID_TYPES = [
  { value: '10', label: '10 - PIB kupca (domace pravno lice)' },
  { value: '11', label: '11 - JMBG (domace fizicko lice preduzetnik)' },
  { value: '12', label: '12 - PIB:JBKJS kupca' },
  { value: '13', label: '13 - Kod penzionerske kartice' },
  { value: '14', label: '14 - PIB (poljoprivredno gazdinstvo/pravno lice)' },
  { value: '15', label: '15 - JMBG (poljoprivredno gazdinstvo)' },
  { value: '16', label: '16 - BPG' },
  { value: '20', label: '20 - Broj licne karte (domace fizicko lice)' },
  { value: '21', label: '21 - Broj izbeglicke legitimacije' },
  { value: '22', label: '22 - EBS' },
  { value: '23', label: '23 - Broj pasosa (domace fizicko lice)' },
  { value: '30', label: '30 - Broj pasosa (strano fizicko lice)' },
  { value: '31', label: '31 - Broj diplomatske legitimacije/LK' },
  { value: '32', label: '32 - Broj licne karte MKD' },
  { value: '33', label: '33 - Broj licne karte MNE' },
  { value: '34', label: '34 - Broj licne karte ALB' },
  { value: '35', label: '35 - Broj licne karte BIH' },
  { value: '36', label: '36 - EU/CH/NO/IS licna karta' },
  { value: '40', label: '40 - TIN iz strane drzave' },
]

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
  const { user: currentUser } = useAuth()
  const isSuperAdmin = currentUser?.roleName === 'SUPERADMIN'

  const [orgs, setOrgs] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState('')

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
      setError('Please select organization.')
      return
    }

    if (!selectedClientId) {
      setError('Selected organization is not mapped to a client.')
      return
    }

    if (buyerIdValue && !buyerType) {
      setError('Please select Buyer ID Type when Buyer ID Value is entered.')
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
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || 'Request failed.'
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
    } finally {
      setSubmitting(false)
    }
  }

  const tabs = ['items', 'payments']

  return (
    <AppShell title="Create Fiscal Bill" subtitle="Manual fiscal bill creation (spec 4.2)">
      <div className="card fiscal-bill-workspace">
        <section className="fiscal-header-area">
          <h3 className="fiscal-area-title">Header</h3>
          <div className="fiscal-header-grid">
            <div className="fiscal-field">
              <label className="fiscal-field-label">Organization</label>
              <select
                className="fiscal-input fiscal-input--select"
                value={selectedOrgId}
                onChange={e => setSelectedOrgId(e.target.value)}
              >
                <option value="">Select organization...</option>
                {orgs.map(org => (
                  <option key={org.orgId} value={org.orgId}>{org.name}</option>
                ))}
              </select>
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">Invoice Type</label>
              <select className="fiscal-input fiscal-input--select" value={invoiceType} onChange={e => setInvoiceType(e.target.value)}>
                {INVOICE_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">Transaction Type</label>
              <select className="fiscal-input fiscal-input--select" value={transactionType} onChange={e => setTransactionType(e.target.value)}>
                {TRANSACTION_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">Order ID (optional)</label>
              <input className="fiscal-input fiscal-input--text" value={orderId} onChange={e => setOrderId(e.target.value)} placeholder="e.g. 12345" />
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">Customer Name (optional)</label>
              <input className="fiscal-input fiscal-input--text" value={customerName} onChange={e => setCustomerName(e.target.value)} placeholder="Customer name" />
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">Buyer ID Type (optional)</label>
              <select className="fiscal-input fiscal-input--select" value={buyerType} onChange={e => setBuyerType(e.target.value)}>
                <option value="">Select Buyer ID Type</option>
                {BUYER_ID_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>

            <div className="fiscal-field">
              <label className="fiscal-field-label">Buyer ID Value (optional)</label>
              <input className="fiscal-input fiscal-input--text" value={buyerIdValue} onChange={e => setBuyerIdValue(e.target.value)} placeholder="Identifier part after code prefix" />
            </div>
          </div>

          {selectedOrgId && selectedOrg && (
            <p className="muted fiscal-client-hint">Client: {selectedOrg.clientName || selectedOrg.clientId}</p>
          )}
          {selectedOrgId && !selectedOrg?.clientId && (
            <p className="error-text fiscal-error">No client mapping found for selected organization.</p>
          )}
        </section>

        <section className="fiscal-detail-area">
          <div className="fiscal-tabs" role="tablist" aria-label="Fiscal bill details tabs">
            {tabs.map(tab => (
              <button
                key={tab}
                type="button"
                className={`fiscal-tab ${activeTab === tab ? 'active' : ''}`}
                onClick={() => setActiveTab(tab)}
              >
                {tab === 'items' ? `Items (${items.length})` : `Payments (${payments.length})`}
              </button>
            ))}
          </div>

          {activeTab === 'items' && (
            <div className="fiscal-row-list">
              {items.map((item, idx) => (
                <div key={item.id} className="fiscal-row-card">
                  <div className="fiscal-row-card-head">
                    <h4>Item {idx + 1}</h4>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => removeItem(item.id)}
                      disabled={items.length === 1}
                    >
                      Remove
                    </button>
                  </div>
                  <div className="fiscal-item-grid">
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">Name</label>
                      <input className="fiscal-input fiscal-input--text" value={item.name} onChange={e => setItemField(item.id, 'name', e.target.value)} placeholder="Product name" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">Quantity</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={item.quantity} onChange={e => setItemField(item.id, 'quantity', e.target.value)} placeholder="1" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">Unit Price</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={item.unitPrice} onChange={e => setItemField(item.id, 'unitPrice', e.target.value)} placeholder="0.00" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">Total</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={item.totalAmount} onChange={e => setItemField(item.id, 'totalAmount', e.target.value)} placeholder="0.00" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">Tax Label</label>
                      <select className="fiscal-input fiscal-input--select" value={item.taxLabel} onChange={e => setItemField(item.id, 'taxLabel', e.target.value)}>
                        {TAX_LABELS.map(l => <option key={l} value={l}>{l}</option>)}
                      </select>
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">Tax Prefix</label>
                      <input className="fiscal-input fiscal-input--text" value={item.taxPrefix} onChange={e => setItemField(item.id, 'taxPrefix', e.target.value)} placeholder="20" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">GTIN</label>
                      <input className="fiscal-input fiscal-input--text" value={item.gtin} onChange={e => setItemField(item.id, 'gtin', e.target.value)} placeholder="optional" />
                    </div>
                  </div>
                </div>
              ))}

              <div className="fiscal-tab-actions">
                <button type="button" className="secondary-button" onClick={addItem}>+ Add Item</button>
                <span className="fiscal-total">Items Total: {itemsTotal()}</span>
              </div>
            </div>
          )}

          {activeTab === 'payments' && (
            <div className="fiscal-row-list">
              {payments.map((payment, idx) => (
                <div key={payment.id} className="fiscal-row-card">
                  <div className="fiscal-row-card-head">
                    <h4>Payment {idx + 1}</h4>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => removePayment(payment.id)}
                      disabled={payments.length === 1}
                    >
                      Remove
                    </button>
                  </div>
                  <div className="fiscal-payment-grid">
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">Payment Type</label>
                      <select className="fiscal-input fiscal-input--select" value={payment.paymentType} onChange={e => setPaymentField(payment.id, 'paymentType', e.target.value)}>
                        {PAYMENT_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                      </select>
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">Amount</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={payment.amount} onChange={e => setPaymentField(payment.id, 'amount', e.target.value)} placeholder="0.00" />
                    </div>
                  </div>
                </div>
              ))}

              <div className="fiscal-tab-actions">
                <button type="button" className="secondary-button" onClick={addPayment}>+ Add Payment</button>
                <span className="fiscal-total">
                  Payments Total: {paymentsTotal()}
                  {paymentsTotal() !== itemsTotal() && (
                    <span className="fiscal-total-warning">Mismatch with items total ({itemsTotal()})</span>
                  )}
                </span>
              </div>
            </div>
          )}
        </section>
      </div>

      <div className="fiscal-submit-row">
        <button className="primary-button" onClick={handleSubmit} disabled={submitting}>
          {submitting ? 'Submitting...' : 'Create Fiscal Bill'}
        </button>
      </div>

      {error && (
        <div className="card" style={{ marginTop: '1rem', borderLeft: '4px solid red', background: '#fff5f5' }}>
          <strong>Error:</strong> {error}
        </div>
      )}

      {result && (
        <div className="card" style={{ marginTop: '1rem', borderLeft: '4px solid green', background: '#f0fff4' }}>
          <p><strong>Status:</strong> {result.status}</p>
          {result.sdcInvoiceNumber && <p><strong>Invoice Number:</strong> {result.sdcInvoiceNumber}</p>}
          {result.fiscalbillId && <p><strong>Fiscal Bill ID:</strong> {result.fiscalbillId}</p>}
          {result.lastError && <p style={{ color: 'red' }}><strong>Error:</strong> {result.lastError}</p>}
        </div>
      )}
    </AppShell>
  )
}
