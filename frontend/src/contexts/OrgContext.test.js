import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { OrgProvider, resolveInitialOrgId, storageKey, useOrg } from './OrgContext'

vi.mock('../services/api', () => ({
  orgsApi: {
    list: vi.fn(),
    myAccess: vi.fn(),
  },
}))

const mockUseAuth = vi.fn()
vi.mock('./AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}))

import { orgsApi } from '../services/api'

function wrapper({ children }) {
  return React.createElement(OrgProvider, null, children)
}

describe('OrgContext helpers', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  describe('storageKey', () => {
    it('returns activeOrg:<userId> when userId is present', () => {
      expect(storageKey({ userId: 42 })).toBe('activeOrg:42')
    })

    it('falls back to id then email', () => {
      expect(storageKey({ id: 7 })).toBe('activeOrg:7')
      expect(storageKey({ email: 'a@b.com' })).toBe('activeOrg:a@b.com')
    })

    it('returns null when no identifier', () => {
      expect(storageKey({})).toBeNull()
      expect(storageKey(null)).toBeNull()
    })
  })

  describe('resolveInitialOrgId', () => {
    const user = { userId: 1 }
    const orgs = [
      { orgId: 10, name: 'A' },
      { orgId: 20, name: 'B' },
    ]

    it('returns stored id when valid', () => {
      localStorage.setItem('activeOrg:1', '20')
      expect(resolveInitialOrgId(orgs, user)).toBe('20')
    })

    it('clears stale stored id', () => {
      localStorage.setItem('activeOrg:1', '999')
      expect(resolveInitialOrgId(orgs, user)).toBe('')
      expect(localStorage.getItem('activeOrg:1')).toBeNull()
    })

    it('auto-selects single org', () => {
      expect(resolveInitialOrgId([{ orgId: 10, name: 'A' }], user)).toBe('10')
    })

    it('returns empty string for multi-org with no stored value', () => {
      expect(resolveInitialOrgId(orgs, user)).toBe('')
    })
  })
})

describe('OrgProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('calls myAccess for normal user', async () => {
    mockUseAuth.mockReturnValue({ user: { userId: 1, roleName: 'USER' } })
    orgsApi.myAccess.mockResolvedValue([{ orgId: 1, name: 'Org' }])

    const { result } = renderHook(() => useOrg(), { wrapper })

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(orgsApi.myAccess).toHaveBeenCalled()
    expect(orgsApi.list).not.toHaveBeenCalled()
    expect(result.current.orgs).toHaveLength(1)
    expect(result.current.activeOrgId).toBe('1')
  })

  it('calls list for SuperAdmin', async () => {
    mockUseAuth.mockReturnValue({ user: { userId: 1, roleName: 'SUPERADMIN' } })
    orgsApi.list.mockResolvedValue([{ orgId: 5, name: 'All Org' }])

    const { result } = renderHook(() => useOrg(), { wrapper })

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(orgsApi.list).toHaveBeenCalled()
    expect(orgsApi.myAccess).not.toHaveBeenCalled()
    expect(result.current.orgs).toHaveLength(1)
    expect(result.current.activeOrgId).toBe('5')
  })

  it('sets error loadFailed and empty orgs on API failure', async () => {
    mockUseAuth.mockReturnValue({ user: { userId: 1, roleName: 'USER' } })
    orgsApi.myAccess.mockRejectedValue(new Error('network'))

    const { result } = renderHook(() => useOrg(), { wrapper })

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.error).toBe('loadFailed')
    expect(result.current.orgs).toEqual([])
    expect(result.current.activeOrgId).toBe('')
  })
})
