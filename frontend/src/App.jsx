import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './state/AuthContext'
import AppShell from './components/AppShell'
import LoadingState from './components/LoadingState'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import PrisonersPage from './pages/PrisonersPage'
import PrisonerDetailPage from './pages/PrisonerDetailPage'
import WorkflowPage from './pages/WorkflowPage'
import RulesPage from './pages/RulesPage'
import NotificationsPage from './pages/NotificationsPage'

function ProtectedApp() {
  const { user, loading } = useAuth()
  if (loading) return <LoadingState fullscreen label="Securing your operations desk" />
  if (!user) return <Navigate to="/login" replace />

  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/prisoners" element={<PrisonersPage />} />
        <Route path="/prisoners/:id" element={<PrisonerDetailPage />} />
        <Route path="/workflow" element={<WorkflowPage />} />
        <Route path="/alerts" element={['ADMIN', 'AUDITOR'].includes(user.role) ? <NotificationsPage /> : <Navigate to="/" replace />} />
        <Route path="/rules" element={<RulesPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  )
}

export default function App() {
  const { user, loading } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={loading ? <LoadingState fullscreen /> : user ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route path="/*" element={<ProtectedApp />} />
    </Routes>
  )
}
