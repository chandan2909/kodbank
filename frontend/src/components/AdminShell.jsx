import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function AdminShell({ active = 'accounts', children }) {
  const navigate = useNavigate()
  const { logout, username } = useAuth()

  const nav = [
    { id: 'accounts', label: 'Accounts', to: '/admin' },
    {
      id: 'audit',
      label: 'Audit trail',
      to: '/audit',
      state: { from: 'admin' },
    },
  ]

  return (
    <div className="admin-shell">
      <aside className="admin-side">
        <div className="admin-brand">
          <span className="admin-brand-mark">KB</span>
          <div>
            <strong>KodBank</strong>
            <span>Operations</span>
          </div>
        </div>

        <nav className="admin-nav">
          {nav.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`admin-nav-item ${active === item.id ? 'is-active' : ''}`}
              onClick={() => navigate(item.to, item.state ? { state: item.state } : undefined)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="admin-side-note">
          <p className="admin-side-note-title">Operations desk</p>
          <p>
            Monitor accounts, unlock cards, freeze activity, and review the full audit trail.
          </p>
        </div>

        <div className="admin-side-foot">
          <div className="admin-operator">
            <div>
              <strong>{username || 'Operator'}</strong>
            </div>
          </div>
          <button
            type="button"
            className="admin-ghost-btn"
            onClick={() => { logout(); navigate('/login') }}
          >
            Sign out
          </button>
        </div>
      </aside>

      <main className="admin-main">
        {children}
      </main>
    </div>
  )
}
