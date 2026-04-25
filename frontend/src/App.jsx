import React from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Login from './pages/Login'
import Account from './pages/Account'
import Users from './pages/Users'
import ClientsOrgs from './pages/ClientsOrgs'
import ApiConfig from './pages/ApiConfig'
import Orders from './pages/Orders'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/account" element={<ProtectedRoute><Account /></ProtectedRoute>} />
        <Route path="/users" element={<ProtectedRoute><Users /></ProtectedRoute>} />
        <Route path="/clients-orgs" element={<ProtectedRoute><ClientsOrgs /></ProtectedRoute>} />
        <Route path="/api-config" element={<ProtectedRoute><ApiConfig /></ProtectedRoute>} />
        <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/orders" replace />} />
      </Routes>
    </AuthProvider>
  )
}
