import { describe, expect, it } from 'vitest'
import {
  calcPaymentMatchAmount,
  calcTotalAmount,
  FALLBACK_TAX_LABELS,
  FALLBACK_TAX_LABEL_OPTIONS,
  inferBuyerTypeFromNumericId,
  isFiscalResultFailed,
  isFiscalResultSuccess,
  normalizeTaxLabelOptions,
} from './createFiscalBillUtils'

describe('createFiscalBillUtils', () => {
  describe('calcTotalAmount', () => {
    it('multiplies quantity and unit price', () => {
      expect(calcTotalAmount('2', '50', '0.00')).toBe('100.00')
    })

    it('keeps previous total when unit price is negative', () => {
      expect(calcTotalAmount('2', '-1', '12.34')).toBe('12.34')
    })
  })

  describe('inferBuyerTypeFromNumericId', () => {
    it('maps 9 digits to PIB when type is empty or PIB/JMBG', () => {
      expect(inferBuyerTypeFromNumericId('123456789', '')).toBe('10')
      expect(inferBuyerTypeFromNumericId('123456789', '11')).toBe('10')
    })

    it('maps 13 digits to JMBG when type is empty or PIB/JMBG', () => {
      expect(inferBuyerTypeFromNumericId('1234567890123', '')).toBe('11')
      expect(inferBuyerTypeFromNumericId('1234567890123', '10')).toBe('11')
    })

    it('does not override unrelated buyer types', () => {
      expect(inferBuyerTypeFromNumericId('123456789', '12')).toBeNull()
      expect(inferBuyerTypeFromNumericId('1234567890123', '20')).toBeNull()
    })
  })

  describe('calcPaymentMatchAmount', () => {
    it('fills remaining balance for a payment row', () => {
      expect(calcPaymentMatchAmount('100.00', '100.00', '30')).toBe('30.00')
      expect(calcPaymentMatchAmount('100.00', '60.00', '20')).toBe('60.00')
    })

    it('never returns negative amounts', () => {
      expect(calcPaymentMatchAmount('50.00', '100.00', '100')).toBe('50.00')
    })
  })

  describe('normalizeTaxLabelOptions', () => {
    it('uses active API labels and rates when available', () => {
      expect(normalizeTaxLabelOptions([
        { label: 'A', rate: 10, isActive: true },
        { label: 'E', rate: 20.00, isActive: true },
        { label: 'Z', rate: 0, isActive: false },
      ])).toEqual([
        { label: 'A', rate: '10' },
        { label: 'E', rate: '20' },
      ])
    })

    it('falls back when API returns no usable labels', () => {
      expect(normalizeTaxLabelOptions([])).toEqual(FALLBACK_TAX_LABEL_OPTIONS)
      expect(normalizeTaxLabelOptions(null)).toEqual(FALLBACK_TAX_LABEL_OPTIONS)
      expect(FALLBACK_TAX_LABEL_OPTIONS.map((option) => option.label)).toEqual(FALLBACK_TAX_LABELS)
    })
  })

  describe('fiscal result helpers', () => {
    it('detects success and failure states', () => {
      expect(isFiscalResultSuccess({ status: 'SUCCESS' })).toBe(true)
      expect(isFiscalResultFailed({ status: 'FAILED' })).toBe(true)
      expect(isFiscalResultFailed({ status: 'SUCCESS', lastError: 'x' })).toBe(true)
    })
  })
})
