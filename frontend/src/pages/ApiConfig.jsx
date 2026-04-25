import React, { useEffect, useState } from 'react'
import AppShell from '../components/AppShell'
import { apiConfigApi } from '../services/api'

export default function ApiConfig() {
  const [connections, setConnections] = useState([])
  const [templates, setTemplates] = useState([])

  useEffect(() => {
    apiConfigApi.listConnections().then(setConnections)
    apiConfigApi.listTemplates().then(setTemplates)
  }, [])

  return (
    <AppShell title="API Configuration" subtitle="Provider connections and reusable metadata templates.">
      <div className="form-grid">
        <section className="table-card">
          <h3>API Connections</h3>
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Provider</th>
                <th>Mode</th>
                <th>Base URL</th>
              </tr>
            </thead>
            <tbody>
              {connections.map((connection) => (
                <tr key={connection.id}>
                  <td>{connection.name}</td>
                  <td>{connection.provider}</td>
                  <td>{connection.mode}</td>
                  <td>{connection.baseUrl}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
        <section className="table-card">
          <h3>API Templates</h3>
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Operation</th>
                <th>Method</th>
                <th>Params</th>
              </tr>
            </thead>
            <tbody>
              {templates.map((template) => (
                <tr key={template.id}>
                  <td>{template.name}</td>
                  <td>{template.operation}</td>
                  <td>{template.method}</td>
                  <td>{template.parameters.join(', ')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    </AppShell>
  )
}
