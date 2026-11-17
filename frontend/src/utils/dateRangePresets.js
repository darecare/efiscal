import {
  addDays,
  addMonths,
  differenceInCalendarDays,
  endOfDay,
  endOfMonth,
  endOfWeek,
  isSameDay,
  startOfDay,
  startOfMonth,
  startOfWeek,
} from 'date-fns'
import { createStaticRanges } from 'react-date-range'

/**
 * Sidebar presets for react-date-range DateRangePicker.
 * The library's defaultStaticRanges / defaultInputRanges are English-only;
 * locale= only affects the calendar grid, not these labels.
 */
export function buildStaticRanges(t) {
  const now = new Date()
  const defineds = {
    startOfWeek: startOfWeek(now),
    endOfWeek: endOfWeek(now),
    startOfLastWeek: startOfWeek(addDays(now, -7)),
    endOfLastWeek: endOfWeek(addDays(now, -7)),
    startOfToday: startOfDay(now),
    endOfToday: endOfDay(now),
    startOfYesterday: startOfDay(addDays(now, -1)),
    endOfYesterday: endOfDay(addDays(now, -1)),
    startOfMonth: startOfMonth(now),
    endOfMonth: endOfMonth(now),
    startOfLastMonth: startOfMonth(addMonths(now, -1)),
    endOfLastMonth: endOfMonth(addMonths(now, -1)),
  }

  return createStaticRanges([
    {
      label: t('dateRangePresets.today'),
      range: () => ({ startDate: defineds.startOfToday, endDate: defineds.endOfToday }),
    },
    {
      label: t('dateRangePresets.yesterday'),
      range: () => ({ startDate: defineds.startOfYesterday, endDate: defineds.endOfYesterday }),
    },
    {
      label: t('dateRangePresets.thisWeek'),
      range: () => ({ startDate: defineds.startOfWeek, endDate: defineds.endOfWeek }),
    },
    {
      label: t('dateRangePresets.lastWeek'),
      range: () => ({ startDate: defineds.startOfLastWeek, endDate: defineds.endOfLastWeek }),
    },
    {
      label: t('dateRangePresets.thisMonth'),
      range: () => ({ startDate: defineds.startOfMonth, endDate: defineds.endOfMonth }),
    },
    {
      label: t('dateRangePresets.lastMonth'),
      range: () => ({ startDate: defineds.startOfLastMonth, endDate: defineds.endOfLastMonth }),
    },
  ])
}

export function buildInputRanges(t) {
  return [
    {
      label: t('dateRangePresets.daysUpToToday'),
      range(value) {
        const today = startOfDay(new Date())
        return {
          startDate: addDays(today, (Math.max(Number(value), 1) - 1) * -1),
          endDate: endOfDay(new Date()),
        }
      },
      getCurrentValue(range) {
        const today = endOfDay(new Date())
        if (!range?.endDate || !isSameDay(range.endDate, today)) return '-'
        if (!range.startDate) return '∞'
        return differenceInCalendarDays(today, range.startDate) + 1
      },
    },
    {
      label: t('dateRangePresets.daysStartingToday'),
      range(value) {
        const today = new Date()
        return {
          startDate: today,
          endDate: addDays(today, Math.max(Number(value), 1) - 1),
        }
      },
      getCurrentValue(range) {
        const today = startOfDay(new Date())
        if (!range?.startDate || !isSameDay(range.startDate, today)) return '-'
        if (!range.endDate) return '∞'
        return differenceInCalendarDays(range.endDate, today) + 1
      },
    },
  ]
}
