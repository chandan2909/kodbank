import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { apiErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'

function money(n) {
  return Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const { cardNumber, username, isAdmin } = useAuth()
  const [balance, setBalance] = useState(null)
  const [recent, setRecent] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.allSettled([
      api.get('/account/balance'),
      api.get('/account/statement', { params: { page: 0, size: 5 } }),
    ]).then(([bal, stmt]) => {
      if (bal.status === 'fulfilled') setBalance(bal.value.data.balance)
      if (stmt.status === 'fulfilled') {
        setRecent(stmt.value.data.transactions || [])
      } else {
        setError(apiErrorMessage(stmt.reason))
      }
    }).finally(() => setLoading(false))
  }, [])

  const isCredit = (t) => t.direction === 'C'
    || t.type === 'Deposit'
    || t.type === 'Transfer In'

  return (
    <>
      <header className="admin-topbar">
        <div>
          <p className="admin-eyebrow">Welcome back</p>
          <h1>Account overview</h1>
        </div>
        <div className="admin-topbar-meta">
          {cardNumber && (
            <span className="admin-pill">Card •••• {cardNumber.slice(-4)}</span>
          )}
          {username && <span className="admin-pill">{username}</span>}
          {isAdmin && <span className="admin-pill accent">Admin</span>}
        </div>
      </header>

      <section className="admin-kpis" aria-label="Account summary">
        <article className="admin-kpi">
          <span className="admin-kpi-label">Available balance</span>
          <strong className="admin-kpi-value">
            {loading ? '…' : balance !== null ? `Rs. ${money(balance)}` : '—'}
          </strong>
          <span className="admin-kpi-hint">Current account balance</span>
        </article>
        <article className="admin-kpi">
          <span className="admin-kpi-label">Card</span>
          <strong className="admin-kpi-value" style={{ fontSize: 22 }}>
            {cardNumber ? `•••• ${cardNumber.slice(-4)}` : '—'}
          </strong>
          <span className="admin-kpi-hint">
            {cardNumber ? `${cardNumber.slice(0, 4)}X…${cardNumber.slice(12)}` : 'No card on file'}
          </span>
        </article>
        <article className="admin-kpi">
          <span className="admin-kpi-label">Recent activity</span>
          <strong className="admin-kpi-value">{recent.length}</strong>
          <span className="admin-kpi-hint">Last {recent.length} transactions</span>
        </article>
        <article className="admin-kpi">
          <span className="admin-kpi-label">Session</span>
          <strong className="admin-kpi-value ok">Active</strong>
          <span className="admin-kpi-hint">
            {isAdmin ? 'Administrator access' : 'Customer access'}
          </span>
        </article>
      </section>

      {error && <div className="error-box" style={{ marginBottom: 16 }}>{error}</div>}

      <div className="content-sides">
        <aside className="side-rail" aria-label="Quick actions">
          <h3>Quick actions</h3>
          <ul className="fill-list">
            <li>Cash — deposit, withdraw, or Fast cash presets.</li>
            <li>Transfer — send money with OTP after saving payees.</li>
            <li>Change PIN — update after travel or shared devices.</li>
            <li>Statement — filter dates and download a PDF.</li>
          </ul>
          <p className="fill-note">
            Everything settles against the same double-entry ledger shown under Statement.
          </p>
        </aside>

        <div className="content-core">
          <section className="admin-panel">
            <div className="admin-toolbar" style={{ marginBottom: 0, paddingBottom: 12 }}>
              <div>
                <p className="admin-kpi-label" style={{ marginBottom: 0 }}>Recent transactions</p>
              </div>
              <button className="admin-btn ghost" onClick={() => navigate('/statement')}>
                View full statement
              </button>
            </div>

            {loading ? (
              <p className="admin-empty">Loading activity…</p>
            ) : recent.length === 0 ? (
              <div className="admin-empty">
                <p>No recent transactions</p>
                <p className="fill-note">
                  Deposit or transfer funds to see activity here. Your full history lives under Statement.
                </p>
              </div>
            ) : (
              <ul className="txn-list" style={{ marginTop: 8, marginBottom: 0, maxHeight: 'none' }}>
                {recent.map((t, i) => (
                  <li key={i}>
                    <div>
                      <div className="txn-date">
                        {new Date(t.date).toLocaleString('en-IN', {
                          dateStyle: 'medium',
                          timeStyle: 'short',
                        })}
                      </div>
                      <span className={isCredit(t) ? 'type-deposit' : 'type-withdrawl'}>
                        {t.type}
                      </span>
                    </div>
                    <strong>
                      {isCredit(t) ? '+' : '−'} Rs.{money(t.amount)}
                    </strong>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>

        <aside className="side-rail" aria-label="Security checklist">
          <h3>Security checklist</h3>
          <ul className="fill-list">
            <li>Sign out on shared computers after every session.</li>
            <li>Review recent activity weekly for unfamiliar entries.</li>
            <li>Lock or freeze a lost card from Admin (admin users).</li>
            <li>KodBank never asks for your PIN or OTP by phone.</li>
          </ul>
          <p className="fill-note">
            Support: 1800-000-KODB · Branch Mon–Sat 10:00–18:00 IST.
          </p>
        </aside>
      </div>
    </>
  )
}
