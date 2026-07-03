export const FALLBACK_TAX_LABELS = ['A', 'E', 'G', 'Đ', 'N']

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
    return [...fallback]
  }
  const labels = [...new Set(
    taxes
      .filter((tax) => tax.isActive !== false)
      .map((tax) => tax.label)
      .filter(Boolean),
  )]
  return labels.length > 0 ? labels : [...fallback]
}
