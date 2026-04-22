import React, { useState, useEffect } from 'react'
import { useNavigate, Link, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { merchantProService } from '../services/api'
import { IconDashboard, IconOrders, IconAccount, IconUsers, IconProfile, IconLogOut } from '../components/Icons'

const Dashboard = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [orders, setOrders] = useState([])
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      // Try to load data from MerchantPro
      // This will fail if API credentials are not configured, which is expected
      try {
        const [ordersData, productsData] = await Promise.all([
          merchantProService.getOrders(),
          merchantProService.getProducts(),
        ])
        setOrders(ordersData.orders || ordersData || [])
        setProducts(productsData.products || productsData || [])
      } catch (apiError) {
        // API not configured yet - this is expected for initial setup
        console.log('MerchantPro API not configured yet')
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

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
          <div className="dashboard-header">
            <h2>Workflow Management Dashboard</h2>
            <p>Manage your online shop workflows efficiently</p>
          </div>

          {loading ? (
            <div className="loading">Loading dashboard data...</div>
          ) : error ? (
            <div className="error-message">{error}</div>
          ) : (
            <>
              <div className="card">
                <h3>MerchantPro Integration Status</h3>
                <p>
                  {orders.length > 0 || products.length > 0
                    ? '✓ Connected to MerchantPro API'
                    : '⚠ MerchantPro API not configured. Please configure your API credentials in the backend .env file.'}
                </p>
              </div>

              <div className="card">
                <h3>Recent Orders</h3>
                {orders.length > 0 ? (
                  <p>Showing {orders.length} orders from MerchantPro</p>
                ) : (
                  <p>No orders available. Configure MerchantPro API to see orders.</p>
                )}
              </div>

              <div className="card">
                <h3>Products</h3>
                {products.length > 0 ? (
                  <p>Showing {products.length} products from MerchantPro</p>
                ) : (
                  <p>No products available. Configure MerchantPro API to see products.</p>
                )}
              </div>

              <div className="card">
                <h3>Quick Actions</h3>
                <p>
                  This dashboard helps you manage workflows that are currently too slow
                  in your shop's admin panel. Configure the MerchantPro API connection
                  to start managing orders and products efficiently.
                </p>
              </div>
            </>
          )}
        </main>
      </div>
    </div>
  )
}

export default Dashboard
