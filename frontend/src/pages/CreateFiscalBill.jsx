import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { fiscalBillApi, orgsApi, clientsOrgsApi } from '../services/api'
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
  const [clientOrgs, setClientOrgs] = useState([])
  const [selectedOrgId, setSelectedOrgId] = useState('')
  const [selectedClientId, setSelectedClientId] = useState('')

  // Header fields
  const [invoiceType, setInvoiceType] = useState(0)
  const [transactionType, setTransactionType] = useState(0)
  const [orderId, setOrderId] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [buyerType, setBuyerType] = useState('10')
  const [buyerIdValue, setBuyerIdValue] = useState('')

  // Items
  const [items, setItems] = useState([emptyItem()])

  // Payments
  const [payments, setPayments] = useState([emptyPayment()])

  const [activeTab, setActiveTab] = useState('header')
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadOrgs = isSuperAdmin ? orgsApi.list() : orgsApi.myAccess()
    loadOrgs.then(setOrgs).catch(() => setOrgs([]))
    clientsOrgsApi.list().then(setClientOrgs).catch(() => setClientOrgs([]))
  }, [isSuperAdmin])

  // Derive available clients for the selected org
  const clientsForOrg = clientOrgs.filter(co =>
    !selectedOrgId || String(co.orgId) === String(selectedOrgId)
  )

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

    if (!selectedOrgId || !selectedClientId) {
      setError('Please select organization and client.')
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
      buyerType: buyerIdValue ? buyerType : null,
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

  const tabs = ['header', 'items', 'payments']

  return (
    <AppShell title="Create Fiscal Bill" subtitle="Manual fiscal bill creation (spec 4.2)">
      {/* Org / Client selectors */}
      <div className="card" style={{ marginBottom: '1rem', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
        <div className="form-group" style={{ flex: 1, minWidth: '200px' }}>
          <label className="form-label">Organization</label>
          <select className="form-input" value={selectedOrgId} onChange={e => setSelectedOrgId(e.target.value)}>
            <option value="">Select organization...</option>
            {orgs.map(org => (
              <option key={org.orgId} value={org.orgId}>{org.name}</option>
            ))}
          </select>
        </div>
        <div className="form-group" style={{ flex: 1, minWidth: '200px' }}>
          <label className="form-label">Client</label>
          <select className="form-input" value={selectedClientId} onChange={e => setSelectedClientId(e.target.value)}>
            <option value="">Select client...</option>
            {clientsForOrg.map(co => (
              <option key={co.clientId} value={co.clientId}>{co.clientName || co.clientId}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Tab navigation */}
      <div style={{ display: 'flex', gap: '0', borderBottom: '2px solid var(--color-border, #e2e8f0)', marginBottom: '1rem' }}>
        {tabs.map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            style={{
              padding: '0.5rem 1.25rem',
              border: 'none',
              background: 'none',
              cursor: 'pointer',
              fontWeight: activeTab === tab ? 700 : 400,
              borderBottom: activeTab === tab ? '2px solid #3b82f6' : '2px solid transparent',
              marginBottom: '-2px',
              textTransform: 'capitalize',
            }}
          >
            {tab === 'header' ? 'Header' : tab === 'items' ? `Items (${items.length})` : `Payments (${payments.length})`}
          </button>
        ))}
      </div>

      {/* Header tab */}
      {activeTab === 'header' && (
        <div className="card">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '1rem' }}>
            <div className="form-group">
              <label className="form-label">Invoice Type</label>
              <select className="form-input" value={invoiceType} onChange={e => setInvoiceType(e.target.value)}>
                {INVOICE_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Transaction Type</label>
              <select className="form-input" value={transactionType} onChange={e => setTransactionType(e.target.value)}>
                {TRANSACTION_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Order ID (optional)</label>
              <input className="form-input" value={orderId} onChange={e => setOrderId(e.target.value)} placeholder="e.g. 12345" />
            </div>
            <div className="form-group">
              <label className="form-label">Customer Name (optional)</label>
              <input className="form-input" value={customerName} onChange={e => setCustomerName(e.target.value)} placeholder="Customer name" />
            </div>
            <div className="form-group">
              <label className="form-label">Buyer ID Type (optional)</label>
              <select className="form-input" value={buyerType} onChange={e => setBuyerType(e.target.value)}>
                {BUYER_ID_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Buyer ID Value (optional)</label>
              <input className="form-input" value={buyerIdValue} onChange={e => setBuyerIdValue(e.target.value)} placeholder="Identifier part after code prefix" />
            </div>
          </div>
        </div>
      )}

      {/* Items tab */}
      {activeTab === 'items' && (
        <div className="card">
          <table className="data-table" style={{ fontSize: '0.85rem' }}>
            <thead>
              <tr>
                <th>Name</th>
                <th>Qty</th>
                <th>Unit Price</th>
                <th>Total</th>
                <th>Tax Label</th>
                <th>Tax Prefix</th>
                <th>GTIN</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => (
                <tr key={item.id}>
                  <td><input className="form-input" style={{ minWidth: '140px' }} value={item.name} onChange={e => setItemField(item.id, 'name', e.target.value)} placeholder="Product name" /></td>
                  <td><input className="form-input" style={{ width: '70px' }} type="number" value={item.quantity} onChange={e => setItemField(item.id, 'quantity', e.target.value)} placeholder="1" /></td>
                  <td><input className="form-input" style={{ width: '90px' }} type="number" value={item.unitPrice} onChange={e => setItemField(item.id, 'unitPrice', e.target.value)} placeholder="0.00" /></td>
                  <td><input className="form-input" style={{ width: '90px' }} type="number" value={item.totalAmount} onChange={e => setItemField(item.id, 'totalAmount', e.target.value)} placeholder="0.00" /></td>
                  <td>
                    <select className="form-input" style={{ width: '70px' }} value={item.taxLabel} onChange={e => setItemField(item.id, 'taxLabel', e.target.value)}>
                      {TAX_LABELS.map(l => <option key={l} value={l}>{l}</option>)}
                    </select>
                  </td>
                  <td><input className="form-input" style={{ width: '70px' }} value={item.taxPrefix} onChange={e => setItemField(item.id, 'taxPrefix', e.target.value)} placeholder="20" /></td>
                  <td><input className="form-input" style={{ width: '110px' }} value={item.gtin} onChange={e => setItemField(item.id, 'gtin', e.target.value)} placeholder="optional" /></td>
                  <td>
                    <button className="secondary-button" style={{ padding: '0.25rem 0.5rem' }} onClick={() => removeItem(item.id)} disabled={items.length === 1}>✕</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <button className="secondary-button" onClick={addItem}>+ Add Item</button>
            <span style={{ fontWeight: 600 }}>Items Total: {itemsTotal()}</span>
          </div>
        </div>
      )}

      {/* Payments tab */}
      {activeTab === 'payments' && (
        <div className="card">
          <table className="data-table" style={{ fontSize: '0.85rem' }}>
            <thead>
              <tr>
                <th>Payment Type</th>
                <th>Amount</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {payments.map(p => (
                <tr key={p.id}>
                  <td>
                    <select className="form-input" value={p.paymentType} onChange={e => setPaymentField(p.id, 'paymentType', e.target.value)}>
                      {PAYMENT_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                    </select>
                  </td>
                  <td><input className="form-input" style={{ width: '120px' }} type="number" value={p.amount} onChange={e => setPaymentField(p.id, 'amount', e.target.value)} placeholder="0.00" /></td>
                  <td>
                    <button className="secondary-button" style={{ padding: '0.25rem 0.5rem' }} onClick={() => removePayment(p.id)} disabled={payments.length === 1}>✕</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <button className="secondary-button" onClick={addPayment}>+ Add Payment</button>
            <span style={{ fontWeight: 600 }}>
              Payments Total: {paymentsTotal()}
              {paymentsTotal() !== itemsTotal() && (
                <span style={{ color: 'red', marginLeft: '0.5rem' }}>⚠ Mismatch with items total ({itemsTotal()})</span>
              )}
            </span>
          </div>
        </div>
      )}

      {/* Submit */}
      <div style={{ marginTop: '1.25rem', display: 'flex', gap: '1rem', alignItems: 'center' }}>
        <button className="primary-button" onClick={handleSubmit} disabled={submitting}>
          {submitting ? 'Submitting…' : 'Create Fiscal Bill'}
        </button>
      </div>

      {/* Error */}
      {error && (
        <div className="card" style={{ marginTop: '1rem', borderLeft: '4px solid red', background: '#fff5f5' }}>
          <strong>Error:</strong> {error}
        </div>
      )}

      {/* Result */}
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
