import React, { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { fiscalBillApi, orgsApi, productsApi, taxApi } from '../services/api'
import { useOrg } from '../contexts/OrgContext'
import {
  calcPaymentMatchAmount,
  calcTotalAmount,
  FALLBACK_TAX_LABELS,
  inferBuyerTypeFromNumericId,
  isFiscalResultFailed,
  isFiscalResultSuccess,
  normalizeTaxLabelOptions,
} from './createFiscalBillUtils'
import './CreateFiscalBill.css'

const INVOICE_TYPE_VALUES = [0, 2, 4]
const TRANSACTION_TYPE_VALUES = [0, 1]
const PAYMENT_TYPE_VALUES = [0, 1, 2, 3, 4, 5, 6]
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
  const { activeOrgId, activeOrg } = useOrg()
  const [allowedPaymentTypes, setAllowedPaymentTypes] = useState([])

  // Header fields
  const [invoiceType, setInvoiceType] = useState(0)
  const [transactionType, setTransactionType] = useState(0)
  const [orderId, setOrderId] = useState('')
  const [customerName, setCustomerName] = useState('')
  const [sendEmail, setSendEmail] = useState(false)
  const [customerEmail, setCustomerEmail] = useState('')
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
  const [fieldErrors, setFieldErrors] = useState({})
  const [itemErrors, setItemErrors] = useState({})
  const [paymentErrors, setPaymentErrors] = useState({})
  const [taxLabelOptions, setTaxLabelOptions] = useState(FALLBACK_TAX_LABELS)
  const [downloadingPdf, setDownloadingPdf] = useState(false)

  const suggestDebounceRef = useRef({})
  const headerSectionRef = useRef(null)
  const itemsSectionRef = useRef(null)
  const sidebarSectionRef = useRef(null)

  const prevOrgIdRef = useRef(activeOrgId)

  useEffect(() => {
    taxApi.list()
      .then((taxes) => setTaxLabelOptions(normalizeTaxLabelOptions(taxes)))
      .catch(() => setTaxLabelOptions(FALLBACK_TAX_LABELS))
  }, [])

  useEffect(() => {
    if (prevOrgIdRef.current !== activeOrgId) {
      prevOrgIdRef.current = activeOrgId
      setItems([emptyItem()])
      setPayments([emptyPayment()])
      setUserModifiedPayment(false)
      setResult(null)
      setError(null)
      setFieldErrors({})
      setItemErrors({})
      setPaymentErrors({})
    }
  }, [activeOrgId])

  useEffect(() => {
    if (activeOrgId) {
      orgsApi.getPaymentTypes(activeOrgId)
        .then(types => {
          setAllowedPaymentTypes(types.length > 0 ? types : PAYMENT_TYPE_VALUES)
        })
        .catch(() => {
          setAllowedPaymentTypes(PAYMENT_TYPE_VALUES)
        })
    } else {
      setAllowedPaymentTypes([])
    }
  }, [activeOrgId])

  const selectedClientId = activeOrg?.clientId != null ? String(activeOrg.clientId) : ''

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

  function setPaymentField(id, field, value, options = {}) {
    if (field === 'amount' && options.markModified !== false) {
      setUserModifiedPayment(true)
    }
    setPayments(prev => prev.map(p => p.id === id ? { ...p, [field]: value } : p))
  }

  function matchPaymentToRemaining(paymentId, paymentAmount) {
    const rem = calcPaymentMatchAmount(currentItemsTotal, paymentsTotal(), paymentAmount)
    if (payments.length === 1) {
      setPaymentField(paymentId, 'amount', rem, { markModified: false })
      return
    }
    setPaymentField(paymentId, 'amount', rem)
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

    const numeric = val.replace(/\D/g, '')
    setBuyerType((prev) => {
      const inferred = inferBuyerTypeFromNumericId(numeric, prev)
      if (inferred && inferred !== prev) {
        triggerBuyerIdMsg()
        return inferred
      }
      return prev
    })
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
    if (!activeOrgId || q.length < 2) {
      patchItem(itemId, { suggestions: [], suggestLoading: false })
      return
    }

    patchItem(itemId, { suggestLoading: true })
    suggestDebounceRef.current[itemId] = setTimeout(async () => {
      try {
        const data = await productsApi.search(Number(activeOrgId), { q })
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
      const live = await productsApi.lookup(Number(activeOrgId), {
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

  function clearValidationErrors() {
    setFieldErrors({})
    setItemErrors({})
    setPaymentErrors({})
  }

  function scrollToFirstError(nextFieldErrors, nextItemErrors, nextPaymentErrors) {
    const target = (() => {
      if (
        nextFieldErrors.orgId ||
        nextFieldErrors.clientId ||
        nextFieldErrors.buyerType ||
        nextFieldErrors.buyerIdValue ||
        nextFieldErrors.referentDocumentNumber
      ) {
        return headerSectionRef.current
      }
      if (Object.keys(nextItemErrors).length > 0) {
        return itemsSectionRef.current
      }
      if (Object.keys(nextPaymentErrors).length > 0 || nextFieldErrors.paymentTotal) {
        return sidebarSectionRef.current
      }
      return null
    })()
    if (target?.scrollIntoView) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }

  function validateForm() {
    const nextFieldErrors = {}
    const nextItemErrors = {}
    const nextPaymentErrors = {}
    let globalError = null

    if (!activeOrgId) {
      nextFieldErrors.orgId = t('orgSwitcher.selectPrompt')
      globalError = nextFieldErrors.orgId
    } else if (!selectedClientId) {
      nextFieldErrors.clientId = t('createFiscalBill.orgNotMapped')
      globalError = nextFieldErrors.clientId
    }

    const trimmedBuyerIdValue = buyerIdValue.trim()
    const hasBuyerType = Boolean(buyerType)

    if (trimmedBuyerIdValue && !hasBuyerType) {
      nextFieldErrors.buyerType = t('createFiscalBill.buyerTypeRequired')
      globalError = globalError || nextFieldErrors.buyerType
    }

    if (hasBuyerType && !trimmedBuyerIdValue) {
      nextFieldErrors.buyerIdValue = t('createFiscalBill.buyerIdValueRequired')
      globalError = globalError || nextFieldErrors.buyerIdValue
    }

    if (showReferenceField && !referentDocumentNumber.trim()) {
      nextFieldErrors.referentDocumentNumber = t('createFiscalBill.validation.referentDocumentRequired')
      globalError = globalError || nextFieldErrors.referentDocumentNumber
    }

    if (items.length === 0) {
      globalError = globalError || t('createFiscalBill.invalidEmptyItems')
    }

    items.forEach((item, idx) => {
      const errs = {}
      if (!item.name.trim()) {
        errs.name = t('createFiscalBill.validation.productNameRequired')
      }
      const q = parseFloat(item.quantity)
      if (item.quantity === '' || isNaN(q)) {
        errs.quantity = t('createFiscalBill.validation.quantityRequired')
      } else if (q <= 0) {
        errs.quantity = t('createFiscalBill.validation.quantityPositive')
      }
      const p = parseFloat(item.unitPrice)
      if (item.unitPrice === '' || isNaN(p)) {
        errs.unitPrice = t('createFiscalBill.validation.unitPriceRequired')
      } else if (p < 0) {
        errs.unitPrice = t('createFiscalBill.validation.unitPriceInvalid')
      }
      if (Object.keys(errs).length > 0) {
        nextItemErrors[item.id] = errs
        if (!globalError) {
          globalError = t('createFiscalBill.validation.itemSummary', { n: idx + 1, message: Object.values(errs)[0] })
        }
      }
    })

    payments.forEach((payment) => {
      const amt = parseFloat(payment.amount)
      if (payment.amount === '' || isNaN(amt)) {
        nextPaymentErrors[payment.id] = { amount: t('createFiscalBill.validation.paymentAmountRequired') }
      } else if (amt <= 0) {
        nextPaymentErrors[payment.id] = { amount: t('createFiscalBill.validation.paymentAmountPositive') }
      }
    })

    const tItems = parseFloat(itemsTotal())
    const tPayments = parseFloat(paymentsTotal())
    if (!isNaN(tItems) && !isNaN(tPayments) && Math.abs(tItems - tPayments) > 0.01) {
      nextFieldErrors.paymentTotal = t('createFiscalBill.paymentTotalMismatch', { total: tItems.toFixed(2) })
      globalError = globalError || nextFieldErrors.paymentTotal
    }

    return {
      valid: !globalError && Object.keys(nextItemErrors).length === 0 && Object.keys(nextPaymentErrors).length === 0,
      globalError,
      nextFieldErrors,
      nextItemErrors,
      nextPaymentErrors,
    }
  }

  async function handleSubmit() {
    setError(null)
    setResult(null)
    clearValidationErrors()

    const validation = validateForm()
    if (!validation.valid) {
      setFieldErrors(validation.nextFieldErrors)
      setItemErrors(validation.nextItemErrors)
      setPaymentErrors(validation.nextPaymentErrors)
      setError(validation.globalError)
      scrollToFirstError(validation.nextFieldErrors, validation.nextItemErrors, validation.nextPaymentErrors)
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

    const normalizedBuyerType = buyerType || null
    const normalizedBuyerIdValue = buyerIdValue.trim() || null
    const composedBuyerId = normalizedBuyerType && normalizedBuyerIdValue
      ? `${normalizedBuyerType}:${normalizedBuyerIdValue}`
      : null

    const payload = {
      orderId: orderId || null,
      customerName: customerName || null,
      customerEmail: sendEmail && customerEmail.trim() ? customerEmail.trim() : null,
      sendEmail,
      invoiceType: parseInt(invoiceType),
      transactionType: parseInt(transactionType),
      buyerId: composedBuyerId,
      buyerType: normalizedBuyerType,
      buyerVat: normalizedBuyerIdValue,
      items: payloadItems,
      payments: payloadPayments,
      referentDocumentNumber: showReferenceField && referentDocumentNumber.trim() ? referentDocumentNumber.trim() : null,
    }

    const idempotencyKey = crypto.randomUUID()
    setSubmitting(true)
    try {
      const data = await fiscalBillApi.createManual(
        payload, idempotencyKey,
        Number(activeOrgId), Number(selectedClientId)
      )
      setResult(data)
    } catch (err) {
      const data = err?.response?.data
      if (err?.response?.status === 502 && data?.status) {
        setResult(data)
        setError(null)
      } else {
        const msg = data?.message || data?.lastError || data || err?.message || t('createFiscalBill.requestFailed')
        setError(typeof msg === 'string' ? msg : JSON.stringify(msg))
      }
    } finally {
      setSubmitting(false)
    }
  }

  function resetFormForAnother() {
    setInvoiceType(0)
    setTransactionType(0)
    setOrderId('')
    setCustomerName('')
    setSendEmail(false)
    setCustomerEmail('')
    setBuyerType('')
    setBuyerIdValue('')
    setBuyerIdAutoMsg(false)
    setReferentDocumentNumber('')
    setCloseAdvance(false)
    setItems([emptyItem()])
    setPayments([emptyPayment()])
    setUserModifiedPayment(false)
    setResult(null)
    setError(null)
    clearValidationErrors()
  }

  async function handleDownloadPdf() {
    const fiscalBillId = result?.fiscalbillId
    if (!fiscalBillId) return

    setDownloadingPdf(true)
    try {
      const blob = await fiscalBillApi.downloadPdf(fiscalBillId, 'a4')
      const url = window.URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `fiscal-bill-${fiscalBillId}-a4.pdf`
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || err?.message || t('fiscalBills.downloadPdfFailed')
      setError(typeof msg === 'string' ? msg : t('fiscalBills.downloadPdfFailed'))
    } finally {
      setDownloadingPdf(false)
    }
  }

  const balDue = (parseFloat(currentItemsTotal) - parseFloat(paymentsTotal())).toFixed(2)
  const isSettled = Math.abs(parseFloat(balDue)) < 0.01
  const resultCardClass = result
    ? (isFiscalResultFailed(result) ? 'fiscal-result-card fiscal-result-card--failed' : 'fiscal-result-card fiscal-result-card--success')
    : ''

  return (
    <AppShell title={t('createFiscalBill.title')} subtitle={t('createFiscalBill.subtitle')}>
      
      {error && (
        <div className="fiscal-result-card fiscal-result-card--failed" style={{ marginBottom: '1rem' }}>
          <strong>{t('createFiscalBill.errorLabel')}:</strong> {error}
        </div>
      )}

      {result && (
        <div className={resultCardClass} style={{ marginBottom: '1rem' }}>
          <p><strong>{isFiscalResultFailed(result) ? t('createFiscalBill.resultFailedTitle') : t('createFiscalBill.resultSuccessTitle')}</strong></p>
          <p><strong>{t('createFiscalBill.statusLabel')}:</strong> {result.status}</p>
          {result.sdcInvoiceNumber && <p><strong>{t('createFiscalBill.invoiceNumberLabel')}:</strong> {result.sdcInvoiceNumber}</p>}
          {result.fiscalbillId && <p><strong>{t('createFiscalBill.fiscalBillIdLabel')}:</strong> {result.fiscalbillId}</p>}
          {result.lastError && <p className="fiscal-result-error-line"><strong>{t('createFiscalBill.errorLabel')}:</strong> {result.lastError}</p>}
          {isFiscalResultSuccess(result) && (
            <div className="fiscal-result-actions">
              {result.efiscalLink && (
                <a
                  href={result.efiscalLink}
                  target="_blank"
                  rel="noreferrer"
                  className="secondary-button"
                >
                  {t('createFiscalBill.openVerificationLink')}
                </a>
              )}
              {result.fiscalbillId && (
                <button
                  type="button"
                  className="secondary-button"
                  onClick={handleDownloadPdf}
                  disabled={downloadingPdf}
                >
                  {downloadingPdf ? t('fiscalBills.downloadingPdf') : t('fiscalBills.downloadPdfA4')}
                </button>
              )}
              <Link to="/fiscal-bills" className="secondary-button fiscal-result-link-button">
                {t('createFiscalBill.goToFiscalBills')}
              </Link>
              <button type="button" className="primary-button" onClick={resetFormForAnother}>
                {t('createFiscalBill.createAnother')}
              </button>
            </div>
          )}
        </div>
      )}

      <div className="fiscal-layout-split">
        <div className="fiscal-main-column">
          {/* HEADER SECTION */}
          <section className="fiscal-section-card" ref={headerSectionRef}>
            <h3 className="fiscal-section-title">{t('createFiscalBill.header')}</h3>
            <div className="fiscal-header-grid">
              {!activeOrgId && (
                <p className="muted org-scope-hint fiscal-client-hint" style={{ gridColumn: '1 / -1' }}>
                  {t('orgSwitcher.selectPrompt')}
                </p>
              )}
              {fieldErrors.orgId && (
                <p className="error-text fiscal-error" style={{ gridColumn: '1 / -1' }}>{fieldErrors.orgId}</p>
              )}
              {activeOrgId && !activeOrg?.clientId && (
                <p className="error-text fiscal-error" style={{ gridColumn: '1 / -1' }}>
                  {fieldErrors.clientId || t('createFiscalBill.noClientMapping')}
                </p>
              )}

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
                <select
                  className={`fiscal-input fiscal-input--select${fieldErrors.buyerType ? ' fiscal-input--invalid' : ''}`}
                  value={buyerType}
                  onChange={e => setBuyerType(e.target.value)}
                  aria-invalid={fieldErrors.buyerType ? 'true' : undefined}
                >
                  <option value="">{t('createFiscalBill.selectBuyerIdType')}</option>
                  {BUYER_ID_TYPE_VALUES.map(v => <option key={v} value={v}>{t(`createFiscalBill.buyerIdTypes.${v}`)}</option>)}
                </select>
                {fieldErrors.buyerType && <span className="error-text fiscal-error">{fieldErrors.buyerType}</span>}
                {buyerIdAutoMsg && <span className="buyer-id-auto-msg">{t('createFiscalBill.buyerIdAutoSelected')}</span>}
              </div>

              <div className="fiscal-field">
                <label className="fiscal-field-label">{t('createFiscalBill.buyerIdValueOptional')}</label>
                <input
                  className={`fiscal-input fiscal-input--text${fieldErrors.buyerIdValue ? ' fiscal-input--invalid' : ''}`}
                  value={buyerIdValue}
                  onChange={handleBuyerIdChange}
                  placeholder={t('createFiscalBill.buyerIdValuePlaceholder')}
                  aria-invalid={fieldErrors.buyerIdValue ? 'true' : undefined}
                />
                {fieldErrors.buyerIdValue && <span className="error-text fiscal-error">{fieldErrors.buyerIdValue}</span>}
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
                    className={`fiscal-input fiscal-input--text${fieldErrors.referentDocumentNumber ? ' fiscal-input--invalid' : ''}`}
                    value={referentDocumentNumber}
                    onChange={e => setReferentDocumentNumber(e.target.value)}
                    placeholder={t('createFiscalBill.referentDocumentNumberPlaceholder')}
                    aria-invalid={fieldErrors.referentDocumentNumber ? 'true' : undefined}
                  />
                  {fieldErrors.referentDocumentNumber && (
                    <span className="error-text fiscal-error">{fieldErrors.referentDocumentNumber}</span>
                  )}
                </div>
              )}

              <div className="fiscal-field fiscal-field--checkbox">
                <label className="fiscal-field-label fiscal-field-label--inline">
                  <input
                    type="checkbox"
                    checked={sendEmail}
                    onChange={(e) => setSendEmail(e.target.checked)}
                  />
                  {' '}{t('createFiscalBill.sendEmail')}
                </label>
              </div>

              {sendEmail && (
                <div className="fiscal-field">
                  <label className="fiscal-field-label">{t('createFiscalBill.customerEmailOptional')}</label>
                  <input
                    className="fiscal-input fiscal-input--text"
                    type="email"
                    value={customerEmail}
                    onChange={(e) => setCustomerEmail(e.target.value)}
                    placeholder={t('createFiscalBill.customerEmailPlaceholder')}
                  />
                </div>
              )}
            </div>
            {activeOrgId && activeOrg && (
              <p className="muted fiscal-client-hint" style={{ marginTop: '1rem' }}>{t('createFiscalBill.clientHint', { name: activeOrg.clientName || activeOrg.clientId })}</p>
            )}
          </section>

          {/* ITEMS SECTION */}
          <section className="fiscal-section-card" ref={itemsSectionRef}>
            <h3 className="fiscal-section-title">{t('createFiscalBill.itemsSection')}</h3>
            <div className="fiscal-row-list">
              {items.map((item, idx) => (
                <div key={item.id} className={`fiscal-row-card fiscal-row-card--enhanced${itemErrors[item.id] ? ' fiscal-row-card--invalid' : ''}`}>
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
                          className={`fiscal-input fiscal-input--text${itemErrors[item.id]?.name ? ' fiscal-input--invalid' : ''}`}
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
                          disabled={!activeOrgId}
                          autoComplete="off"
                          aria-autocomplete="list"
                          aria-expanded={item.showSuggestions && item.suggestions.length > 0}
                          aria-invalid={itemErrors[item.id]?.name ? 'true' : undefined}
                        />
                        {item.showSuggestions && activeOrgId && item.name.trim().length >= 2 && (
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
                        {!activeOrgId && (
                          <span className="muted fiscal-price-hint">{t('orgSwitcher.selectPrompt')}</span>
                        )}
                      </div>
                      {itemErrors[item.id]?.name && (
                        <span className="error-text fiscal-error">{itemErrors[item.id].name}</span>
                      )}
                      {item.priceVerifying && (
                        <span className="muted fiscal-price-hint">{t('createFiscalBill.priceVerifying')}</span>
                      )}
                      {!item.priceVerifying && item.priceStatus === 'verified' && (
                        <span className="fiscal-price-hint fiscal-price-hint--ok">{t('createFiscalBill.priceVerified')}</span>
                      )}
                      {!item.priceVerifying && item.priceStatus === 'unverified' && (
                        <span className="fiscal-price-hint fiscal-price-hint--warn">{t('createFiscalBill.priceUnverified')}</span>
                      )}
                    </div>
                    
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.quantity')}</label>
                      <input
                        className={`fiscal-input fiscal-input--number${itemErrors[item.id]?.quantity ? ' fiscal-input--invalid' : ''}`}
                        type="number"
                        value={item.quantity}
                        onChange={e => setItemField(item.id, 'quantity', e.target.value)}
                        placeholder="1"
                        min="0.01"
                        step="any"
                        aria-invalid={itemErrors[item.id]?.quantity ? 'true' : undefined}
                      />
                      {itemErrors[item.id]?.quantity && (
                        <span className="error-text fiscal-error">{itemErrors[item.id].quantity}</span>
                      )}
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.unitPrice')}</label>
                      <input
                        className={`fiscal-input fiscal-input--number${itemErrors[item.id]?.unitPrice ? ' fiscal-input--invalid' : ''}`}
                        type="number"
                        value={item.unitPrice}
                        onChange={e => setItemField(item.id, 'unitPrice', e.target.value)}
                        placeholder="0.00"
                        min="0"
                        step="0.01"
                        aria-invalid={itemErrors[item.id]?.unitPrice ? 'true' : undefined}
                      />
                      {itemErrors[item.id]?.unitPrice && (
                        <span className="error-text fiscal-error">{itemErrors[item.id].unitPrice}</span>
                      )}
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.total')}</label>
                      <input className="fiscal-input fiscal-input--number fiscal-input--readonly" type="number" value={item.totalAmount} onChange={e => setItemField(item.id, 'totalAmount', e.target.value)} placeholder="0.00" />
                    </div>
                    <div className="fiscal-field">
                      <label className="fiscal-field-label">{t('createFiscalBill.taxLabel')}</label>
                      <select className="fiscal-input fiscal-input--select" value={item.taxLabel} onChange={e => setItemField(item.id, 'taxLabel', e.target.value)}>
                        {taxLabelOptions.map(l => <option key={l} value={l}>{l}</option>)}
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

        <div className="fiscal-sidebar" ref={sidebarSectionRef}>
          {/* SUMMARY AND PAYMENTS SECTION */}
          <section className="fiscal-section-card">
            <h3 className="fiscal-section-title">{t('createFiscalBill.paymentsSection')}</h3>
            
            <div className="fiscal-summary-box" style={{ marginBottom: '1.5rem' }}>
              <div className="fiscal-summary-row fiscal-summary-row--total">
                <span>{t('createFiscalBill.summary.itemsTotalLabel')}</span>
                <span>{currentItemsTotal}</span>
              </div>
              <div className="fiscal-summary-row">
                <span>{t('createFiscalBill.summary.paymentsTotalLabel')}</span>
                <span>{paymentsTotal()}</span>
              </div>
              <div className={`fiscal-summary-row fiscal-summary-row--balance ${isSettled ? 'settled' : ''}`}>
                <span>{t('createFiscalBill.summary.balanceDueLabel')}</span>
                <span>{balDue}</span>
              </div>
              {fieldErrors.paymentTotal && (
                <span className="error-text fiscal-error">{fieldErrors.paymentTotal}</span>
              )}
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
                      <input
                        className={`fiscal-input fiscal-input--number${paymentErrors[payment.id]?.amount ? ' fiscal-input--invalid' : ''}`}
                        type="number"
                        value={payment.amount}
                        onChange={e => setPaymentField(payment.id, 'amount', e.target.value)}
                        placeholder="0.00"
                        aria-invalid={paymentErrors[payment.id]?.amount ? 'true' : undefined}
                      />
                      <button 
                        type="button" 
                        className="secondary-button fiscal-match-total-btn" 
                        title={t('createFiscalBill.matchTotal')}
                        onClick={() => matchPaymentToRemaining(payment.id, payment.amount)}
                      >
                        =
                      </button>
                    </div>
                    {paymentErrors[payment.id]?.amount && (
                      <span className="error-text fiscal-error">{paymentErrors[payment.id].amount}</span>
                    )}
                  </div>
                </div>
              ))}

              <div className="fiscal-tab-actions" style={{ marginTop: '1rem' }}>
                <button type="button" className="secondary-button" onClick={addPayment} style={{ width: '100%' }}>{t('createFiscalBill.addPayment')}</button>
              </div>
            </div>

            <div className="fiscal-submit-row">
              {!isSettled && !submitting && (
                <p className="fiscal-submit-hint">{t('createFiscalBill.submitDisabledBalance')}</p>
              )}
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
