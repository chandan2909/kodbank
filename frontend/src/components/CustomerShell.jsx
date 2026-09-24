import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const NAV = [
  { id: 'home', label: 'Home', to: '/dashboard' },
  { id: 'cash', label: 'Cash', to: '/cash?tab=deposit' },
  { id: 'transfer', label: 'Transfer', to: '/transfer?tab=send' },
  { id: 'statement', label: 'Statement', to: '/statement' },
  { id: 'change-pin', label: 'Change PIN', to: '/change-pin' },
]

export default function CustomerShell({ active = 'home', children }) {
  const navigate = useNavigate()
  const { logout, username, cardNumber, isAdmin } = useAuth()

  const nav = isAdmin
    ? [...NAV, { id: 'admin', label: 'Admin console', to: '/admin' }]
    : NAV

  return (
    <div className="admin-shell">
      <aside className="admin-side">
        <div className="admin-brand">
          <span className="admin-brand-mark">KB</span>
          <div>
            <strong>KodBank</strong>
            <span>Customer</span>
          </div>
        </div>

        <nav className="admin-nav">
          {nav.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`admin-nav-item ${active === item.id ? 'is-active' : ''}`}
              onClick={() => navigate(item.to)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="admin-side-note">
          <p className="admin-side-note-title">Secure banking</p>
          <p>
            Deposits, transfers, and statements update in real time.
            Never share your PIN or OTP — support will not ask for them.
          </p>
        </div>

        <div className="admin-side-foot">
          <div className="admin-operator">
            <div>
              <strong>{username || 'Customer'}</strong>
              <span>
                {cardNumber ? `•••• ${cardNumber.slice(-4)}` : 'Signed in'}
              </span>
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

      <main className="admin-main customer-main">
        {children}
      </main>
    </div>
  )
}
