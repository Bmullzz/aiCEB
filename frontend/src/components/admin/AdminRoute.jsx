import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'

export default function AdminRoute() {
  const { token } = useAuth()

  if (token === null) {
    return <Navigate to="/admin/login" replace />
  }

  return <Outlet />
}
