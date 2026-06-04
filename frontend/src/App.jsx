import React from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import ActionProtectedRoute from './components/ActionProtectedRoute'
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
import Products from './pages/Products'
import Roles from './pages/Roles'
import Toast from './components/Toast'

function guarded(element, options = {}) {
  return (
    <ProtectedRoute>
      <ActionProtectedRoute {...options}>{element}</ActionProtectedRoute>
    </ProtectedRoute>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/account" element={<ProtectedRoute><Account /></ProtectedRoute>} />
        <Route path="/users" element={guarded(<Users />, { action: 'USERS_MANAGE' })} />
        <Route path="/roles" element={guarded(<Roles />, { actions: ['ROLES_MANAGE', 'USERS_MANAGE'] })} />
        <Route path="/clients" element={guarded(<Clients />, { requireSuperAdmin: true })} />
        <Route path="/organizations" element={guarded(<Organizations />, { action: 'ORGS_MANAGE' })} />
        <Route path="/api-config" element={guarded(<ApiConfig />, { action: 'ORGS_MANAGE' })} />
        <Route path="/orders" element={guarded(<Orders />, { action: 'MERCHANTPRO_FETCH_ORDERS' })} />
        <Route path="/fiscal-bills" element={guarded(<FiscalBills />, { action: 'FISCAL_VIEW_BILLS' })} />
        <Route path="/fiscal-bills/create" element={guarded(<CreateFiscalBill />, { action: 'FISCAL_CREATE_BILL' })} />
        <Route path="/fiscal-bills/get-status" element={guarded(<GetStatus />, { action: 'FISCAL_VIEW_BILLS' })} />
        <Route path="/fiscal-bills/products" element={guarded(<Products />, { action: 'FISCAL_MANAGE_PRODUCTS' })} />
        <Route path="/fiscal-bills/paytype-map" element={guarded(<PayTypeMap />, { action: 'ORGS_MANAGE' })} />
        <Route path="/taxes" element={guarded(<Taxes />, { action: 'ORGS_MANAGE' })} />
        <Route path="*" element={<Navigate to="/orders" replace />} />
      </Routes>
      <Toast />
    </AuthProvider>
  )
}
