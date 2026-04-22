import React, { useState, useRef, useEffect, useMemo } from 'react'
import { useNavigate, Link, useLocation, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import api from '../services/api'
import { IconDashboard, IconOrders, IconAccount, IconUsers, IconProfile, IconLogOut, IconSearch, IconLoader } from '../components/Icons'

const getSupplierMinDate = () => {
  const d = new Date()
  d.setDate(d.getDate() - 14)
  return d.toISOString().split('T')[0]
}

const Orders = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()

  const isSupplier = user?.role === 'dobavljac'
  const supplierMinDate = getSupplierMinDate()

  // Filter states (initialized from URL params)
  const [createdAfter, setCreatedAfter] = useState(() => {
    const param = searchParams.get('created_after')
    if (param) return param
    if (user?.role === 'dobavljac') return getSupplierMinDate()
    return ''
  })
  const [shippingStatus, setShippingStatus] = useState(() => searchParams.get('shipping_status') || 'awaiting')
  const [paymentStatusFilter, setPaymentStatusFilter] = useState(() => searchParams.get('payment_status') || '')
  const [paymentMethodFilter, setPaymentMethodFilter] = useState(() => searchParams.get('payment_method') || '')
  const [vendorFilter, setVendorFilter] = useState(() => searchParams.get('vendor') || '')
  const [commercialistFilter, setCommercialistFilter] = useState(() => searchParams.get('commercialist') || '')

  // Data states
  const [allOrders, setAllOrders] = useState([])
  const [filteredOrders, setFilteredOrders] = useState([])
  const [totalRecords, setTotalRecords] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [hasFetchedOrders, setHasFetchedOrders] = useState(false)
  const [isCombinedView, setIsCombinedView] = useState(false)

  // Pagination states
  const [currentPage, setCurrentPage] = useState(1)
  const pageSize = 100

  // Selection states
  const [selectedIds, setSelectedIds] = useState(new Set())
  const [selectAllMatching, setSelectAllMatching] = useState(false)
  const selectAllOnPageRef = useRef(null)

  // Sticky scrollbar refs
  const tableWrapperRef = useRef(null)
  const stickyScrollRef = useRef(null)

  // Status update modal states
  const [statusModalOpen, setStatusModalOpen] = useState(false)
  const [itemsToUpdate, setItemsToUpdate] = useState([])
  const [selectedStatus, setSelectedStatus] = useState('')
  const [productStatuses, setProductStatuses] = useState([])
  const [updating, setUpdating] = useState(false)
  const [updateProgress, setUpdateProgress] = useState(0)
  const [summaryModalOpen, setSummaryModalOpen] = useState(false)
  const [updateResults, setUpdateResults] = useState(null)

  // Grouped-by-order view
  const [isGroupedView, setIsGroupedView] = useState(false)
  const [expandedOrderIds, setExpandedOrderIds] = useState(() => new Set())

  // Tag modal state
  const [tagModalOpen, setTagModalOpen] = useState(false)
  const [tagInput, setTagInput] = useState('')

  const getRowId = (row, index) => {
    if (row.line_item_id != null) return `${row.order_id}-${row.line_item_id}`
    return `${row.order_id}-${row.product_id}-${index}`
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleDateChange = (value) => {
    if (isSupplier && value && value < supplierMinDate) {
      return
    }
    setCreatedAfter(value)
  }

  const getClientFilteredData = (data, overrides = {}) => {
    const effectivePaymentStatus = overrides.paymentStatusFilter ?? paymentStatusFilter
    const effectivePaymentMethod = overrides.paymentMethodFilter ?? paymentMethodFilter
    const effectiveVendor = overrides.vendorFilter ?? vendorFilter
    const effectiveCommercialist = overrides.commercialistFilter ?? commercialistFilter
    let filtered = [...data]

    if (effectivePaymentStatus) {
      filtered = filtered.filter(row => row.payment_status === effectivePaymentStatus)
    }
    if (effectivePaymentMethod) {
      filtered = filtered.filter(row => row.payment_method === effectivePaymentMethod)
    }
    if (effectiveVendor) {
      filtered = filtered.filter(row => row.vendor === effectiveVendor)
    }
    if (effectiveCommercialist) {
      filtered = filtered.filter(row => row.commercialist === effectiveCommercialist)
    }

    return filtered
  }

  const fetchOrders = async () => {
    try {
      setLoading(true)
      setError(null)

      const params = { limit: pageSize, sort: 'date_created.desc' }
      if (createdAfter) params.created_after = createdAfter
      if (shippingStatus) params.shipping_status = shippingStatus

      let allFetched = []
      let start = 0
      let totalOrders = Infinity

      while (start < totalOrders) {
        const response = await api.get('/orders/', { params: { ...params, start } })
        const data = response.data.data || []
        const meta = response.data.meta || {}

        allFetched = [...allFetched, ...data]
        if (totalOrders === Infinity && meta.total != null) totalOrders = meta.total

        if (data.length === 0) break
        start += pageSize
      }

      const filteredNonNegative = allFetched.filter((row) => !isNegativeQuantity(row?.quantity))
      setAllOrders(filteredNonNegative)
      setTotalRecords(filteredNonNegative.length)
      return filteredNonNegative
    } catch (err) {
      const errorMessage = err.response?.data?.detail || 'Neuspešno preuzimanje porudžbina.'
      setError(errorMessage)
      console.error('Greška prilikom preuzimanja porudžbina:', err)
      return null
    } finally {
      setLoading(false)
    }
  }

  const applyClientFilters = (data) => {
    const filtered = getClientFilteredData(data)
    setFilteredOrders(filtered)
    setCurrentPage(1)
  }

  const handleFetchOrders = () => {
    setIsCombinedView(false)
    setHasFetchedOrders(true)
    setCurrentPage(1)
    fetchOrders()
  }

  const COMBINED_PROCESSING_COMBOS = [
    { payment_status: 'awaiting', payment_method_code: 'cash_delivery' },
    { payment_status: 'paid', payment_method_code: 'wire' },
    { payment_status: 'paid', payment_method_code: 'intesa' }
  ]

  const refreshAfterBulkAction = async (selectionSnapshot) => {
    let refreshedOrders = null
    if (isCombinedView) {
      refreshedOrders = await fetchCombinedOrders()
    } else {
      refreshedOrders = await fetchOrders()
    }

    if (!selectionSnapshot || !refreshedOrders) return

    if (selectionSnapshot.selectAllMatching) {
      setSelectAllMatching(true)
      setSelectedIds(new Set())
      return
    }

    const refreshedFiltered = getClientFilteredData(
      refreshedOrders,
      isCombinedView ? { paymentStatusFilter: '', paymentMethodFilter: '' } : {}
    )
    const restoredIds = new Set()
    refreshedFiltered.forEach((row, index) => {
      const rowId = getRowId(row, index)
      if (selectionSnapshot.selectedIds.has(rowId)) {
        restoredIds.add(rowId)
      }
    })

    setSelectedIds(restoredIds)
    setSelectAllMatching(false)
  }

  const fetchCombinedOrders = async () => {
    try {
      setLoading(true)
      setError(null)

      const fetchOneCombo = async (combo) => {
        let allFetched = []
        let start = 0
        let totalOrders = Infinity
        const baseParams = {
          limit: pageSize,
          sort: 'date_created.desc',
          shipping_status: 'awaiting',
          payment_status: combo.payment_status,
          payment_method_code: combo.payment_method_code
        }
        if (createdAfter) baseParams.created_after = createdAfter
        while (start < totalOrders) {
          const response = await api.get('/orders/', { params: { ...baseParams, start } })
          const data = response.data.data || []
          const meta = response.data.meta || {}
          allFetched = [...allFetched, ...data]
          if (totalOrders === Infinity && meta.total != null) totalOrders = meta.total
          if (data.length === 0) break
          start += pageSize
        }
        return allFetched
      }

      const results = await Promise.all(COMBINED_PROCESSING_COMBOS.map(fetchOneCombo))
      const merged = results.flat()
      const filteredNonNegative = merged.filter((row) => !isNegativeQuantity(row?.quantity))
      setAllOrders(filteredNonNegative)
      setTotalRecords(filteredNonNegative.length)
      setPaymentStatusFilter('')
      setPaymentMethodFilter('')
      setIsCombinedView(true)
      setHasFetchedOrders(true)
      setCurrentPage(1)
      return filteredNonNegative
    } catch (err) {
      const errorMessage = err.response?.data?.detail || 'Neuspešno preuzimanje porudžbina.'
      setError(errorMessage)
      console.error('Greška prilikom preuzimanja porudžbina za obradu:', err)
      return null
    } finally {
      setLoading(false)
    }
  }

  const handlePageChange = (newPage) => {
    if (!hasFetchedOrders) return
    if (newPage < 1 || newPage > totalPages || newPage === currentPage) return
    setCurrentPage(newPage)
  }

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    const date = new Date(dateString)
    return date.toLocaleString('sr-RS', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  const formatCurrency = (amount, currency) => {
    if (!amount) return 'N/A'
    return `${amount.toLocaleString('sr-RS')} ${currency || ''}`
  }

  const isNegativeQuantity = (q) => {
    if (q == null || q === '') return false
    const n = typeof q === 'number' ? q : Number(String(q).replace(',', '.'))
    return Number.isFinite(n) && n < 0
  }

  const getStatusStyle = (color) => {
    if (!color) return {}
    return {
      backgroundColor: color.substring(0, 7),
      opacity: 0.8
    }
  }

  const getPaymentStatusStyle = (status) => {
    const colors = {
      'Plaćanje na čekanju': '#fff7df',
      'Plaćanje je uspešno': '#eaf7e1',
      'Plaćanje nije uspelo': '#feebef',
      'Plaćanje je otkazano': '#feebef',
      'Plaćanje je odbijeno': '#feebef',
      'Uplata je refundirana': '#e5f1fe',
      'Parcijalno plaćeno': '#e5f1fe'
    }
    return colors[status] ? { backgroundColor: colors[status] } : {}
  }

  const handleRowCheckbox = (rowId) => {
    const newSelectedIds = new Set(selectedIds)
    if (newSelectedIds.has(rowId)) {
      newSelectedIds.delete(rowId)
    } else {
      newSelectedIds.add(rowId)
    }
    setSelectedIds(newSelectedIds)
    setSelectAllMatching(false)
  }

  const handleOrderGroupCheckbox = (group, checked) => {
    const itemIds = group.items.map(item => getRowId(item, item._globalIndex))
    const newSet = new Set(selectedIds)

    // In grouped view, the order checkbox is a "bulk select" control:
    // - checked true  => select ALL product line items in this order
    // - checked false => deselect ALL product line items in this order
    if (checked) {
      itemIds.forEach(id => newSet.add(id))
    } else {
      itemIds.forEach(id => newSet.delete(id))
    }

    setSelectedIds(newSet)
    setSelectAllMatching(false)
  }

  const toggleExpandOrder = (orderId) => {
    const oid = String(orderId)
    setExpandedOrderIds(prev => {
      const next = new Set(prev)
      if (next.has(oid)) next.delete(oid)
      else next.add(oid)
      return next
    })
  }

  const handleSelectAllOnPage = (checked) => {
    const startIdx = (currentPage - 1) * pageSize
    let displayedRowIds
    if (isGroupedView && paginatedOrders.length > 0 && paginatedOrders[0].items) {
      displayedRowIds = paginatedOrders.flatMap(group =>
        group.items.map(item => getRowId(item, item._globalIndex))
      )
    } else {
      const pageOrders = filteredOrders.slice(startIdx, startIdx + pageSize)
      displayedRowIds = pageOrders.map((row, index) => getRowId(row, startIdx + index))
    }
    if (checked) {
      const newSelectedIds = new Set([...selectedIds, ...displayedRowIds])
      setSelectedIds(newSelectedIds)
    } else {
      const newSelectedIds = new Set(selectedIds)
      displayedRowIds.forEach(id => newSelectedIds.delete(id))
      setSelectedIds(newSelectedIds)
    }
    setSelectAllMatching(false)
  }

  const handleSelectAllMatching = (checked) => {
    setSelectAllMatching(checked)
    if (checked) {
      setSelectedIds(new Set())
    }
  }

  // Action state
  const [selectedAction, setSelectedAction] = useState('')

  const getSelectionCount = () => {
    if (selectAllMatching) return filteredOrders.length
    return selectedIds.size
  }

  const hasSelection = selectAllMatching || selectedIds.size > 0

  const handleApplyAction = () => {
    if (!selectedAction || !hasSelection) return

    switch (selectedAction) {
      case 'edit_product_status':
        handleEditProductStatus()
        break
      case 'download_commercialist_files':
        handleGenerateCommercialistFiles()
        break
      case 'add_tags':
        setTagModalOpen(true)
        break
      case 'mark_collected':
        handleBulkUpdateTags(['Preuzeto'])
        break
      default:
        alert('Nepoznata akcija')
    }
  }

  const getSelectedOrderCount = () => {
    if (selectAllMatching) {
      const orderIds = new Set(filteredOrders.map(r => String(r.order_id)))
      return orderIds.size
    }
    const orderIds = new Set()
    filteredOrders.forEach((row, index) => {
      if (selectedIds.has(getRowId(row, index))) orderIds.add(String(row.order_id))
    })
    return orderIds.size
  }

  const handleBulkUpdateTags = async (tags) => {
    if (!tags || tags.length === 0) {
      if (tagModalOpen) setTagModalOpen(false)
      return
    }
    const selectedIdsList = selectAllMatching
      ? [...new Set(filteredOrders.map((row, index) => getRowId(row, index)))]
      : Array.from(selectedIds)
    if (selectedIdsList.length === 0) {
      alert('Nema odabranih stavki')
      return
    }
    try {
      setUpdating(true)
      setTagModalOpen(false)
      setTagInput('')
      const selectionSnapshot = {
        selectAllMatching,
        selectedIds: new Set(selectedIds)
      }
      const response = await api.post('/orders/bulk-update-tags', {
        selected_ids: selectedIdsList,
        tags
      })
      setUpdateResults(response.data)
      setSummaryModalOpen(true)
      setSelectedAction('')
      await refreshAfterBulkAction(selectionSnapshot)
    } catch (err) {
      console.error('Neuspešno ažuriranje tagova:', err)
      alert(err.response?.data?.detail || 'Neuspešno ažuriranje tagova')
    } finally {
      setUpdating(false)
    }
  }

  const handleEditProductStatus = () => {
    let rowsToProcess
    if (selectAllMatching) {
      rowsToProcess = filteredOrders
    } else {
      rowsToProcess = filteredOrders.filter((row, index) =>
        selectedIds.has(getRowId(row, index))
      )
    }

    if (rowsToProcess.length === 0) {
      alert('Nema odabranih stavki')
      return
    }

    setStatusModalOpen(true)
    setItemsToUpdate(rowsToProcess)
  }

  const handleGenerateCommercialistFiles = () => {
    try {
      let rowsToProcess
      if (selectAllMatching) {
        rowsToProcess = filteredOrders
      } else {
        rowsToProcess = filteredOrders.filter((row, index) =>
          selectedIds.has(getRowId(row, index))
        )
      }

      if (rowsToProcess.length === 0) {
        alert('Nema odabranih stavki')
        return
      }

      const normalizeWarehouse = (value) =>
        String(value || '')
          .toLowerCase()
          .trim()
          .replace(/\s+/g, ' ')

      const isVpLager = (row) => normalizeWarehouse(row?.warehouse) === 'vp lager roba'

      const downloadTxt = (filename, content) => {
        if (!content?.trim()) return false
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = filename
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
        return true
      }

      const byCommercialist = {}
      rowsToProcess.forEach(row => {
        const commercialist = row.commercialist || 'unknown'
        if (!byCommercialist[commercialist]) {
          byCommercialist[commercialist] = []
        }
        byCommercialist[commercialist].push(row)
      })

      const today = new Date().toISOString().split('T')[0]
      let generatedCount = 0

      Object.entries(byCommercialist).forEach(([commercialist, rows]) => {
        const baseFilename = `spisak_${commercialist.toLowerCase().replace(/\s+/g, '_')}_${today}`

        // Group all rows by vendor
        const byVendor = {}
        rows.forEach((row) => {
          const vendor = row.vendor || 'Nepoznat'
          if (!byVendor[vendor]) byVendor[vendor] = []
          byVendor[vendor].push(row)
        })

        const vendorsSorted = Object.keys(byVendor).sort((a, b) =>
          String(a).localeCompare(String(b), 'sr', { sensitivity: 'base' })
        )

        let content = ''

        vendorsSorted.forEach((vendor) => {
          const vendorRows = byVendor[vendor]
          const vpVendorRows = vendorRows.filter((r) => isVpLager(r))
          const standardVendorRows = vendorRows.filter((r) => !isVpLager(r))
          const isMixed = vpVendorRows.length > 0 && standardVendorRows.length > 0

          content += `Dobavljač: ${vendor}\n\n`

          // VP section (flat list, no order block numbers, no customer info)
          if (vpVendorRows.length > 0) {
            if (isMixed) content += `VP:\n`
            vpVendorRows.forEach((row) => {
              content += `${row.quantity || 1} kom ${row.product_name || 'N/A'}`
              const sifra = row.sifra_dobavljaca || row.supplier_sku
              if (sifra) content += ` (${sifra})`
              content += '\n'
            })
            content += '\n'
          }

          // Standard section (numbered order blocks with customer info)
          if (standardVendorRows.length > 0) {
            if (isMixed) content += `--------------------------------\nOstale:\n`

            const byOrderId = {}
            standardVendorRows.forEach((row) => {
              const oid = String(row.order_id ?? '')
              if (!byOrderId[oid]) byOrderId[oid] = []
              byOrderId[oid].push(row)
            })

            let blockIndex = 0
            Object.entries(byOrderId).forEach(([, orderRows]) => {
              blockIndex += 1
              const first = orderRows[0]
              const customerName = first.shipping_name || 'N/A'
              const grad = first.shipping_city || first.shipping_state || 'N/A'

              content += `${blockIndex}.\n`
              orderRows.forEach((row) => {
                content += `${row.quantity || 1} kom ${row.product_name || 'N/A'}`
                const sifra = row.sifra_dobavljaca || row.supplier_sku
                if (sifra) content += ` (${sifra})`
                content += '\n'
              })
              content += `Ime kupca: ${customerName}, ${grad}\n\n`
            })
          }
        })

        if (downloadTxt(`${baseFilename}.txt`, content)) generatedCount += 1
      })

      alert(`Generisano ${generatedCount} fajl(ova)`)
    } catch (err) {
      console.error('Greška prilikom preuzimanja fajlova:', err)
      alert('Neuspešno preuzimanje fajlova')
    }
  }

  // Auto-fetch when landing with filter params in URL
  useEffect(() => {
    const hasParams =
      searchParams.get('created_after') ||
      searchParams.get('shipping_status') ||
      searchParams.get('payment_status') ||
      searchParams.get('payment_method') ||
      searchParams.get('vendor') ||
      searchParams.get('commercialist')
    if (hasParams) {
      setHasFetchedOrders(true)
      fetchOrders()
    }
  }, [])

  // Load product statuses on mount
  useEffect(() => {
    const loadProductStatuses = async () => {
      try {
        const response = await api.get('/config/product-statuses')
        setProductStatuses(response.data)
      } catch (err) {
        console.error('Neuspešno učitavanje statusa proizvoda:', err)
      }
    }
    loadProductStatuses()
  }, [])

  const handleUpdateProductStatus = async () => {
    if (!selectedStatus) {
      alert('Molimo odaberite status proizvoda')
      return
    }

    try {
      setUpdating(true)
      setUpdateProgress(0)
      setStatusModalOpen(false)
      const selectionSnapshot = {
        selectAllMatching,
        selectedIds: new Set(selectedIds)
      }

      const selectedIdsList = Array.from(
        selectAllMatching
          ? new Set(filteredOrders.map((row, index) => getRowId(row, index)))
          : selectedIds
      )

      const filters = { sort: 'date_created.desc' }
      if (createdAfter) filters.created_after = createdAfter
      if (isCombinedView) {
        filters.shipping_status = 'awaiting'
        filters.combined_combos = COMBINED_PROCESSING_COMBOS
      } else if (shippingStatus) {
        filters.shipping_status = shippingStatus
      }

      const response = await api.post('/orders/bulk-update-status', {
        selected_ids: selectedIdsList,
        status_id: parseInt(selectedStatus),
        filters: filters
      })

      setUpdateResults(response.data)
      setSummaryModalOpen(true)

      setSelectedAction('')

      await refreshAfterBulkAction(selectionSnapshot)

    } catch (err) {
      console.error('Neuspešno ažuriranje statusa proizvoda:', err)
      alert(err.response?.data?.detail || 'Neuspešno ažuriranje statusa proizvoda')
    } finally {
      setUpdating(false)
      setUpdateProgress(0)
    }
  }

  // Clear combined view when fetch filters change
  useEffect(() => {
    setIsCombinedView(false)
  }, [createdAfter, shippingStatus])

  // Clear selection when filters change
  useEffect(() => {
    setSelectedIds(new Set())
    setSelectAllMatching(false)
  }, [createdAfter, shippingStatus, paymentStatusFilter, paymentMethodFilter, vendorFilter, commercialistFilter])

  // Apply client filters whenever fetched data or any client-side filter changes.
  useEffect(() => {
    applyClientFilters(allOrders)
  }, [allOrders, paymentStatusFilter, paymentMethodFilter, vendorFilter, commercialistFilter])

  // Sync URL with applied filters only
  useEffect(() => {
    const params = new URLSearchParams()
    if (createdAfter) params.set('created_after', createdAfter)
    if (shippingStatus) params.set('shipping_status', shippingStatus)
    if (paymentStatusFilter) params.set('payment_status', paymentStatusFilter)
    if (paymentMethodFilter) params.set('payment_method', paymentMethodFilter)
    if (vendorFilter) params.set('vendor', vendorFilter)
    if (commercialistFilter) params.set('commercialist', commercialistFilter)
    setSearchParams(params, { replace: true })
  }, [createdAfter, shippingStatus, paymentStatusFilter, paymentMethodFilter, vendorFilter, commercialistFilter])

  // Sync sticky scrollbar with table horizontal scroll
  useEffect(() => {
    const tableWrapper = tableWrapperRef.current
    const stickyScroll = stickyScrollRef.current

    if (!tableWrapper || !stickyScroll) return

    const handleTableScroll = () => {
      stickyScroll.scrollLeft = tableWrapper.scrollLeft
    }

    const handleStickyScroll = () => {
      tableWrapper.scrollLeft = stickyScroll.scrollLeft
    }

    // Hide sticky scrollbar when reaching bottom of page
    const handlePageScroll = () => {
      const scrollHeight = document.documentElement.scrollHeight
      const scrollTop = window.scrollY || document.documentElement.scrollTop
      const clientHeight = window.innerHeight
      const distanceFromBottom = scrollHeight - (scrollTop + clientHeight)
      
      // Hide when within 100px of bottom
      if (distanceFromBottom < 100) {
        stickyScroll.style.display = 'none'
      } else {
        stickyScroll.style.display = 'block'
      }
    }

    tableWrapper.addEventListener('scroll', handleTableScroll)
    stickyScroll.addEventListener('scroll', handleStickyScroll)
    window.addEventListener('scroll', handlePageScroll)

    // Update sticky scrollbar width and position to match table wrapper
    const updateScrollbar = () => {
      const table = tableWrapper.querySelector('table')
      if (table) {
        const scrollContent = stickyScroll.querySelector('.scroll-content')
        if (scrollContent) {
          scrollContent.style.width = `${table.scrollWidth}px`
        }
        
        // Match the scrollbar width and position to the table wrapper
        const rect = tableWrapper.getBoundingClientRect()
        stickyScroll.style.left = `${rect.left}px`
        stickyScroll.style.width = `${rect.width}px`
        
        console.log('Sticky scroll updated:', {
          tableWidth: table.scrollWidth,
          wrapperLeft: rect.left,
          wrapperWidth: rect.width
        })
      }
      
      // Check initial scroll position
      handlePageScroll()
    }

    // Use setTimeout to ensure table is fully rendered
    setTimeout(updateScrollbar, 100)
    
    const resizeObserver = new ResizeObserver(updateScrollbar)
    const table = tableWrapper.querySelector('table')
    if (table) {
      resizeObserver.observe(table)
      resizeObserver.observe(tableWrapper)
    }
    
    // Update on window resize
    window.addEventListener('resize', updateScrollbar)

    return () => {
      tableWrapper.removeEventListener('scroll', handleTableScroll)
      stickyScroll.removeEventListener('scroll', handleStickyScroll)
      window.removeEventListener('scroll', handlePageScroll)
      window.removeEventListener('resize', updateScrollbar)
      resizeObserver.disconnect()
    }
  }, [filteredOrders])

  const groupedOrders = useMemo(() => {
    if (!isGroupedView) return null
    const groups = new Map()
    filteredOrders.forEach((row, globalIndex) => {
      const oid = String(row.order_id)
      if (!groups.has(oid)) {
        groups.set(oid, {
          order_id: row.order_id,
          shipping_name: row.shipping_name,
          payment_status: row.payment_status,
          payment_method: row.payment_method,
          shipping_status: row.shipping_status,
          date_created: row.date_created,
          tags: row.tags || [],
          items: []
        })
      }
      groups.get(oid).items.push({ ...row, _globalIndex: globalIndex })
    })
    return Array.from(groups.values())
  }, [filteredOrders, isGroupedView])

  const totalPages = isGroupedView
    ? Math.ceil((groupedOrders?.length || 0) / pageSize) || 1
    : Math.ceil(filteredOrders.length / pageSize) || 1
  const startIndex = (currentPage - 1) * pageSize
  const paginatedOrders = isGroupedView
    ? (groupedOrders?.slice(startIndex, startIndex + pageSize) ?? [])
    : filteredOrders.slice(startIndex, startIndex + pageSize)

  const vendorSource = commercialistFilter
    ? allOrders.filter(r => r.commercialist === commercialistFilter)
    : allOrders
  const uniqueVendorsRaw = [...new Set(vendorSource.map(r => r.vendor).filter(Boolean))].sort()
  const uniqueVendors = vendorFilter && !uniqueVendorsRaw.includes(vendorFilter)
    ? [vendorFilter, ...uniqueVendorsRaw].sort()
    : uniqueVendorsRaw

  const commercialistSource = vendorFilter
    ? allOrders.filter(r => r.vendor === vendorFilter)
    : allOrders
  const uniqueCommercialistsRaw = [...new Set(commercialistSource.map(r => r.commercialist).filter(Boolean))].sort()
  const uniqueCommercialists = commercialistFilter && !uniqueCommercialistsRaw.includes(commercialistFilter)
    ? [commercialistFilter, ...uniqueCommercialistsRaw].sort()
    : uniqueCommercialistsRaw

  const uniquePaymentStatusesRaw = [...new Set(allOrders.map(r => r.payment_status).filter(Boolean))].sort()
  const uniquePaymentStatuses = paymentStatusFilter && !uniquePaymentStatusesRaw.includes(paymentStatusFilter)
    ? [paymentStatusFilter, ...uniquePaymentStatusesRaw].sort()
    : uniquePaymentStatusesRaw
  const uniquePaymentMethodsRaw = [...new Set(allOrders.map(r => r.payment_method).filter(Boolean))].sort()
  const uniquePaymentMethods = paymentMethodFilter && !uniquePaymentMethodsRaw.includes(paymentMethodFilter)
    ? [paymentMethodFilter, ...uniquePaymentMethodsRaw].sort()
    : uniquePaymentMethodsRaw

  const hasPreFetchFilter = !!(createdAfter || shippingStatus)

  return (
    <div className="dashboard-layout">
      <nav className="navbar">
        <h1><span className="title-brand">Kliklak</span> <span className="title-product">Dashboard</span></h1>
        <div className="navbar-actions">
          <span className="navbar-welcome">Dobrodošli, {user?.username}</span>
          <Link to="/account" className="account-link" aria-label="Nalog">
            <IconProfile />
          </Link>
          <button onClick={handleLogout} className="btn btn-nav">
            <IconLogOut /> Izlogujte se
          </button>
        </div>
      </nav>

      <div className="main-layout">
        <aside className="sidebar">
          <ul className="sidebar-menu">
            <li>
              <Link
                to="/dashboard"
                className={location.pathname === '/dashboard' ? 'active' : ''}
              >
                <IconDashboard /> Kontrolni panel
              </Link>
            </li>
            <li>
              <Link
                to="/orders"
                className={location.pathname === '/orders' ? 'active' : ''}
              >
                <IconOrders /> Obrada porudžbina
              </Link>
            </li>
            <li>
              <Link
                to="/account"
                className={location.pathname === '/account' ? 'active' : ''}
              >
                <IconAccount /> Nalog
              </Link>
            </li>
            {user?.role === 'superuser' && (
              <li>
                <Link
                  to="/users"
                  className={location.pathname === '/users' ? 'active' : ''}
                >
                  <IconUsers /> Korisnici
                </Link>
              </li>
            )}
          </ul>
        </aside>

        <main className="dashboard-content">
          <div className="orders-header">
            <h2 className="page-title-with-icon"><IconOrders /> Obrada porudžbina</h2>
            <p>
              {isSupplier
                ? `Porudžbine za dobavljača: ${user?.vendor_name || '—'}`
                : 'Preuzmite i upravljajte porudžbinama sa MerchantPro'}
            </p>
          </div>

          {/* Filters */}
          <div className="card filters-card">
            <h3>Filteri</h3>

            <div className="filters-section filters-section-prefetch">
              <h4 className="filters-section-heading">Filteri za preuzimanje</h4>
              <div className="filters-grid">
                <div className="filter-group">
                  <label htmlFor="createdAfter">
                    Datum od
                    {isSupplier && <span className="hint"> (max 2 nedelje unazad)</span>}
                  </label>
                  <input
                    type="date"
                    id="createdAfter"
                    value={createdAfter}
                    min={isSupplier ? supplierMinDate : undefined}
                    onChange={(e) => handleDateChange(e.target.value)}
                    required={isSupplier}
                  />
                </div>

                <div className="filter-group">
                  <label htmlFor="shippingStatus">Status isporuke</label>
                  <select
                    id="shippingStatus"
                    value={shippingStatus}
                    onChange={(e) => setShippingStatus(e.target.value)}
                  >
                    {/* <option value="">Svi</option> */}
                    {/* <option value="temporary">Privremeno</option> */}
                    <option value="awaiting">Na čekanju</option>
                    <option value="confirmed">Potvrđeno</option>
                    {/* <option value="in_process">U obradi</option> */}
                    {/* <option value="shipped">Poslato</option> */}
                    {/* <option value="delivered">Isporučeno</option> */}
                    {/* <option value="returned">Vraćeno</option> */}
                    {/* <option value="cancelled">Otkazano</option> */}
                  </select>
                </div>

                <div className="filter-group filter-group-buttons">
                  <button
                    onClick={handleFetchOrders}
                    className="btn btn-primary"
                    disabled={loading || !hasPreFetchFilter}
                    title={!hasPreFetchFilter ? 'Izaberite bar jedan filter za preuzimanje' : ''}
                  >
                    {loading ? <><IconLoader /> Učitavanje...</> : <><IconSearch /> Preuzmite porudžbine</>}
                  </button>
                  {!isSupplier && (
                    <button
                      type="button"
                      onClick={fetchCombinedOrders}
                      className="btn btn-combined"
                      disabled={loading}
                      title="Preuzmi porudžbine za obradu (3 kombinacije: pouzeće, virman, kartica)"
                    >
                      Preuzmite za obradu
                    </button>
                  )}
                </div>
              </div>
            </div>

            <div className="filters-section filters-section-client">
              <h4 className="filters-section-heading">Filteri nad preuzetim podacima</h4>
              {!hasFetchedOrders && (
                <p className="filters-section-hint">Preuzmite porudžbine da biste koristili ove filtere.</p>
              )}
              <div className="filters-grid">
                <div className="filter-group">
                  <label htmlFor="paymentStatus">Status plaćanja</label>
                  <select
                    id="paymentStatus"
                    value={paymentStatusFilter}
                    onChange={(e) => setPaymentStatusFilter(e.target.value)}
                    disabled={!hasFetchedOrders}
                  >
                    <option value="">Svi</option>
                    {uniquePaymentStatuses.map(p => (
                      <option key={p} value={p}>{p}</option>
                    ))}
                  </select>
                </div>

                <div className="filter-group">
                  <label htmlFor="paymentMethod">Način plaćanja</label>
                  <select
                    id="paymentMethod"
                    value={paymentMethodFilter}
                    onChange={(e) => setPaymentMethodFilter(e.target.value)}
                    disabled={!hasFetchedOrders}
                  >
                    <option value="">Svi</option>
                    {uniquePaymentMethods.map(m => (
                      <option key={m} value={m}>{m}</option>
                    ))}
                  </select>
                </div>

                {!isSupplier && (
                  <div className="filter-group">
                    <label htmlFor="vendor">Dobavljač</label>
                    <select
                      id="vendor"
                      value={vendorFilter}
                      onChange={(e) => setVendorFilter(e.target.value)}
                      disabled={!hasFetchedOrders}
                    >
                      <option value="">Svi</option>
                      {uniqueVendors.map(v => (
                        <option key={v} value={v}>{v}</option>
                      ))}
                    </select>
                  </div>
                )}

                {!isSupplier && (
                  <div className="filter-group">
                    <label htmlFor="commercialist">Komercijalista</label>
                    <select
                      id="commercialist"
                      value={commercialistFilter}
                      onChange={(e) => setCommercialistFilter(e.target.value)}
                      disabled={!hasFetchedOrders}
                    >
                      <option value="">Svi</option>
                      {uniqueCommercialists.map(c => (
                        <option key={c} value={c}>{c}</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="card error-card">
              <p className="error-message">{error}</p>
            </div>
          )}

          {/* Loading State */}
          {loading && (
            <div className="loading">Učitavanje porudžbina...</div>
          )}

          {/* Action Bar */}
          {!loading && allOrders.length > 0 && (
            <div className="action-bar">
              <div className="action-bar-left">
                <label className="action-bar-checkbox">
                  <input
                    ref={selectAllOnPageRef}
                    type="checkbox"
                    onChange={(e) => handleSelectAllOnPage(e.target.checked)}
                    aria-label="Izaberite sve stavke na stranici"
                  />
                  <span>Stranica</span>
                </label>
                <label className="action-bar-checkbox">
                  <input
                    type="checkbox"
                    checked={selectAllMatching}
                    onChange={(e) => handleSelectAllMatching(e.target.checked)}
                    aria-label="Izaberite sve (sve stranice)"
                  />
                  <span>Sve</span>
                </label>
                <label className="action-bar-checkbox">
                  <input
                    type="checkbox"
                    checked={isGroupedView}
                    onChange={(e) => {
                      const nextGrouped = e.target.checked
                      setIsGroupedView(nextGrouped)
                      setExpandedOrderIds(new Set())

                      // Keep current selection; only transform when going from orders -> products.
                      if (!nextGrouped) {
                        // orders -> products: expand selection to ALL products within currently selected orders
                        if (!selectAllMatching) {
                          const selectedOrderIds = new Set()
                          selectedIds.forEach((sid) => {
                            const orderIdPart = String(sid).split('-')[0]
                            if (orderIdPart) selectedOrderIds.add(orderIdPart)
                          })

                          const newSelectedIds = new Set()
                          filteredOrders.forEach((row, index) => {
                            if (selectedOrderIds.has(String(row.order_id))) {
                              newSelectedIds.add(getRowId(row, index))
                            }
                          })

                          setSelectedIds(newSelectedIds)
                          // After expansion, "Stranica" no longer necessarily matches the selection subset.
                          if (selectAllOnPageRef.current) selectAllOnPageRef.current.checked = false
                        }
                      }

                      setSelectedAction((prev) => {
                        const productActions = ['edit_product_status', 'download_commercialist_files']
                        const orderActions = ['add_tags', 'mark_collected']
                        if (nextGrouped) {
                          // orders view => product-based actions are unavailable
                          return productActions.includes(prev) ? '' : prev
                        }
                        // products view => order-based actions are unavailable
                        return orderActions.includes(prev) ? '' : prev
                      })
                    }}
                    aria-label="Grupišite po porudžbini"
                  />
                  <span>Grupišite po porudžbini</span>
                </label>
                <span className="selection-count">
                  {hasSelection
                    ? (() => {
                      
                        const count = isGroupedView ? getSelectedOrderCount() : getSelectionCount()
                        
                        const mod10 = count % 10
                        const mod100 = count % 100
                        if (isGroupedView) {
                          
                          const suffix =
                            mod100 >= 10 && mod100 <= 20
                              ? 'a'
                              : mod10 === 1
                                ? 'a'
                                : mod10 > 1 && mod10 < 5
                                  ? 'e'
                                  : 'a'

                          const suffix2 =
                            mod100 >= 10 && mod100 <= 20
                              ? 'o'
                              : mod10 === 1
                                ? 'a'
                                : mod10 > 1 && mod10 < 5
                                  ? 'e'
                                  : 'o'
                          const noun = 'porudžbin' + suffix
                          const adj = 'izabran' + suffix2
                          return `${count} ${noun} ${adj}`
                        }
                        const suffix = mod10 == 1 ? '' : 'a'
                        const suffix2 =
                          mod100 >= 10 && mod100 <= 20
                          ? 'o'
                          : mod10   === 1
                              ? ''
                              : mod10 > 1 && mod10 < 5
                                ? 'a'
                                : 'o'
                        return `${count} Proizvod${suffix} izabran${suffix2}`
                      })()
                    : isGroupedView ? 'Nema izabranih porudžbina' : 'Nema izabranih stavki'}
                </span>
              </div>
              <div className="action-bar-right">
                <select
                  className="action-select"
                  value={selectedAction}
                  onChange={(e) => {
                    setSelectedAction(e.target.value)
                  }}
                  disabled={!hasSelection}
                >
                  <option value="" disabled>
                    {isGroupedView ? 'Akcije nad porudžbinama' : 'Akcije nad proizvodima'}
                  </option>
                  {!isGroupedView ? (
                    <>
                      <option value="edit_product_status">
                        Izmenite statuse proizvoda u porudžbinama
                      </option>
                      {!isSupplier && (
                        <option value="download_commercialist_files">
                          Preuzmite fajlove
                        </option>
                      )}
                    </>
                  ) : (
                    <>
                      <option value="add_tags">Dodajte nove tagove</option>
                      <option value="mark_collected">Označi porudžbine kao preuzete</option>
                    </>
                  )}
                </select>
                <button
                  className="btn btn-primary action-apply-btn"
                  onClick={handleApplyAction}
                  disabled={!hasSelection || !selectedAction}
                >
                  Primenite
                </button>
              </div>
            </div>
          )}

          {/* Combined view badge */}
          {isCombinedView && !loading && (
            <div className="card combined-view-badge" role="status">
              <span>Prikaz: porudžbine za obradu (3 kombinacije)</span>
              <button
                type="button"
                className="btn combined-view-dismiss"
                onClick={() => setIsCombinedView(false)}
                aria-label="Zatvori"
              >
                ×
              </button>
            </div>
          )}

          {/* Orders Table */}
          {!loading && filteredOrders.length > 0 && (
            <>
              <div className="card">
                <div 
                  ref={tableWrapperRef} 
                  className="table-container"
                  style={{ overflowX: 'auto', overflowY: 'visible' }}
                >
                  <table className="orders-table">
                  <thead>
                    <tr>
                      <th className="checkbox-column-narrow"></th>
                      {isGroupedView && <th className="expand-toggle-cell"></th>}
                      <th>Šifra porudžbine</th>
                      <th>Ime kupca</th>
                      <th>Način plaćanja</th>
                      <th>Status plaćanja</th>
                      <th>Status isporuke</th>
                      <th>Datum kreiranja</th>
                      {isGroupedView ? (
                        <>
                          <th>Broj stavki</th>
                          <th>Tagovi</th>
                        </>
                      ) : (
                        <>
                          <th>Naziv proizvoda</th>
                          <th>Količina</th>
                          <th>Status</th>
                          {!isSupplier && <th>Dobavljač</th>}
                          {!isSupplier && <th>Skladište</th>}
                          {!isSupplier && <th>Šifra dobavljača</th>}
                          {!isSupplier && <th>Komercijalista</th>}
                        </>
                      )}
                    </tr>
                  </thead>
                  <tbody>
                    {isGroupedView
                      ? paginatedOrders.map((group) => {
                          const oid = String(group.order_id)
                          const isExpanded = expandedOrderIds.has(oid)
                          const itemIds = group.items.map(item => getRowId(item, item._globalIndex))
                          const someSelected = itemIds.some(id => selectedIds.has(id)) || (selectAllMatching && itemIds.length > 0)
                          return (
                            <React.Fragment key={oid}>
                              <tr
                                className={`order-group-header ${someSelected ? 'selected' : ''}`}
                                onClick={() => toggleExpandOrder(oid)}
                              >
                                <td className="checkbox-column-narrow" onClick={(e) => e.stopPropagation()}>
                                  <input
                                    type="checkbox"
                                    checked={someSelected}
                                    onChange={(e) => handleOrderGroupCheckbox(group, e.target.checked)}
                                    disabled={selectAllMatching}
                                    aria-label={`Izaberite porudžbinu ${oid}`}
                                    onClick={(e) => e.stopPropagation()}
                                  />
                                </td>
                                <td className="expand-toggle-cell">
                                  <button
                                    type="button"
                                    className="expand-toggle"
                                    onClick={(e) => { e.stopPropagation(); toggleExpandOrder(oid) }}
                                    aria-expanded={isExpanded}
                                    aria-label={isExpanded ? 'Zatvori' : 'Proširi'}
                                  >
                                    {isExpanded ? '▼' : '▶'}
                                  </button>
                                </td>
                                <td>{group.order_id || 'N/A'}</td>
                                <td>{group.shipping_name || ''}</td>
                                <td>{group.payment_method || ''}</td>
                                <td>
                                  <span style={{
                                    ...getPaymentStatusStyle(group.payment_status),
                                    padding: '4px 8px',
                                    display: 'inline-block',
                                    borderRadius: '4px'
                                  }}>
                                    {group.payment_status || 'N/A'}
                                  </span>
                                </td>
                                <td>{group.shipping_status || 'N/A'}</td>
                                <td>{formatDate(group.date_created)}</td>
                                <td>{group.items.length}</td>
                                <td className="order-tags-cell">
                                  {(group.tags || []).length
                                    ? (group.tags || []).map(t => (t && (t.name ?? (typeof t === 'string' ? t : ''))) || '').filter(Boolean).join(', ')
                                    : '—'}
                                </td>
                              </tr>
                              {isExpanded && group.items.map((item) => {
                                const rowId = getRowId(item, item._globalIndex)
                                return (
                                  <tr key={rowId} className="order-item-row">
                                    <td className="checkbox-column-narrow" aria-hidden="true" />
                                    <td className="expand-toggle-cell"></td>
                                    <td colSpan={8} className="order-item-details-cell">
                                      <div className="order-item-details">
                                        <span className="order-item-detail order-item-qty">{item.quantity ?? ''}</span>
                                        <span className="order-item-detail order-item-name product-name">{item.product_name || 'N/A'}</span>
                                        <span className="order-item-detail order-item-sifra">{item.sifra_dobavljaca || ''}</span>
                                        <span className="order-item-detail order-item-status">
                                          {item.status_name ? (
                                            <div className="status-cell">
                                              <span
                                                className="status-indicator"
                                                style={getStatusStyle(item.status_color)}
                                              />
                                              <span>{item.status_name}</span>
                                            </div>
                                          ) : ''}
                                        </span>
                                        {!isSupplier && <span className="order-item-detail order-item-vendor">{item.vendor || ''}</span>}
                                        {!isSupplier && <span className="order-item-detail order-item-warehouse">{item.warehouse || ''}</span>}
                                        {!isSupplier && <span className="order-item-detail order-item-commercialist">{item.commercialist || ''}</span>}
                                      </div>
                                    </td>
                                  </tr>
                                )
                              })}
                            </React.Fragment>
                          )
                        })
                      : paginatedOrders.map((row, index) => {
                          const rowId = getRowId(row, startIndex + index)
                          const isSelected = selectedIds.has(rowId) || selectAllMatching
                          return (
                            <tr
                              key={rowId}
                              className={isSelected ? 'selected' : ''}
                            >
                              <td className="checkbox-column-narrow">
                                <input
                                  type="checkbox"
                                  checked={isSelected}
                                  disabled={selectAllMatching}
                                  onChange={() => handleRowCheckbox(rowId)}
                                  aria-label={`Select row ${rowId}`}
                                />
                              </td>
                              <td>{row.order_id || 'N/A'}</td>
                              <td>{row.shipping_name || ''}</td>
                              <td>{row.payment_method || ''}</td>
                              <td>
                                <span style={{
                                  ...getPaymentStatusStyle(row.payment_status),
                                  padding: '4px 8px',
                                  display: 'inline-block',
                                  borderRadius: '4px'
                                }}>
                                  {row.payment_status || 'N/A'}
                                </span>
                              </td>
                              <td>{row.shipping_status || 'N/A'}</td>
                              <td>{formatDate(row.date_created)}</td>
                              <td className="product-name">{row.product_name || 'N/A'}</td>
                              <td>{row.quantity ?? ''}</td>
                              <td>
                                {row.status_name ? (
                                  <div className="status-cell">
                                    <span
                                      className="status-indicator"
                                      style={getStatusStyle(row.status_color)}
                                    />
                                    <span>{row.status_name}</span>
                                  </div>
                                ) : (
                                  ''
                                )}
                              </td>
                              {!isSupplier && <td>{row.vendor || ''}</td>}
                              {!isSupplier && <td>{row.warehouse || ''}</td>}
                              {!isSupplier && <td>{row.sifra_dobavljaca || ''}</td>}
                              {!isSupplier && <td>{row.commercialist || ''}</td>}
                            </tr>
                          )
                        })}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <div className="pagination">
                <div className="pagination-info">
                  Showing {startIndex + 1}-{Math.min(startIndex + pageSize, isGroupedView ? (groupedOrders?.length || 0) : filteredOrders.length)} of {isGroupedView ? (groupedOrders?.length || 0) : filteredOrders.length} {isGroupedView ? 'porudžbina' : 'records'}
                  {(vendorFilter || commercialistFilter) && ' (filtered)'}
                  {filteredOrders.length < allOrders.length && ` from ${allOrders.length} total`}
                </div>
                <div className="pagination-controls">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 1}
                    className="page-btn"
                  >
                    ◀ Prev
                  </button>
                  <span className="page-number">
                    Page {currentPage} of {totalPages || 1}
                  </span>
                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages}
                    className="page-btn"
                  >
                    Next ▶
                  </button>
                </div>
              </div>
            </div>

            {/* Sticky horizontal scrollbar */}
            <div 
              ref={stickyScrollRef}
              className="sticky-scrollbar"
              style={{
                position: 'fixed',
                bottom: 0,
                overflowX: 'auto',
                overflowY: 'hidden',
                height: '20px',
                backgroundColor: '#f8f9fa',
                zIndex: 1000,
                borderTop: '2px solid #dee2e6',
                boxShadow: '0 -2px 4px rgba(0,0,0,0.1)',
                padding: '2px 0'
              }}
            >
              <div 
                className="scroll-content" 
                style={{ 
                  height: '10px',
                  backgroundColor: 'transparent'
                }}
              ></div>
            </div>
            </>
          )}

          {/* Empty State */}
          {!loading && !error && filteredOrders.length === 0 && allOrders.length === 0 && (
            <div className="card empty-state-card">
              <p>Nema pronađenih porudžbina. Kliknite "Preuzmite porudžbine" da učitate podatke.</p>
            </div>
          )}

          {/* Filtered Empty State */}
          {!loading && !error && filteredOrders.length === 0 && allOrders.length > 0 && (
            <div className="card empty-state-card">
              <p>Nijedna porudžbina ne odgovara trenutnim filterima (Dobavljač ili Komercijalista).</p>
            </div>
          )}
        </main>
      </div>

      {/* Status Update Modal */}
      {statusModalOpen && (
        <div className="modal-overlay" onClick={() => setStatusModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Izmenite status proizvoda</h2>
            <p className="modal-summary">{itemsToUpdate.length} stavki će biti ažurirano</p>
            
            <div className="form-group">
              <label htmlFor="product-status">Status proizvoda:</label>
              <select
                id="product-status"
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="form-control"
              >
                <option value="">-- Izaberite status --</option>
                {productStatuses.map((status) => (
                  <option key={status.id} value={status.id}>
                    {status.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="modal-actions">
              <button
                onClick={() => setStatusModalOpen(false)}
                className="btn btn-secondary"
              >
                Otkažite
              </button>
              <button
                onClick={handleUpdateProductStatus}
                className="btn btn-primary"
                disabled={!selectedStatus}
              >
                Ažurirajte
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Tag Modal */}
      {tagModalOpen && (
        <div className="modal-overlay" onClick={() => { setTagModalOpen(false); setTagInput('') }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Dodajte tagove porudžbinama</h2>
            <p className="modal-summary">
              {getSelectedOrderCount()} porudžbina će biti ažurirano
            </p>
            <div className="form-group">
              <label htmlFor="tag-input">Tag:</label>
              <input
                id="tag-input"
                type="text"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                className="form-control"
                placeholder="npr. Preuzeto"
              />
            </div>
            <div className="modal-actions">
              <button
                onClick={() => { setTagModalOpen(false); setTagInput('') }}
                className="btn btn-secondary"
              >
                Otkažite
              </button>
              <button
                onClick={() => handleBulkUpdateTags(tagInput.trim() ? [tagInput.trim()] : [])}
                className="btn btn-primary"
                disabled={!tagInput.trim()}
              >
                Dodajte tag
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Progress Modal */}
      {updating && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Ažuriranje porudžbina...</h2>
            <div className="progress-bar">
              <div
                className="progress-bar-fill"
                style={{ width: `${updateProgress}%` }}
              ></div>
            </div>
            <p>Molimo sačekajte dok se porudžbine ažuriraju.</p>
          </div>
        </div>
      )}

      {/* Summary Modal */}
      {summaryModalOpen && updateResults && (
        <div className="modal-overlay" onClick={() => setSummaryModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Ažuriranje završeno</h2>
            <p className="modal-summary">
              Uspešno ažurirano {updateResults.successful_updates} od {updateResults.total_orders} porudžbina
            </p>

            {updateResults.failed_updates > 0 && (
              <div className="failed-orders">
                <h3>Neuspešna ažuriranja ({updateResults.failed_updates}):</h3>
                <ul>
                  {updateResults.results
                    .filter((r) => !r.success)
                    .map((r) => (
                      <li key={r.order_id}>
                        Porudžbina {r.order_id}: {r.error}
                      </li>
                    ))}
                </ul>
              </div>
            )}

            <div className="modal-actions">
              <button
                onClick={() => setSummaryModalOpen(false)}
                className="btn btn-primary"
              >
                Zatvorite
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Orders
