import React from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Login from './pages/Login'
import Account from './pages/Account'
import Users from './pages/Users'
import Clients from './pages/Clients'
import Organizations from './pages/Organizations'
import ApiConfig from './pages/ApiConfig'
import Orders from './pages/Orders'
import FiscalBills from './pages/FiscalBills'
import GetStatus from './pages/GetStatus'
import Taxes from './pages/Taxes'
import CreateFiscalBill from './pages/CreateFiscalBill'
import PayTypeMap from './pages/PayTypeMap'
import Roles from './pages/Roles'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/account" element={<ProtectedRoute><Account /></ProtectedRoute>} />
        <Route path="/users" element={<ProtectedRoute><Users /></ProtectedRoute>} />
        <Route path="/roles" element={<ProtectedRoute><Roles /></ProtectedRoute>} />
        <Route path="/clients" element={<ProtectedRoute><Clients /></ProtectedRoute>} />
        <Route path="/organizations" element={<ProtectedRoute><Organizations /></ProtectedRoute>} />
        <Route path="/api-config" element={<ProtectedRoute><ApiConfig /></ProtectedRoute>} />
        <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
        <Route path="/fiscal-bills" element={<ProtectedRoute><FiscalBills /></ProtectedRoute>} />
        <Route path="/fiscal-bills/create" element={<ProtectedRoute><CreateFiscalBill /></ProtectedRoute>} />
        <Route path="/fiscal-bills/get-status" element={<ProtectedRoute><GetStatus /></ProtectedRoute>} />
        <Route path="/fiscal-bills/paytype-map" element={<ProtectedRoute><PayTypeMap /></ProtectedRoute>} />
        <Route path="/taxes" element={<ProtectedRoute><Taxes /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/orders" replace />} />
      </Routes>
    </AuthProvider>
  )
}
