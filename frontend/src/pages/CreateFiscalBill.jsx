import React, { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { fiscalBillApi, orgsApi, productsApi } from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import './CreateFiscalBill.css'

const INVOICE_TYPE_VALUES = [0, 2, 4]
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
    totalAmount: '0.00',
    taxLabel: 'A',
    taxPrefix: '20',
    gtin: '',
    productId: '',
    sku: '',
    ean: '',
    priceStatus: '',
    priceVerifying: false,
    suggestions: [],
    suggestLoading: false,
    showSuggestions: false,
    suggestError: null,
  }
}

function emptyPayment(amount = '') {
  return { id: crypto.randomUUID(), paymentType: 1, amount }
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
  const [buyerIdAutoMsg, setBuyerIdAutoMsg] = useState(false)
  const [referentDocumentNumber, setReferentDocumentNumber] = useState('')
  const [closeAdvance, setCloseAdvance] = useState(false)

  // Items
  const [items, setItems] = useState([emptyItem()])

  // Payments
  const [payments, setPayments] = useState([emptyPayment()])
  const [userModifiedPayment, setUserModifiedPayment] = useState(false)

  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  const suggestDebounceRef = useRef({})

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

  function itemsTotal() {
    return items.reduce((sum, i) => sum + (parseFloat(i.totalAmount) || 0), 0).toFixed(2)
  }

  function paymentsTotal() {
    return payments.reduce((sum, p) => sum + (parseFloat(p.amount) || 0), 0).toFixed(2)
  }

  const currentItemsTotal = itemsTotal()

  // Auto-sync the payment amount if user hasn't explicitly set up multiple payments
  useEffect(() => {
    if (payments.length === 1 && !userModifiedPayment) {
      if (payments[0].amount !== currentItemsTotal) {
        setPayments(prev => [{ ...prev[0], amount: currentItemsTotal }])
      }
    }
  }, [currentItemsTotal, payments.length, userModifiedPayment])

  function calcTotalAmount(quantity, unitPrice, oldTotal) {
    const q = parseFloat(quantity) || 0
    const p = parseFloat(unitPrice) || 0
    if (q >= 0 && p >= 0) {
      return (q * p).toFixed(2)
    }
    return oldTotal
  }

  function setItemField(id, field, value) {
    setItems(prev => prev.map(item => {
      if (item.id === id) {
        const next = { ...item, [field]: value }
        if (field === 'quantity' || field === 'unitPrice') {
          next.totalAmount = calcTotalAmount(next.quantity, next.unitPrice, next.totalAmount)
        } else if (field === 'totalAmount') {
          // If user manually overrides total, we update it
          next.totalAmount = value
        }
        return next
      }
      return item
    }))
  }

  function patchItem(id, patch) {
    setItems(prev => prev.map(item => {
      if (item.id === id) {
        const next = { ...item, ...patch }
        if ('quantity' in patch || 'unitPrice' in patch) {
          next.totalAmount = calcTotalAmount(next.quantity, next.unitPrice, next.totalAmount)
        }
        return next
      }
      return item
    }))
  }

  function addItem() {
    setItems(prev => [...prev, emptyItem()])
  }

  function removeItem(id) {
    setItems(prev => prev.filter(item => item.id !== id))
  }

  function setPaymentField(id, field, value) {
    if (field === 'amount') setUserModifiedPayment(true)
    setPayments(prev => prev.map(p => p.id === id ? { ...p, [field]: value } : p))
  }

  function addPayment() {
    setUserModifiedPayment(true)
    const remaining = Math.max(0, parseFloat(currentItemsTotal) - parseFloat(paymentsTotal()))
    setPayments(prev => [...prev, emptyPayment(remaining.toFixed(2))])
  }

  function removePayment(id) {
    setUserModifiedPayment(true)
    setPayments(prev => prev.filter(p => p.id !== id))
  }

  function handleBuyerIdChange(e) {
    const val = e.target.value
    setBuyerIdValue(val)
    
    // Auto-infer buyer type based on common lengths (PIB=9, JMBG=13)
    const numeric = val.replace(/\D/g, '')
    if (numeric.length === 13 && (!buyerType || buyerType === '10' || buyerType === '11')) {
      setBuyerType('11')
      triggerBuyerIdMsg()
    } else if (numeric.length === 9 && (!buyerType || buyerType === '10' || buyerType === '11')) {
      setBuyerType('10')
      triggerBuyerIdMsg()
    }
  }

  function triggerBuyerIdMsg() {
    setBuyerIdAutoMsg(true)
    setTimeout(() => setBuyerIdAutoMsg(false), 3000)
  }

  function handleNameChange(itemId, value) {
    patchItem(itemId, {
      name: value,
      showSuggestions: true,
      suggestError: null,
    })

    if (suggestDebounceRef.current[itemId]) {
      clearTimeout(suggestDebounceRef.current[itemId])
    }

    const q = value.trim()
    if (!selectedOrgId || q.length < 2) {
      patchItem(itemId, { suggestions: [], suggestLoading: false })
      return
    }

    patchItem(itemId, { suggestLoading: true })
    suggestDebounceRef.current[itemId] = setTimeout(async () => {
      try {
        const data = await productsApi.search(Number(selectedOrgId), { q })
        setItems((prev) => prev.map((item) => {
          if (item.id !== itemId || item.name.trim() !== q) return item
          return {
            ...item,
            suggestions: data,
            suggestLoading: false,
            showSuggestions: true,
            suggestError: null,
          }
        }))
      } catch (err) {
        const msg = err?.response?.data?.message || err?.response?.data || t('createFiscalBill.searchLoadFailed')
        setItems((prev) => prev.map((item) => {
          if (item.id !== itemId || item.name.trim() !== q) return item
          return {
            ...item,
            suggestions: [],
            suggestLoading: false,
            suggestError: typeof msg === 'string' ? msg : JSON.stringify(msg),
          }
        }))
      }
    }, 300)
  }

  function hideSuggestions(itemId) {
    patchItem(itemId, { showSuggestions: false })
  }

  async function selectProduct(itemId, product) {
    patchItem(itemId, {
      name: product.name || '',
      productId: product.productId ? String(product.productId) : '',
      sku: product.sku || '',
      ean: product.ean || '',
      gtin: product.ean || '',
      priceStatus: '',
      priceVerifying: true,
      suggestions: [],
      showSuggestions: false,
      suggestLoading: false,
      suggestError: null,
      quantity: '1', // Default quantity when selecting product
    })

    try {
      const live = await productsApi.lookup(Number(selectedOrgId), {
        sku: product.sku || undefined,
        ean: product.ean || undefined,
      })
      const price = live.priceGross != null ? String(live.priceGross) : ''
      patchItem(itemId, {
        name: live.name || product.name || '',
        unitPrice: price,
        priceVerifying: false,
        priceStatus: 'verified',
      })
    } catch {
      patchItem(itemId, {
        priceVerifying: false,
        priceStatus: 'unverified',
      })
    }
  }

  const showCloseAdvanceCheckbox = Number(invoiceType) === 0 && Number(transactionType) === 0
  const showReferenceField =
    Number(invoiceType) === 2 ||
    Number(transactionType) === 1 ||
    (Number(invoiceType) === 4 && Number(transactionType) === 0) ||
    closeAdvance

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

    if (items.length === 0) {
      setError(t('createFiscalBill.invalidEmptyItems'))
      return
    }

    const invalidItem = items.find(i => {
      const q = parseFloat(i.quantity)
      const p = parseFloat(i.unitPrice)
      return isNaN(q) || isNaN(p) || q <= 0 || p < 0 || !i.name.trim()
    })
    
    if (invalidItem) {
      setError(t('createFiscalBill.invalidQuantities'))
      return
    }

    const tItems = parseFloat(itemsTotal())
    const tPayments = parseFloat(paymentsTotal())
    if (Math.abs(tItems - tPayments) > 0.01) {
       setError(t('createFiscalBill.paymentTotalMismatch', { total: tItems.toFixed(2) }))
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
      referentDocumentNumber: showReferenceField && referentDocumentNumber.trim() ? referentDocumentNumber.trim() : null,
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

  const balDue = (parseFloat(currentItemsTotal) - parseFloat(paymentsTotal())).toFixed(2)
  const isSettled = Math.abs(parseFloat(balDue)) < 0.01

  return (
    <AppShell title={t('createFiscalBill.title')} subtitle={t('createFiscalBill.subtitle')}>
      
      {error && (
        <div className="card" style={{ marginBottom: '1rem', borderLeft: '4px solid #ef4444', background: '#fef2f2' }}>
          <strong>{t('createFiscalBill.errorLabel')}:</strong> {error}
        </div>
      )}

      {result && (
        <div className="card" style={{ marginBottom: '1rem', borderLeft: '4px solid #22c55e', background: '#f0fdf4' }}>
          <p><strong>{t('createFiscalBill.statusLabel')}:</strong> {result.status}</p>
          {result.sdcInvoiceNumber && <p><strong>{t('createFiscalBill.invoiceNumberLabel')}:</strong> {result.sdcInvoiceNumber}</p>}
          {result.fiscalbillId && <p><strong>{t('createFiscalBill.fiscalBillIdLabel')}:</strong> {result.fiscalbillId}</p>}
          {result.lastError && <p style={{ color: '#ef4444' }}><strong>{t('createFiscalBill.errorLabel')}:</strong> {result.lastError}</p>}
        </div>
      )}

      <div className="fiscal-layout-split">
        <div className="fiscal-main-column">
          {/* HEADER SECTION */}
          <section className="fiscal-section-card">
            <h3 className="fiscal-section-title">{t('createFiscalBill.header')}</h3>
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
                {selectedOrgId && !selectedOrg?.clientId && (
                  <span className="error-text fiscal-error">{t('createFiscalBill.noClientMapping')}</span>
                )}
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
                <label className="fiscal-field-label">{t('createFiscalBill.buyerIdValueOptional')}</label>
                <input className="fiscal-input fiscal-input--text" value={buyerIdValue} onChange={handleBuyerIdChange} placeholder={t('createFiscalBill.buyerIdValuePlaceholder')} />
              </div>

              <div className="fiscal-field">
                <label className="fiscal-field-label">{t('createFiscalBill.buyerIdTypeOptional')}</label>
                <select className="fiscal-input fiscal-input--select" value={buyerType} onChange={e => setBuyerType(e.target.value)}>
                  <option value="">{t('createFiscalBill.selectBuyerIdType')}</option>
                  {BUYER_ID_TYPE_VALUES.map(v => <option key={v} value={v}>{t(`createFiscalBill.buyerIdTypes.${v}`)}</option>)}
                </select>
                {buyerIdAutoMsg && <span className="buyer-id-auto-msg">{t('createFiscalBill.buyerIdAutoSelected')}</span>}
              </div>

              {showCloseAdvanceCheckbox && (
                <div className="fiscal-field fiscal-field--checkbox">
                  <label className="fiscal-field-label fiscal-field-label--inline">
                    <input
                      type="checkbox"
                      checked={closeAdvance}
                      onChange={e => {
                        setCloseAdvance(e.target.checked)
                        if (!e.target.checked) setReferentDocumentNumber('')
                      }}
                    />
                    {' '}{t('createFiscalBill.closeAdvance')}
                  </label>
                </div>
              )}

              {showReferenceField && (
                <div className="fiscal-field">
                  <label className="fiscal-field-label">{t('createFiscalBill.referentDocumentNumber')}</label>
                  <input
                    className="fiscal-input fiscal-input--text"
                    value={referentDocumentNumber}
                    onChange={e => setReferentDocumentNumber(e.target.value)}
                    placeholder={t('createFiscalBill.referentDocumentNumberPlaceholder')}
                  />
                </div>
              )}
            </div>
            {selectedOrgId && selectedOrg && (
              <p className="muted fiscal-client-hint" style={{ marginTop: '1rem' }}>{t('createFiscalBill.clientHint', { name: selectedOrg.clientName || selectedOrg.clientId })}</p>
            )}
          </section>

          {/* ITEMS SECTION */}
          <section className="fiscal-section-card">
            <h3 className="fiscal-section-title">{t('createFiscalBill.itemsSection')}</h3>
            <div className="fiscal-row-list">
              {items.map((item, idx) => (
                <div key={item.id} className="fiscal-row-card fiscal-row-card--enhanced">
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
                    <div className="fiscal-field fiscal-field--with-search" style={{ gridColumn: '1 / -1' }}>
                      <label className="fiscal-field-label">{t('createFiscalBill.productName')}</label>
                      <div className="product-name-combobox">
                        <input
                          className="fiscal-input fiscal-input--text"
                          value={item.name}
                          onChange={e => handleNameChange(item.id, e.target.value)}
                          onFocus={() => {
                            if (item.name.trim().length >= 2) {
                              patchItem(item.id, { showSuggestions: true })
                            }
                          }}
                          onBlur={() => {
                            setTimeout(() => hideSuggestions(item.id), 150)
                          }}
                          placeholder={t('createFiscalBill.searchPlaceholder')}
                          disabled={!selectedOrgId}
                          autoComplete="off"
                        />
                        {item.showSuggestions && selectedOrgId && item.name.trim().length >= 2 && (
                          <ul className="product-suggest-list" role="listbox">
                            {item.suggestLoading && (
                              <li className="product-suggest-item product-suggest-item--muted">{t('common.loadingDots')}</li>
                            )}
                            {item.suggestError && (
                              <li className="product-suggest-item product-suggest-item--error">{item.suggestError}</li>
                            )}
                            {!item.suggestLoading && !item.suggestError && item.suggestions.length === 0 && (
                              <li className="product-suggest-item product-suggest-item--muted">{t('createFiscalBill.searchNoResults')}</li>
                            )}
                            {!item.suggestLoading && item.suggestions.map((p) => (
                              <li key={p.productId} role="option">
                                <button
                                  type="button"
                                  className="product-suggest-option"
                                  onMouseDown={(e) => e.preventDefault()}
                                  onClick={() => selectProduct(item.id, p)}
                                >
                                  <span className="product-suggest-name">{p.name}</span>
                                  <span className="product-suggest-meta">
                                    {p.sku ? `${t('products.columns.sku')}: ${p.sku}` : ''}
                                    {p.sku && p.ean ? ' · ' : ''}
                                    {p.ean ? `${t('products.columns.ean')}: ${p.ean}` : ''}
                                  </span>
                                </button>
                              </li>
                            ))}
                          </ul>
                        )}
                        {!selectedOrgId && (
                          <span className="muted fiscal-price-hint">{t('createFiscalBill.selectOrgRequired')}</span>
                        )}
                      </div>
                      {item.priceVerifying && (
                        <span className="muted fiscal-price-hint">{t('createFiscalBill.priceVerifying')}</span>
                      )}
                      {!item.priceVerifying && item.priceStatus === 'verified' && (
                        <span className="fiscal-price-hint" style={{ color: '#10b981' }}>{t('createFiscalBill.priceVerified')}</span>
                      )}
                      {!item.priceVerifying && item.priceStatus === 'unverified' && (
                        <span className="fiscal-price-hint fiscal-price-hint--warn">{t('createFiscalBill.priceUnverified')}</span>
                      )}
                    </div>
                    
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.quantity')}</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={item.quantity} onChange={e => setItemField(item.id, 'quantity', e.target.value)} placeholder="1" min="0.01" step="any" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.unitPrice')}</label>
                      <input className="fiscal-input fiscal-input--number" type="number" value={item.unitPrice} onChange={e => setItemField(item.id, 'unitPrice', e.target.value)} placeholder="0.00" min="0" step="0.01" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.total')}</label>
                      <input className="fiscal-input fiscal-input--number fiscal-input--readonly" type="number" value={item.totalAmount} onChange={e => setItemField(item.id, 'totalAmount', e.target.value)} placeholder="0.00" />
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

              <div className="fiscal-tab-actions" style={{ marginTop: '1rem' }}>
                <button type="button" className="secondary-button" onClick={addItem}>{t('createFiscalBill.addItem')}</button>
              </div>
            </div>
          </section>
        </div>

        <div className="fiscal-sidebar">
          {/* SUMMARY AND PAYMENTS SECTION */}
          <section className="fiscal-section-card">
            <h3 className="fiscal-section-title">{t('createFiscalBill.paymentsSection')}</h3>
            
            <div className="fiscal-summary-box" style={{ marginBottom: '1.5rem' }}>
              <div className="fiscal-summary-row fiscal-summary-row--total">
                <span>{t('createFiscalBill.itemsTotal', { total: '' })}</span>
                <span>{currentItemsTotal}</span>
              </div>
              <div className="fiscal-summary-row">
                <span>{t('createFiscalBill.paymentsTotal', { total: '' })}</span>
                <span>{paymentsTotal()}</span>
              </div>
              <div className={`fiscal-summary-row fiscal-summary-row--balance ${isSettled ? 'settled' : ''}`}>
                <span>{t('createFiscalBill.balanceDue', { amount: '' })}</span>
                <span>{balDue}</span>
              </div>
            </div>

            <div className="fiscal-row-list">
              {payments.map((payment, idx) => (
                <div key={payment.id} className="fiscal-row-card fiscal-row-card--enhanced" style={{ padding: '0.75rem', marginBottom: '0.75rem' }}>
                  <div className="fiscal-row-card-head" style={{ marginBottom: '0.5rem', paddingBottom: '0.5rem' }}>
                    <h4 style={{ fontSize: '1rem' }}>{t('createFiscalBill.paymentNumber', { n: idx + 1 })}</h4>
                    <button
                      type="button"
                      className="secondary-button match-total-btn"
                      onClick={() => removePayment(payment.id)}
                      disabled={payments.length === 1}
                    >
                      {t('common.remove')}
                    </button>
                  </div>
                  <div className="fiscal-payment-grid" style={{ gridTemplateColumns: '1fr' }}>
                    <div className="fiscal-field">
                      <select className="fiscal-input fiscal-input--select" value={payment.paymentType} onChange={e => setPaymentField(payment.id, 'paymentType', e.target.value)}>
                        {PAYMENT_TYPE_VALUES.filter(v => allowedPaymentTypes.includes(v)).map(v => <option key={v} value={v}>{t(`createFiscalBill.paymentTypes.${v}`)}</option>)}
                      </select>
                    </div>
                    <div className="fiscal-field" style={{ display: 'flex', gap: '0.5rem' }}>
                      <input className="fiscal-input fiscal-input--number" type="number" value={payment.amount} onChange={e => setPaymentField(payment.id, 'amount', e.target.value)} placeholder="0.00" />
                      <button 
                        type="button" 
                        className="secondary-button" 
                        title={t('createFiscalBill.matchTotal')}
                        onClick={() => {
                          const rem = Math.max(0, parseFloat(currentItemsTotal) - (parseFloat(paymentsTotal()) - parseFloat(payment.amount || 0)));
                          setPaymentField(payment.id, 'amount', rem.toFixed(2));
                        }}
                      >
                        =
                      </button>
                    </div>
                  </div>
                </div>
              ))}

              <div className="fiscal-tab-actions" style={{ marginTop: '1rem' }}>
                <button type="button" className="secondary-button" onClick={addPayment} style={{ width: '100%' }}>{t('createFiscalBill.addPayment')}</button>
              </div>
            </div>

            <div className="fiscal-submit-row">
              <button 
                className="primary-button primary-button--large" 
                onClick={handleSubmit} 
                disabled={submitting || !isSettled || items.length === 0}
              >
                {submitting ? t('createFiscalBill.submitting') : t('createFiscalBill.createFiscalBill')}
              </button>
            </div>
          </section>
        </div>
      </div>
    </AppShell>
  )
}
