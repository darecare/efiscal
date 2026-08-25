import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export const authApi = {
  async login(email, password) {
    const response = await api.post('/auth/login', { email, password })
    return response.data
  },
  async me() {
    const response = await api.get('/auth/me')
    return response.data
  },
}

export const usersApi = {
  async list() {
    const response = await api.get('/users')
    return response.data
  },
  async get(userId) {
    const response = await api.get(`/users/${userId}`)
    return response.data
  },
  async create(payload) {
    const response = await api.post('/users', payload)
    return response.data
  },
  async update(userId, payload) {
    const response = await api.put(`/users/${userId}`, payload)
    return response.data
  },
  async remove(userId) {
    await api.delete(`/users/${userId}`)
  },
  async updateMyLanguage(preferredLanguage) {
    const response = await api.patch('/users/me/language', { preferredLanguage })
    return response.data
  },
}

export const clientsOrgsApi = {
  async list() {
    const response = await api.get('/clients-orgs')
    return response.data
  },
}

export const clientsApi = {
  async list() {
    const response = await api.get('/clients')
    return response.data
  },
  async get(clientId) {
    const response = await api.get(`/clients/${clientId}`)
    return response.data
  },
  async create(payload) {
    const response = await api.post('/clients', payload)
    return response.data
  },
  async update(clientId, payload) {
    const response = await api.put(`/clients/${clientId}`, payload)
    return response.data
  },
}

export const orgsApi = {
  async list(clientId) {
    const params = clientId ? { clientId } : {}
    const response = await api.get('/orgs', { params })
    return response.data
  },
  async myAccess() {
    const response = await api.get('/orgs/my-access')
    return response.data
  },
  async create(payload) {
    const response = await api.post('/orgs', payload)
    return response.data
  },
  async update(orgId, payload) {
    const response = await api.put(`/orgs/${orgId}`, payload)
    return response.data
  },
  async getPaymentTypes(orgId) {
    const response = await api.get(`/orgs/${orgId}/payment-types`)
    return response.data
  },
  async setPaymentTypes(orgId, paymentTypes) {
    const response = await api.post(`/orgs/${orgId}/payment-types`, paymentTypes)
    return response.data
  },
}

export const appInfoApi = {
  async get() {
    const response = await api.get('/app-info')
    return response.data
  },
}

export const emailTemplatesApi = {
  async list(orgId) {
    const response = await api.get('/email-templates', { params: { orgId } })
    return response.data
  },
  async create(payload) {
    const response = await api.post('/email-templates', payload)
    return response.data
  },
  async update(templateId, payload) {
    const response = await api.put(`/email-templates/${templateId}`, payload)
    return response.data
  },
  async remove(templateId) {
    await api.delete(`/email-templates/${templateId}`)
  },
}

export const rolesApi = {
  async list(includeInactive = false) {
    const response = await api.get('/roles', { params: { includeInactive } })
    return response.data
  },
  async create(payload) {
    const response = await api.post('/roles', payload)
    return response.data
  },
  async update(roleId, payload) {
    const response = await api.put(`/roles/${roleId}`, payload)
    return response.data
  },
  async replaceActions(roleId, actionIds) {
    const response = await api.put(`/roles/${roleId}/actions`, { actionIds })
    return response.data
  },
  async remove(roleId, reassignToRoleId) {
    await api.delete(`/roles/${roleId}`, { params: { reassignToRoleId } })
  },
}

export const actionsApi = {
  async list(module) {
    const params = module ? { module } : {}
    const response = await api.get('/actions', { params })
    return response.data
  },
}

export const apiConnApi = {
  async list(orgId) {
    const params = orgId ? { orgId } : {}
    const response = await api.get('/apiconn', { params })
    return response.data
  },
  async create(payload) {
    const response = await api.post('/apiconn', payload)
    return response.data
  },
  async update(id, payload) {
    const response = await api.put(`/apiconn/${id}`, payload)
    return response.data
  },
}

export const apiTemplateApi = {
  async list(apiconnId) {
    const response = await api.get('/apitemplate', { params: { apiconnId } })
    return response.data
  },
  async create(payload) {
    const response = await api.post('/apitemplate', payload)
    return response.data
  },
  async update(id, payload) {
    const response = await api.put(`/apitemplate/${id}`, payload)
    return response.data
  },
}

