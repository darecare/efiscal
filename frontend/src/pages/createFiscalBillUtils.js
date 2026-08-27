export const FALLBACK_TAX_LABELS = ['A', 'E', 'G', 'Đ', 'N']

export const FALLBACK_TAX_LABEL_OPTIONS = FALLBACK_TAX_LABELS.map((label) => ({ label, rate: null }))

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
