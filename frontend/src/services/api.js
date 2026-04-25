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
}

export const clientsOrgsApi = {
  async list() {
    const response = await api.get('/clients-orgs')
    return response.data
  },
}

export const apiConfigApi = {
  async listConnections() {
    const response = await api.get('/api-config/connections')
    return response.data
  },
  async listTemplates() {
    const response = await api.get('/api-config/templates')
    return response.data
  },
}

export const ordersApi = {
  async fetch(filters) {
    const response = await api.post('/merchantpro/orders', filters)
    return response.data
  },
}