export const ordersApi = {
  async fetch({ orgId, createdAfter, shippingStatus, start = 0, limit = 100 }) {
    const params = { orgId, start, limit }
    if (createdAfter) params.createdAfter = createdAfter
    if (shippingStatus) params.shippingStatus = shippingStatus
    const response = await api.get('/merchantpro/orders', { params })
    return response.data
  },
}

export const fiscalBillApi = {
  async list(orgId) {
    const response = await api.get('/fiscalbill', { params: { orgId } })
    return response.data
  },
  async details(fiscalbillId) {
    const response = await api.get(`/fiscalbill/${fiscalbillId}/details`)
    return response.data
  },
  async downloadPdf(fiscalbillId, format = 'a4') {
    const response = await api.get(`/fiscalbill/${fiscalbillId}/pdf`, {
      params: { format },
      responseType: 'blob',
    })
    return response.data
  },
  async previewHtml(fiscalbillId, format = 'a4') {
    const response = await api.get(`/fiscalbill/${fiscalbillId}/html`, {
      params: { format },
    })
    return response.data
  },
  async createFromOrder(payload, idempotencyKey, orgId, clientId) {
    const response = await api.post('/fiscalbill/from-order', payload, {
      headers: { 'Idempotency-Key': idempotencyKey },
      params: { orgId, clientId },
    })
    return response.data
  },
  async createManual(payload, idempotencyKey, orgId, clientId) {
    const response = await api.post('/fiscalbill/manual', payload, {
      headers: { 'Idempotency-Key': idempotencyKey },
      params: { orgId, clientId },
    })
    return response.data
  },
  async get(fiscalbillId) {
    const response = await api.get(`/fiscalbill/${fiscalbillId}`)
    return response.data
  },
  async retry(fiscalbillId, idempotencyKey) {
    const response = await api.post(`/fiscalbill/${fiscalbillId}/retry`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    return response.data
  },
  async createCopy(fiscalbillId, idempotencyKey) {
    const response = await api.post(`/fiscalbill/${fiscalbillId}/copy`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    return response.data
  },
  async createRefund(fiscalbillId, idempotencyKey) {
    const response = await api.post(`/fiscalbill/${fiscalbillId}/refund`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    return response.data
  },
  async getStatus(orgId) {
    const response = await api.get('/fiscalbill/status', { params: { orgId } })
    return response.data
  },
}

export const paytypeMapApi = {
  async list(clientId) {
    const response = await api.get('/paytype-map', { params: { clientId } })
    return response.data
  },
  async create(payload) {
    const response = await api.post('/paytype-map', payload)
    return response.data
  },
  async update(id, payload) {
    const response = await api.put(`/paytype-map/${id}`, payload)
    return response.data
  },
  async remove(id) {
    await api.delete(`/paytype-map/${id}`)
  },
}

export const taxCategoryApi = {
  async list() {
    const response = await api.get('/tax-categories')
    return response.data
  },
  async create(payload) {
    const response = await api.post('/tax-categories', payload)
    return response.data
  },
  async update(id, payload) {
    const response = await api.put(`/tax-categories/${id}`, payload)
    return response.data
  },
}

function decodeApiError(body, fallback) {
  if (!body) return fallback
  if (typeof body.message === 'string' && body.message.trim()) return body.message
  const err = body.error
  if (err && typeof err === 'object' && err.message) return err.message
  if (typeof err === 'string' && err !== 'Forbidden' && err !== 'Not Found') return err
  return fallback
}

const PRODUCTS_BULK_CHUNK_SIZE = 500

async function productsBulkChunked(method, url, orgId, productIds, extraBody = {}) {
  let total = 0
  for (let i = 0; i < productIds.length; i += PRODUCTS_BULK_CHUNK_SIZE) {
    const chunk = productIds.slice(i, i + PRODUCTS_BULK_CHUNK_SIZE)
    const response = await api.request({
      method,
      url,
      params: { orgId },
      data: { productIds: chunk, ...extraBody },
    })
    const countKey = method === 'delete' ? 'deleted' : 'updated'
    total += response.data?.[countKey] ?? 0
  }
  const countKey = method === 'delete' ? 'deleted' : 'updated'
  return { [countKey]: total }
}

export const productsApi = {
  async list(orgId, { page = 0, size = 100, q } = {}) {
    const params = { orgId, page, size }
    if (q) params.q = q
    const response = await api.get('/products', { params })
    return response.data
  },
  async listIds(orgId, q) {
    const params = { orgId }
    if (q) params.q = q
    const response = await api.get('/products/ids', { params })
    return response.data
  },
  async create(orgId, payload) {
    const response = await api.post('/products', payload, { params: { orgId } })
    return response.data
  },
  async update(productId, payload) {
    const response = await api.put(`/products/${productId}`, payload)
    return response.data
  },
  async remove(productId) {
    await api.delete(`/products/${productId}`)
  },
  async removeMany(orgId, productIds, { selectAll = false, q } = {}) {
    if (selectAll) {
      const response = await api.delete('/products/bulk', {
        params: { orgId },
        data: { selectAll: true, q: q || undefined },
      })
      return response.data
    }
    return productsBulkChunked('delete', '/products/bulk', orgId, productIds)
  },
  async updateStatusMany(orgId, productIds, isActive, { selectAll = false, q } = {}) {
    if (selectAll) {
      const response = await api.patch('/products/bulk/status', {
        selectAll: true,
        isActive,
        q: q || undefined,
      }, { params: { orgId } })
      return response.data
    }
    return productsBulkChunked('patch', '/products/bulk/status', orgId, productIds, { isActive })
  },
  async search(orgId, { q, name, sku, ean } = {}) {
    const response = await api.get('/products/search', {
      params: { orgId, q, name, sku, ean },
    })
    return response.data
  },
  async syncStatus(orgId) {
    const response = await api.get('/products/sync/status', { params: { orgId } })
    return response.data
  },
  async cancelSync(orgId) {
    await api.post('/products/sync/cancel', null, { params: { orgId } })
  },
  syncStream(orgId, { mode = 'AUTO', onProgress, onDone, onError, onConflict, failedMessage } = {}) {
    const token = localStorage.getItem('token')
    const controller = new AbortController()
    const modeParam = mode ? `&mode=${encodeURIComponent(mode)}` : ''
    const url = `/api/v1/products/sync?orgId=${encodeURIComponent(orgId)}${modeParam}`
    const fallbackMessage = failedMessage || ''

    fetch(url, {
      headers: {
        Accept: 'text/event-stream, application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      signal: controller.signal,
    }).then(async (response) => {
      if (!response.ok) {
        let message = fallbackMessage
        try {
          const body = await response.json()
          if (response.status === 409) {
            onConflict?.(body)
            return
          }
          message = decodeApiError(body, fallbackMessage)
        } catch {
          message = response.statusText || fallbackMessage
        }
        onError?.(new Error(message))
        return
      }
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let completed = false
      const handleEvent = (data) => {
        if (data.done) {
          completed = true
          if (data.error) {
            onError?.(new Error(data.error))
            return
          }
          onDone?.(data)
        } else {
          onProgress?.(data)
        }
      }
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          const payload = line.slice(5).trim()
          if (!payload) continue
          try {
            handleEvent(JSON.parse(payload))
          } catch {
            /* ignore malformed chunks */
          }
        }
      }
      if (buffer.startsWith('data:')) {
        const payload = buffer.slice(5).trim()
        if (payload) {
          try {
            handleEvent(JSON.parse(payload))
          } catch {
            /* ignore */
          }
        }
      }
      if (!completed) {
        onError?.(new Error(fallbackMessage))
      }
    }).catch((err) => {
      if (err.name !== 'AbortError') onError?.(err)
    })

    return { abort: () => controller.abort() }
  },
  async lookup(orgId, { sku, ean } = {}) {
    const response = await api.get('/products/lookup', { params: { orgId, sku, ean } })
    return response.data
  },
}

export const taxApi = {
  async list() {
    const response = await api.get('/taxes')
    return response.data
  },
  async create(payload) {
    const response = await api.post('/taxes', payload)
    return response.data
  },
  async update(id, payload) {
    const response = await api.put(`/taxes/${id}`, payload)
    return response.data
  },
}
