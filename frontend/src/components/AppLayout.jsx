import { Outlet, useLocation } from 'react-router-dom'
import AdminShell from './AdminShell'
import CustomerShell from './CustomerShell'
import { useAuth } from '../context/AuthContext'

function customerActive(pathname) {
  if (pathname === '/cash') return 'cash'
  if (pathname === '/transfer') return 'transfer'
  if (pathname === '/change-pin') return 'change-pin'
  if (pathname === '/statement') return 'statement'
  return 'home'
}

export default function AppLayout() {
  const { isAdmin } = useAuth()
  const { pathname } = useLocation()

  const useAdminShell = pathname === '/admin' || (pathname === '/audit' && isAdmin)

  if (useAdminShell) {
    return (
      <AdminShell active={pathname === '/admin' ? 'accounts' : 'audit'}>
        <Outlet />
      </AdminShell>
    )
  }

  return (
    <CustomerShell active={customerActive(pathname)}>
      <Outlet />
    </CustomerShell>
  )
}
