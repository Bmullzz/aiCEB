import { Navigate, Route, Routes } from 'react-router-dom'
import AuthProvider from './contexts/AuthContext'
import AdminRoute from './components/admin/AdminRoute'
import KioskDisplay from './pages/KioskDisplay'
import TvDisplay from './pages/TvDisplay'
import UnsubscribePage from './pages/UnsubscribePage'
import AdminLogin from './pages/admin/AdminLogin'
import AdminEventList from './pages/admin/AdminEventList'
import AdminEventForm from './pages/admin/AdminEventForm'
import AdminCategoryList from './pages/admin/AdminCategoryList'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<KioskDisplay />} />
        <Route path="/tv" element={<TvDisplay />} />
        <Route path="/unsubscribe" element={<UnsubscribePage />} />
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route element={<AdminRoute />}>
          <Route path="/admin" element={<Navigate to="/admin/events" replace />} />
          <Route path="/admin/events" element={<AdminEventList />} />
          <Route path="/admin/events/new" element={<AdminEventForm />} />
          <Route path="/admin/events/:id/edit" element={<AdminEventForm />} />
          <Route path="/admin/categories" element={<AdminCategoryList />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}
