import {
  LayoutDashboard,
  Package,
  User,
  Users,
  LogOut,
  UserCircle,
  Plus,
  Pencil,
  Trash2,
  Loader2,
  Search,
} from 'lucide-react'

const size = 20
const navSize = 22

export const IconDashboard = () => <LayoutDashboard size={size} strokeWidth={2} aria-hidden />
export const IconOrders = () => <Package size={size} strokeWidth={2} aria-hidden />
export const IconAccount = () => <User size={size} strokeWidth={2} aria-hidden />
export const IconUsers = () => <Users size={size} strokeWidth={2} aria-hidden />
export const IconLogOut = () => <LogOut size={size} strokeWidth={2} aria-hidden />
export const IconProfile = () => <UserCircle size={navSize} strokeWidth={2} aria-hidden />
export const IconAdd = () => <Plus size={18} strokeWidth={2.2} aria-hidden />
export const IconEdit = () => <Pencil size={16} strokeWidth={2} aria-hidden />
export const IconDelete = () => <Trash2 size={16} strokeWidth={2} aria-hidden />
export const IconLoader = () => <Loader2 size={18} strokeWidth={2} className="spin" aria-hidden />
export const IconSearch = () => <Search size={18} strokeWidth={2} aria-hidden />
