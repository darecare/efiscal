import { describe, expect, it } from 'vitest'
import {
  advancePaymentDateTimeBounds,
  calcLineTaxValue,
  calcPaymentMatchAmount,
  calcTotalAmount,
  composeBuyerCostCenterId,
  isAdvanceSale,
  toDateTimeLocalValue,
  validateAdvancePaymentDateTime,
  FALLBACK_TAX_LABELS,
  FALLBACK_TAX_LABEL_OPTIONS,
  inferBuyerTypeFromNumericId,
  isFiscalResultFailed,
  isFiscalResultSuccess,
  normalizeTaxLabelOptions,
  sanitizeQuantityInput,
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

  describe('calcLineTaxValue', () => {
    it('extracts VAT from gross line total', () => {
      expect(calcLineTaxValue('1500.00', '20')).toBe('250.00')
    })

    it('returns zero when rate or total is missing', () => {
      expect(calcLineTaxValue('', '20')).toBe('0.00')
      expect(calcLineTaxValue('100.00', null)).toBe('0.00')
    })
  })

  describe('sanitizeQuantityInput', () => {
    it('limits manual entry to two decimal places', () => {
      expect(sanitizeQuantityInput('1.234')).toBe('1.23')
      expect(sanitizeQuantityInput('2.5')).toBe('2.5')
    })

    it('strips non-numeric characters', () => {
      expect(sanitizeQuantityInput('a3.1b2')).toBe('3.12')
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

  describe('composeBuyerCostCenterId', () => {
    it('composes type and value', () => {
      expect(composeBuyerCostCenterId('30', '099999999')).toBe('30:099999999')
    })

    it('strips duplicate type prefix from value', () => {
      expect(composeBuyerCostCenterId('30', '30:099999999')).toBe('30:099999999')
    })

    it('returns null when type or value is missing', () => {
      expect(composeBuyerCostCenterId('', '099999999')).toBeNull()
      expect(composeBuyerCostCenterId('30', '  ')).toBeNull()
      expect(composeBuyerCostCenterId(null, '099999999')).toBeNull()
    })
  })

  describe('advance payment date and time', () => {
    const now = new Date('2026-09-18T12:00:00')

    it('recognizes only Advance Sale', () => {
      expect(isAdvanceSale(4, 0)).toBe(true)
      expect(isAdvanceSale('4', '0')).toBe(true)
      expect(isAdvanceSale(4, 1)).toBe(false)
      expect(isAdvanceSale(0, 0)).toBe(false)
    })

    it('bounds the picker to the last three days', () => {
      expect(advancePaymentDateTimeBounds(now)).toEqual({
        min: '2026-09-15T12:00',
        max: '2026-09-18T12:00',
      })
    })

    it('formats dates for datetime-local inputs', () => {
      expect(toDateTimeLocalValue(new Date('2026-01-05T08:07:00'))).toBe('2026-01-05T08:07')
    })

    it('accepts a moment within the allowed window', () => {
      expect(validateAdvancePaymentDateTime('2026-09-17T10:30', now)).toBeNull()
    })

    it('rejects missing, future, and too old values', () => {
      expect(validateAdvancePaymentDateTime('', now)).toBe('required')
      expect(validateAdvancePaymentDateTime('not-a-date', now)).toBe('invalid')
      expect(validateAdvancePaymentDateTime('2026-09-18T12:01', now)).toBe('future')
      expect(validateAdvancePaymentDateTime('2026-09-15T11:59', now)).toBe('tooOld')
    })
  })
})
