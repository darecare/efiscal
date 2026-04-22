import axios from 'axios'

const API_URL = import.meta.env.VITE_API_URL || '/api'

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Add token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Auth endpoints
export const authService = {
  login: async (username, password) => {
    const formData = new FormData()
    formData.append('username', username)
    formData.append('password', password)
    const response = await api.post('/auth/login', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
  },
  register: async (email, username, password) => {
    const response = await api.post('/auth/register', {
      email,
      username,
      password,
    })
    return response.data
  },
  getCurrentUser: async () => {
    const response = await api.get('/auth/me')
    return response.data
  },
}

// MerchantPro endpoints
export const merchantProService = {
  getOrders: async (status = null, limit = 50) => {
    const params = { limit }
    if (status) params.status = status
    const response = await api.get('/merchantpro/orders', { params })
    return response.data
  },
  getOrder: async (orderId) => {
    const response = await api.get(`/merchantpro/orders/${orderId}`)
    return response.data
  },
  getProducts: async (limit = 50) => {
    const response = await api.get('/merchantpro/products', {
      params: { limit },
    })
    return response.data
  },
  updateOrderStatus: async (orderId, status) => {
    const response = await api.put(`/merchantpro/orders/${orderId}/status`, {
      status,
    })
    return response.data
  },
}

export const userService = {
  getUsers: async () => {
    const response = await api.get('/users/')
    return response.data
  },
  createUser: async (data) => {
    const response = await api.post('/users/', data)
    return response.data
  },
  updateUser: async (userId, data) => {
    const response = await api.put(`/users/${userId}`, data)
    return response.data
  },
  deleteUser: async (userId) => {
    await api.delete(`/users/${userId}`)
  },
}

export default api
