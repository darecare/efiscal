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
  async create(payload) {
    const response = await api.post('/orgs', payload)
    return response.data
  },
  async update(orgId, payload) {
    const response = await api.put(`/orgs/${orgId}`, payload)
    return response.data
  },
}

export const rolesApi = {
  async list() {
    const response = await api.get('/roles')
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
  async fetch(filters) {
    const response = await api.post('/merchantpro/orders', filters)
    return response.data
  },
}

export const fiscalBillApi = {
  async create(payload, idempotencyKey) {
    const response = await api.post('/fiscalbill', payload, {
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
    })
    return response.data
  },
  async status(fiscalDocumentId) {
    const response = await api.get(`/fiscalbill/${fiscalDocumentId}`)
    return response.data
  },
  async retry(fiscalDocumentId, idempotencyKey) {
    const response = await api.post(`/fiscalbill/${fiscalDocumentId}/retry`, null, {
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
    })
    return response.data
  },
}
