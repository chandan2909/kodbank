import { Routes, Route, Navigate } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ResetPinPage from './pages/ResetPinPage'
import DashboardPage from './pages/DashboardPage'
import CashPage from './pages/CashPage'
import MiniStatementPage from './pages/MiniStatementPage'
import ChangePinPage from './pages/ChangePinPage'
import TransferPage from './pages/TransferPage'
import AdminPage from './pages/AdminPage'
import AuditTrailPage from './pages/AuditTrailPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/reset-pin" element={<ResetPinPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/cash" element={<CashPage />} />
          <Route path="/transfer" element={<TransferPage />} />
          <Route path="/statement" element={<MiniStatementPage />} />
          <Route path="/change-pin" element={<ChangePinPage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/audit" element={<AuditTrailPage />} />
        </Route>

        <Route path="/deposit" element={<Navigate to="/cash?tab=deposit" replace />} />
        <Route path="/withdraw" element={<Navigate to="/cash?tab=withdraw" replace />} />
        <Route path="/fast-cash" element={<Navigate to="/cash?tab=fast" replace />} />
        <Route path="/beneficiaries" element={<Navigate to="/transfer?tab=beneficiaries" replace />} />
        <Route path="/balance" element={<Navigate to="/dashboard" replace />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
