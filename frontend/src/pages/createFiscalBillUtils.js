export const FALLBACK_TAX_LABELS = ['A', 'E', 'G', 'Đ', 'N']

export const FALLBACK_TAX_LABEL_OPTIONS = FALLBACK_TAX_LABELS.map((label) => ({ label, rate: null }))

export const BUYER_COST_CENTER_TYPE_VALUES = ['20', '21', '30', '31', '32', '33', '50', '60']

/** Tax Authority accepts an advance payment moment up to this many days in the past. */
export const ADVANCE_PAYMENT_MAX_DAYS_IN_PAST = 3

const ADVANCE_PAYMENT_MAX_MS_IN_PAST = ADVANCE_PAYMENT_MAX_DAYS_IN_PAST * 24 * 60 * 60 * 1000

/** Tax Authority allows dateAndTimeOfIssue only on Advance (4) + Sale (0). */
export function isAdvanceSale(invoiceType, transactionType) {
  return Number(invoiceType) === 4 && Number(transactionType) === 0
}

export function toDateTimeLocalValue(date) {
  const pad = (part) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function advancePaymentDateTimeBounds(now = new Date()) {
  return {
    min: toDateTimeLocalValue(new Date(now.getTime() - ADVANCE_PAYMENT_MAX_MS_IN_PAST)),
    max: toDateTimeLocalValue(now),
  }
}

/** Returns an error code (`required`, `invalid`, `future`, `tooOld`) or null when the value is usable. */
export function validateAdvancePaymentDateTime(value, now = new Date()) {
  if (!value || !String(value).trim()) {
    return 'required'
  }
  const selected = new Date(value)
  if (Number.isNaN(selected.getTime())) {
    return 'invalid'
  }
  if (selected.getTime() > now.getTime()) {
    return 'future'
  }
  if (selected.getTime() < now.getTime() - ADVANCE_PAYMENT_MAX_MS_IN_PAST) {
    return 'tooOld'
  }
  return null
}

export function composeBuyerCostCenterId(type, value) {
  if (!type || !value || !String(value).trim()) {
    return null
  }
  let costCenterValue = String(value).trim()
  const prefix = `${type}:`
  if (costCenterValue.startsWith(prefix)) {
    costCenterValue = costCenterValue.slice(prefix.length).trim()
  }
  if (!costCenterValue) {
    return null
  }
  return `${type}:${costCenterValue}`
}

export function formatTaxRate(rate) {
  if (rate === null || rate === undefined || rate === '') return null
  const n = Number(rate)
  if (!Number.isFinite(n)) return null
  return String(n)
}

function toTaxLabelOptions(labels) {
  return labels.map((label) => ({ label, rate: null }))
}

export function isFiscalResultSuccess(result) {
  return result?.status === 'SUCCESS'
}

export function isFiscalResultFailed(result) {
  return result?.status === 'FAILED' || Boolean(result?.lastError)
}

export function calcTotalAmount(quantity, unitPrice, oldTotal) {
  const q = parseFloat(quantity) || 0
  const p = parseFloat(unitPrice) || 0
  if (q >= 0 && p >= 0) {
    return (q * p).toFixed(2)
  }
  return oldTotal
}

/** Gross line total → VAT amount (prices include tax), rounded to 2 decimals. */
export function calcLineTaxValue(totalAmount, rate) {
  const total = parseFloat(totalAmount)
  const r = parseFloat(rate)
  if (!Number.isFinite(total) || total <= 0 || !Number.isFinite(r) || r <= 0) {
    return '0.00'
  }
  return (Math.round((total * r / (100 + r)) * 100) / 100).toFixed(2)
}

export function resolveTaxRateForLabel(taxLabelOptions, label) {
  const option = taxLabelOptions.find((entry) => entry.label === label)
  return option?.rate ?? null
}

export function calcBillTotalTax(items, taxLabelOptions) {
  const sum = items.reduce((acc, item) => {
    const rate = resolveTaxRateForLabel(taxLabelOptions, item.taxLabel)
    return acc + (parseFloat(calcLineTaxValue(item.totalAmount, rate)) || 0)
  }, 0)
  return sum.toFixed(2)
}

/** Keeps manual quantity entry to at most 2 decimal places. */
export function sanitizeQuantityInput(value) {
  if (value === '' || value == null) return ''
  let cleaned = String(value).replace(/[^\d.]/g, '')
  const dotIndex = cleaned.indexOf('.')
  if (dotIndex !== -1) {
    cleaned = cleaned.slice(0, dotIndex + 1) + cleaned.slice(dotIndex + 1).replace(/\./g, '')
    const [whole, fraction = ''] = cleaned.split('.')
    cleaned = `${whole}.${fraction.slice(0, 2)}`
  }
  return cleaned
}

export function inferBuyerTypeFromNumericId(numeric, currentBuyerType) {
  if (numeric.length === 13 && (!currentBuyerType || currentBuyerType === '10' || currentBuyerType === '11')) {
    return '11'
  }
  if (numeric.length === 9 && (!currentBuyerType || currentBuyerType === '10' || currentBuyerType === '11')) {
    return '10'
  }
  return null
}

export function calcPaymentMatchAmount(itemsTotal, paymentsTotal, paymentAmount) {
  const remaining = parseFloat(itemsTotal) - (parseFloat(paymentsTotal) - parseFloat(paymentAmount || 0))
  return Math.max(0, remaining).toFixed(2)
}

export function normalizeTaxLabelOptions(taxes, fallback = FALLBACK_TAX_LABELS) {
  if (!Array.isArray(taxes) || taxes.length === 0) {
    return toTaxLabelOptions(fallback)
  }
  const seen = new Set()
  const options = []
  for (const tax of taxes) {
    if (tax?.isActive === false) continue
    const label = tax?.label
    if (!label || seen.has(label)) continue
    seen.add(label)
    options.push({ label, rate: formatTaxRate(tax.rate) })
  }
  return options.length > 0 ? options : toTaxLabelOptions(fallback)
}
