export function isSuperAdmin(user) {
  return user?.roleName === 'SUPERADMIN'
}

export function hasAction(user, code) {
  return isSuperAdmin(user) || (user?.actions?.includes(code) ?? false)
}

export function hasAnyAction(user, codes = []) {
  return isSuperAdmin(user) || codes.some((code) => user?.actions?.includes(code))
}
