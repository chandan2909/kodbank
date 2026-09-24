import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { apiErrorMessage } from '../api/client'
import MessageDialog from '../components/MessageDialog'
import { useAuth } from '../context/AuthContext'

const FILTERS = [
  { id: 'all', label: 'All' },
  { id: 'active', label: 'Active' },
  { id: 'frozen', label: 'Frozen' },
  { id: 'locked', label: 'Locked' },
]

function money(n) {
  return Number(n || 0).toLocaleString('en-IN')
}

export default function AdminPage() {
  const navigate = useNavigate()
  const { isAdmin } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [dialog, setDialog] = useState(null)
  const [limitDrafts, setLimitDrafts] = useState({})
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState('all')
  const [busyId, setBusyId] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = () => {
    api.get('/admin/accounts')
      .then(({ data }) => {
        setAccounts(data)
        const drafts = {}
        data.forEach((a) => { drafts[a.accountId] = String(a.dailyLimit) })
        setLimitDrafts(drafts)
      })
      .catch((err) => setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' }))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (!isAdmin) {
      navigate('/login', { replace: true })
      return
    }
    load()
  }, [isAdmin, navigate])

  const act = async (id, fn) => {
    setBusyId(id)
    try {
      await fn()
      load()
    } catch (err) {
      setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' })
    } finally {
      setBusyId(null)
    }
  }

  const unlock = (id) => act(id, () => api.post(`/admin/accounts/${id}/unlock`))
  const freeze = (id) => act(id, () => api.post(`/admin/accounts/${id}/freeze`))
  const unfreeze = (id) => act(id, () => api.post(`/admin/accounts/${id}/unfreeze`))
  const setLimit = (id) => act(id, () => api.put(`/admin/accounts/${id}/limit`, {
    dailyLimit: Number(limitDrafts[id] || 0),
  }))

  const stats = useMemo(() => {
    const totalBalance = accounts.reduce((s, a) => s + Number(a.balance || 0), 0)
    return {
      total: accounts.length,
      active: accounts.filter((a) => a.status === 'ACTIVE' && !a.locked).length,
      frozen: accounts.filter((a) => a.status !== 'ACTIVE').length,
      locked: accounts.filter((a) => a.locked).length,
      totalBalance,
    }
  }, [accounts])

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase()
    return accounts.filter((a) => {
      if (filter === 'active' && !(a.status === 'ACTIVE' && !a.locked)) return false
      if (filter === 'frozen' && a.status === 'ACTIVE') return false
      if (filter === 'locked' && !a.locked) return false
      if (!q) return true
      return [a.accountNo, a.holderName, a.cardNumber, a.status]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(q))
    })
  }, [accounts, query, filter])

  if (!isAdmin) return null

  return (
    <>
      <header className="admin-topbar">
        <div>
          <p className="admin-eyebrow">Control centre</p>
          <h1>Account operations</h1>
        </div>
        <div className="admin-topbar-meta">
          <span className="admin-pill">{stats.total} accounts</span>
          <span className="admin-pill accent">
            Rs. {money(stats.totalBalance)} under management
          </span>
        </div>
      </header>

      <div className="content-sides wide-core">
        <aside className="side-rail" aria-label="Operator guide">
          <h3>Operator guide</h3>
          <ul className="fill-list">
            <li><strong>Unlock</strong> — clears failed PIN lock after ID checks.</li>
            <li><strong>Freeze</strong> — blocks cash and transfers until unfrozen.</li>
            <li><strong>Daily limit</strong> — caps 24-hour debit volume.</li>
            <li>Confirm holder name and last four card digits before acting.</li>
          </ul>
        </aside>

        <div className="content-core">
          <section className="admin-kpis" aria-label="Overview">
            <article className="admin-kpi">
              <span className="admin-kpi-label">Total accounts</span>
              <strong className="admin-kpi-value">{stats.total}</strong>
              <span className="admin-kpi-hint">Customer accounts on book</span>
            </article>
            <article className="admin-kpi">
              <span className="admin-kpi-label">Active</span>
              <strong className="admin-kpi-value ok">{stats.active}</strong>
              <span className="admin-kpi-hint">Eligible for banking</span>
            </article>
            <article className="admin-kpi">
              <span className="admin-kpi-label">Frozen</span>
              <strong className="admin-kpi-value warn">{stats.frozen}</strong>
              <span className="admin-kpi-hint">Status not ACTIVE</span>
            </article>
            <article className="admin-kpi">
              <span className="admin-kpi-label">Locked cards</span>
              <strong className="admin-kpi-value bad">{stats.locked}</strong>
              <span className="admin-kpi-hint">Failed PIN attempts</span>
            </article>
          </section>

          <section className="admin-panel">
            <div className="admin-toolbar">
              <div className="admin-filters" role="tablist" aria-label="Filter accounts">
                {FILTERS.map((f) => (
                  <button
                    key={f.id}
                    type="button"
                    role="tab"
                    aria-selected={filter === f.id}
                    className={`admin-chip ${filter === f.id ? 'is-on' : ''}`}
                    onClick={() => setFilter(f.id)}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
              <label className="admin-search">
                <span className="sr-only">Search accounts</span>
                <input
                  type="search"
                  placeholder="Search account, holder, or card…"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                />
              </label>
            </div>

            {loading ? (
              <div className="admin-empty">
                <p>Loading accounts…</p>
                <p className="fill-note">Fetching balances, lock status, and daily limits from core banking.</p>
              </div>
            ) : visible.length === 0 ? (
              <div className="admin-empty">
                <p>No accounts match this view.</p>
                <p className="fill-note">
                  Clear the search box or switch filters back to All.
                  Locked and frozen accounts still appear under their respective chips.
                </p>
              </div>
            ) : (
              <div className="admin-grid">
                {visible.map((a) => {
                  const frozen = a.status !== 'ACTIVE'
                  const busy = busyId === a.accountId
                  return (
                    <article
                      key={a.accountId}
                      className={`admin-acct ${frozen ? 'is-frozen' : ''} ${a.locked ? 'is-locked' : ''}`}
                    >
                      <div className="admin-acct-head">
                        <div>
                          <p className="admin-acct-no">{a.accountNo}</p>
                          <p className="admin-acct-holder">{a.holderName || '—'}</p>
                        </div>
                        <div className="admin-acct-badges">
                          <span className={`admin-badge ${frozen ? 'warn' : 'ok'}`}>
                            {frozen ? a.status : 'ACTIVE'}
                          </span>
                          {a.locked ? (
                            <span className="admin-badge bad">LOCK · {a.failedAttempts}</span>
                          ) : (
                            <span className="admin-badge">CARD OK</span>
                          )}
                        </div>
                      </div>

                      <dl className="admin-acct-stats">
                        <div>
                          <dt>Balance</dt>
                          <dd>Rs. {money(a.balance)}</dd>
                        </div>
                        <div>
                          <dt>Daily limit</dt>
                          <dd>Rs. {money(a.dailyLimit)}</dd>
                        </div>
                        <div>
                          <dt>Card</dt>
                          <dd className="mono">
                            {a.cardNumber ? `•••• ${a.cardNumber.slice(-4)}` : '—'}
                          </dd>
                        </div>
                      </dl>

                      <div className="admin-acct-limit">
                        <label>
                          <span>New daily limit</span>
                          <input
                            type="number"
                            min="1"
                            value={limitDrafts[a.accountId] ?? ''}
                            onChange={(e) => setLimitDrafts((d) => ({ ...d, [a.accountId]: e.target.value }))}
                            disabled={busy}
                          />
                        </label>
                        <button
                          type="button"
                          className="admin-btn"
                          disabled={busy}
                          onClick={() => setLimit(a.accountId)}
                        >
                          {busy ? '…' : 'Apply'}
                        </button>
                      </div>

                      <div className="admin-acct-actions">
                        <button
                          type="button"
                          className="admin-btn ghost"
                          disabled={busy || !a.locked}
                          onClick={() => unlock(a.accountId)}
                        >
                          Unlock
                        </button>
                        {frozen ? (
                          <button
                            type="button"
                            className="admin-btn"
                            disabled={busy}
                            onClick={() => unfreeze(a.accountId)}
                          >
                            Unfreeze
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="admin-btn danger"
                            disabled={busy}
                            onClick={() => freeze(a.accountId)}
                          >
                            Freeze
                          </button>
                        )}
                      </div>
                    </article>
                  )
                })}
              </div>
            )}
          </section>
        </div>

        <aside className="side-rail" aria-label="Compliance notes">
          <h3>Compliance</h3>
          <div className="side-rail-stat">
            <span>Under management</span>
            <strong>Rs. {money(stats.totalBalance)}</strong>
          </div>
          <div className="side-rail-stat">
            <span>Active</span>
            <strong className="ok">{stats.active}</strong>
          </div>
          <div className="side-rail-stat">
            <span>Needs attention</span>
            <strong className="warn">{stats.frozen + stats.locked}</strong>
          </div>
          <ul className="fill-list">
            <li>Freeze, unlock, and limit changes are auditable decisions.</li>
            <li>Check Audit trail for unusual money movement.</li>
            <li>Never share operator credentials.</li>
            <li>Confirm holder details before freezing an account.</li>
          </ul>
          <p className="fill-note">
            Escalations: risk@kodbank.example · Desk 4400 · SLA 15 min for card locks.
          </p>
        </aside>
      </div>

      {dialog && (
        <MessageDialog
          title={dialog.title}
          message={dialog.message}
          type={dialog.type}
          onClose={() => setDialog(null)}
        />
      )}
    </>
  )
}
