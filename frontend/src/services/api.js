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
