import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../state/AuthContext'
import { initials, words } from '../utils/format'

const navigation = [
  { to: '/', end: true, icon: 'bi-grid-1x2-fill', label: 'Operations' },
  { to: '/prisoners', icon: 'bi-people-fill', label: 'Custody registry' },
  { to: '/workflow', icon: 'bi-intersect', label: 'Action workflow' },
  { to: '/alerts', icon: 'bi-send-check-fill', label: 'Alert delivery', roles: ['ADMIN', 'AUDITOR'] },
  { to: '/rules', icon: 'bi-journal-check', label: 'Legal rules' }
]

export default function AppShell({ children }) {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const location = useLocation()
  const date = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: '2-digit', month: 'short' }).format(new Date())
  const visibleNavigation = navigation.filter(item => !item.roles || item.roles.includes(user?.role))

  return (
    <div className="app-frame">
      <aside className={`sidebar ${open ? 'sidebar-open' : ''}`}>
        <div className="brand-lockup">
          <div className="brand-mark" aria-hidden="true"><span /><span /></div>
          <div><strong>Liberty Reckoner</strong><small>Custody rights register</small></div>
          <button className="sidebar-close" onClick={() => setOpen(false)} aria-label="Close navigation"><i className="bi bi-x-lg" /></button>
        </div>

        <div className="jurisdiction-chip"><i className="bi bi-shield-check" /><div><span>Secure jurisdiction</span><strong>Delhi pilot network</strong></div></div>
        <nav className="sidebar-nav" aria-label="Primary navigation">
          <span className="nav-section-label">Command desk</span>
          {visibleNavigation.map(item => (
            <NavLink key={item.to} to={item.to} end={item.end} onClick={() => setOpen(false)}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <i className={`bi ${item.icon}`} /><span>{item.label}</span><i className="bi bi-chevron-right nav-arrow" />
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-foot">
          <div className="system-health"><span className="health-dot" /><div><strong>ICJS bridge online</strong><small>Last reconciled 4 min ago</small></div></div>
          <div className="user-panel">
            <div className="avatar">{initials(user?.fullName)}</div>
            <div className="user-copy"><strong>{user?.fullName}</strong><small>{words(user?.role)}</small></div>
            <button onClick={logout} title="Sign out" aria-label="Sign out"><i className="bi bi-box-arrow-right" /></button>
          </div>
        </div>
      </aside>

      {open && <button className="sidebar-scrim" onClick={() => setOpen(false)} aria-label="Close navigation" />}
      <section className="main-shell">
        <header className="topbar">
          <div className="topbar-left">
            <button className="mobile-menu" onClick={() => setOpen(true)} aria-label="Open navigation"><i className="bi bi-list" /></button>
            <div className="route-context"><span>Liberty Reckoner command</span><strong>{visibleNavigation.find(item => item.end ? location.pathname === item.to : location.pathname.startsWith(item.to))?.label || 'Case workspace'}</strong></div>
          </div>
          <div className="topbar-actions">
            <span className="today-chip"><i className="bi bi-calendar3" />{date}</span>
            {['ADMIN', 'AUDITOR'].includes(user?.role) && <NavLink to="/alerts" className="icon-button" aria-label="Alert delivery"><i className="bi bi-bell" /><span className="notification-dot" /></NavLink>}
          </div>
        </header>
        <main className="app-content">{children}</main>
      </section>
    </div>
  )
}
