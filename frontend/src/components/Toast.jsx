import React from 'react'
import { useAuth } from '../contexts/AuthContext'

export default function Toast() {
  const { notification, clearNotification } = useAuth()

  if (!notification) return null

  return (
    <div className="toast-container">
      <div className={`toast ${notification.type}`}>
        <span className="toast-icon">
          {notification.type === 'error' ? '⚠️' : '✓'}
        </span>
        <span className="toast-message">{notification.message}</span>
        <button className="toast-close" onClick={clearNotification}>&times;</button>
      </div>
    </div>
  )
}
