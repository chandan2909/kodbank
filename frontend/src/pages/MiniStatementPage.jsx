import { useEffect, useState, useCallback } from 'react'
import { useNavigate, Navigate } from 'react-router-dom'
import api, { apiErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'

function StatementContent() {
  const navigate = useNavigate()
  const [statement, setStatement] = useState(null)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [type, setType] = useState('')
  const size = 10

  const load = useCallback((pageNum) => {
    const params = { page: pageNum, size }
    if (from) params.from = from
    if (to) params.to = to
    if (type) params.type = type
    api.get('/account/statement', { params })
      .then(({ data }) => setStatement(data))
      .catch((err) => setError(apiErrorMessage(err)))
  }, [from, to, type])

  useEffect(() => {
    load(page)
  }, [load, page])

  const downloadPdf = async () => {
    try {
      const params = {}
      if (from) params.from = from
      if (to) params.to = to
      if (type) params.type = type
      const response = await api.get('/account/statement.pdf', {
        params,
        responseType: 'blob',
      })
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.download = 'statement.pdf'
      link.click()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  const isCredit = (t) => t.direction === 'C'
    || t.type === 'Deposit'
    || t.type === 'Transfer In'

  return (
    <div className="statement-card admin-statement">
      <h2>Statement</h2>

      {error && <div className="error-box">{error}</div>}

      <div className="statement-toolbar">
        <label>
          From
          <input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0) }} />
        </label>
        <label>
          To
          <input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0) }} />
        </label>
        <label>
          Type
          <select value={type} onChange={(e) => { setType(e.target.value); setPage(0) }}>
            <option value="">All</option>
            <option value="DEPOSIT">Deposit</option>
            <option value="WITHDRAWL">Withdraw</option>
            <option value="TRANSFER">Transfer</option>
          </select>
        </label>
        <button className="copy-btn" onClick={() => { setPage(0); load(0) }}>
          APPLY
        </button>
        <button className="copy-btn" onClick={downloadPdf}>
          PDF
        </button>
      </div>

      {statement && (
        <>
          <p className="statement-meta">Card Number: {statement.maskedCardNumber}</p>
          <p className="statement-balance">
            Balance: Rs. {Number(statement.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </p>

          {!statement.transactions || statement.transactions.length === 0 ? (
            <div className="empty-msg">
              <p>No transactions found</p>
              <p className="fill-note">
                Widen the date range or clear the type filter.
                New deposits, withdrawals, and transfers appear here within seconds of confirmation.
              </p>
            </div>
          ) : (
            <ul className="txn-list">
              {statement.transactions.map((t, i) => (
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
                    {isCredit(t) ? '+' : '−'} Rs.{Number(t.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </strong>
                </li>
              ))}
            </ul>
          )}

          <div className="pager">
            <button
              className="copy-btn"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              PREV
            </button>
            <span>
              Page {(statement.page ?? 0) + 1} of {Math.max(statement.totalPages ?? 1, 1)}
              {' · '}
              {statement.totalElements ?? 0} entries
            </span>
            <button
              className="copy-btn"
              disabled={page + 1 >= (statement.totalPages || 1)}
              onClick={() => setPage((p) => p + 1)}
            >
              NEXT
            </button>
          </div>
        </>
      )}

      {!statement && !error && (
        <p className="empty-msg">
          Loading statement…
          <span className="fill-note" style={{ display: 'block', marginTop: 8 }}>
            Fetching your latest balance and ledger entries from KodBank core banking.
          </span>
        </p>
      )}

      <div className="panel-actions">
        <button
          className="admin-btn ghost"
          onClick={() => navigate('/dashboard')}
        >
          Back to home
        </button>
      </div>
    </div>
  )
}

export default function MiniStatementPage() {
  const { isAdmin } = useAuth()

  if (isAdmin) {
    return <Navigate to="/audit" replace />
  }

  return (
    <>
      <header className="admin-topbar">
        <div>
          <p className="admin-eyebrow">Account</p>
          <h1>Mini statement</h1>
        </div>
        <div className="admin-topbar-meta">
          <span className="admin-pill">Filter by date</span>
          <span className="admin-pill accent">PDF export</span>
        </div>
      </header>

      <div className="content-sides">
        <aside className="side-rail" aria-label="Reading your statement">
          <h3>Reading tips</h3>
          <ul className="fill-list">
            <li>Green amounts are credits (money in).</li>
            <li>Red amounts are debits (money out).</li>
            <li>Use From / To to isolate a billing window.</li>
            <li>Type filter: Deposit, Withdraw, or Transfer.</li>
            <li>PDF keeps the same filters you applied on screen.</li>
          </ul>
          <p className="fill-note">
            Entries come from the double-entry ledger and match your available balance.
          </p>
        </aside>

        <div className="content-core">
          <StatementContent />
        </div>

        <aside className="side-rail" aria-label="Statement help">
          <h3>Statement help</h3>
          <div className="side-rail-stat">
            <span>Page size</span>
            <strong>10 entries</strong>
          </div>
          <div className="side-rail-stat">
            <span>Export</span>
            <strong className="ok">PDF ready</strong>
          </div>
          <div className="side-rail-stat">
            <span>Retention</span>
            <strong>Period</strong>
          </div>
          <ul className="fill-list">
            <li>No rows? Widen the date range or clear Type.</li>
            <li>Missing a transfer? Check the other account too.</li>
            <li>Dispute an entry within 30 days via branch support.</li>
          </ul>
          <p className="fill-note">
            Support: 1800-000-KODB · Never share OTP or PIN with anyone claiming to be staff.
          </p>
        </aside>
      </div>
    </>
  )
}
