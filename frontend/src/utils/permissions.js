export function isSuperAdmin(user) {
  return user?.roleName === 'SUPERADMIN'
}

export function hasAction(user, code) {
  return isSuperAdmin(user) || (user?.actions?.includes(code) ?? false)
}

export function hasAnyAction(user, codes = []) {
  return isSuperAdmin(user) || codes.some((code) => user?.actions?.includes(code))
}

const LANDING_CANDIDATES = [
  { path: '/account', action: 'ACCOUNT_ACCESS' },
  { path: '/orders', action: 'MERCHANTPRO_FETCH_ORDERS' },
  { path: '/fiscal-bills', action: 'FISCAL_VIEW_BILLS' },
  { path: '/fiscal-bills/create', action: 'FISCAL_CREATE_BILL' },
  { path: '/fiscal-bills/products', action: 'FISCAL_MANAGE_PRODUCTS' },
  { path: '/users', action: 'USERS_MANAGE' },
  { path: '/roles', action: 'ROLES_MANAGE' },
  { path: '/organizations', action: 'ORGS_MANAGE' },
]

/** First page the user may open, or null when the role grants no page at all. */
export function getLandingPath(user) {
  return LANDING_CANDIDATES.find(({ action }) => hasAction(user, action))?.path ?? null
}
