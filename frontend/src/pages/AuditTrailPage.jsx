import { useCallback, useEffect, useState } from 'react'
import api, { apiErrorMessage } from '../api/client'
import MessageDialog from '../components/MessageDialog'

const TYPES = [
  { value: '', label: 'All' },
  { value: 'DEPOSIT', label: 'Deposit' },
  { value: 'WITHDRAWL', label: 'Withdraw' },
  { value: 'TRANSFER', label: 'Transfer' },
]

function money(n) {
  return Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })
}

function isCredit(item) {
  return item.direction === 'C'
    || item.type === 'Deposit'
    || item.type === 'Transfer In'
}

export default function AuditTrailPage() {
  const [data, setData] = useState(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [type, setType] = useState('')
  const [page, setPage] = useState(0)
  const [dialog, setDialog] = useState(null)
  const [loading, setLoading] = useState(true)
  const size = 10

  const load = useCallback((pageNum) => {
    setLoading(true)
    const params = { page: pageNum, size }
    if (from) params.from = from
    if (to) params.to = to
    if (type) params.type = type
    api.get('/admin/audit', { params })
      .then(({ data: res }) => setData(res))
      .catch((err) => setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' }))
      .finally(() => setLoading(false))
  }, [from, to, type])

  useEffect(() => {
    load(page)
  }, [load, page])

  const items = data?.items || []

  return (
    <>
      <header className="admin-topbar">
        <div>
          <p className="admin-eyebrow">Control centre</p>
          <h1>Audit trail</h1>
        </div>
        <div className="admin-topbar-meta">
          <span className="admin-pill">All customer activity</span>
          <span className="admin-pill accent">
            {data ? data.totalElements : 0} entries
          </span>
        </div>
      </header>

      <div className="content-sides wide-core">
        <aside className="side-rail" aria-label="Audit legend">
          <h3>Legend</h3>
          <div className="side-rail-stat">
            <span>Credit (green)</span>
            <strong className="ok">Money in / +</strong>
          </div>
          <div className="side-rail-stat">
            <span>Debit (red)</span>
            <strong className="warn">Money out / −</strong>
          </div>
          <div className="side-rail-stat">
            <span>Columns</span>
            <strong>Account · Card · Ref</strong>
          </div>
          <ul className="fill-list">
            <li>One row per ledger leg (double-entry).</li>
            <li>Transfers show two rows: Out then In.</li>
            <li>CASH account is the bank vault counterparty.</li>
          </ul>
        </aside>

        <div className="content-core">
          <section className="admin-panel">
            <div className="admin-toolbar">
              <div className="statement-toolbar admin-audit-toolbar">
                <label>
                  From
                  <input
                    type="date"
                    value={from}
                    onChange={(e) => { setFrom(e.target.value); setPage(0) }}
                  />
                </label>
                <label>
                  To
                  <input
                    type="date"
                    value={to}
                    onChange={(e) => { setTo(e.target.value); setPage(0) }}
                  />
                </label>
                <label>
                  Type
                  <select value={type} onChange={(e) => { setType(e.target.value); setPage(0) }}>
                    {TYPES.map((t) => (
                      <option key={t.value} value={t.value}>{t.label}</option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  className="copy-btn"
                  onClick={() => { setPage(0); load(0) }}
                >
                  APPLY
                </button>
              </div>
            </div>

            <p className="fill-note audit-help">
              System-wide ledger of every deposit, withdrawal, and transfer across customer accounts.
              Use date and type filters to investigate a window; amounts show the posting direction for each leg of the double-entry.
            </p>

            {loading ? (
              <div className="admin-empty">
                <p>Loading audit trail…</p>
                <p className="fill-note">Pulling the latest double-entry ledger postings.</p>
              </div>
            ) : items.length === 0 ? (
              <div className="admin-empty">
                <p>No transactions found</p>
                <p className="fill-note">
                  Try a wider date range or switch Type back to All.
                  Entries appear as soon as customers complete cash or transfer activity.
                </p>
              </div>
            ) : (
              <div className="audit-table-wrap">
                <table className="audit-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Account</th>
                      <th>Holder</th>
                      <th>Card</th>
                      <th>Type</th>
                      <th>Amount</th>
                      <th>Balance after</th>
                      <th>Description</th>
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((row, i) => (
                      <tr key={row.id ?? i} style={{ animationDelay: `${Math.min(i * 40, 400)}ms` }}>
                        <td className="mono muted">
                          {new Date(row.date).toLocaleString('en-IN', {
                            dateStyle: 'medium',
                            timeStyle: 'short',
                          })}
                        </td>
                        <td className="mono">{row.accountNo}</td>
                        <td>{row.holderName}</td>
                        <td className="mono">{row.maskedCard}</td>
                        <td>
                          <span className={isCredit(row) ? 'type-deposit' : 'type-withdrawl'}>
                            {row.type}
                          </span>
                        </td>
                        <td className={isCredit(row) ? 'type-deposit' : 'type-withdrawl'}>
                          {isCredit(row) ? '+' : '−'} Rs.{money(row.amount)}
                        </td>
                        <td className="mono">Rs.{money(row.balanceAfter)}</td>
                        <td className="muted">{row.description || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="pager">
              <button
                type="button"
                className="copy-btn"
                disabled={!data || page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                PREV
              </button>
              <span>
                Page {(data?.page ?? 0) + 1} of {Math.max(data?.totalPages ?? 1, 1)}
                {' · '}
                {data?.totalElements ?? 0} entries
              </span>
              <button
                type="button"
                className="copy-btn"
                disabled={!data || page + 1 >= (data.totalPages || 1)}
                onClick={() => setPage((p) => p + 1)}
              >
                NEXT
              </button>
            </div>
          </section>
        </div>

        <aside className="side-rail" aria-label="Audit filters help">
          <h3>Investigation tips</h3>
          <ul className="fill-list">
            <li>Narrow From/To around a reported incident.</li>
            <li>Filter Type to isolate cash vs transfers.</li>
            <li>Balance after shows the post-transaction figure.</li>
            <li>Holder and masked card identify the customer.</li>
            <li>Description carries ATM / transfer narratives.</li>
          </ul>
          <p className="fill-note">
            Operator actions (freeze, unlock, limit) are applied through the admin console;
            this trail covers money movement across the book.
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
