import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import api, { apiErrorMessage } from '../api/client'
import MessageDialog from '../components/MessageDialog'

const OTP_THRESHOLD = 5000
const PRESETS = [100, 500, 1000, 2000, 5000, 10000]
const TABS = [
  { id: 'deposit', label: 'Deposit' },
  { id: 'withdraw', label: 'Withdraw' },
  { id: 'fast', label: 'Fast cash' },
]

export default function CashPage() {
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const tab = TABS.some((t) => t.id === params.get('tab'))
    ? params.get('tab')
    : 'deposit'

  const [amount, setAmount] = useState('')
  const [dialog, setDialog] = useState(null)
  const [loading, setLoading] = useState(false)
  const [otpState, setOtpState] = useState(null)
  const [otpId, setOtpId] = useState('')
  const [otp, setOtp] = useState('')
  const [otpLoading, setOtpLoading] = useState(false)

  const setTab = (id) => {
    setParams({ tab: id })
    setAmount('')
    setOtpState(null)
    setOtpId('')
    setOtp('')
  }

  const needsOtp = tab === 'withdraw' && Number(amount) >= OTP_THRESHOLD

  const requestOtp = async () => {
    if (!amount.trim()) {
      setDialog({ title: 'Error', message: 'Please enter amount first', type: 'error' })
      return
    }
    setOtpLoading(true)
    try {
      const { data } = await api.post('/otp/initiate', { purpose: 'WITHDRAW' })
      setOtpId(data.otpId)
      setOtpState(data)
      setOtp(data.devOtp || '')
    } catch (err) {
      setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' })
    } finally {
      setOtpLoading(false)
    }
  }

  const run = async (fn) => {
    setLoading(true)
    try {
      const { data } = await fn()
      setDialog({ title: 'Success', message: data.message, type: 'success' })
    } catch (err) {
      setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const handleDeposit = () => {
    if (!amount.trim()) {
      setDialog({ title: 'Error', message: 'Please enter an amount to deposit', type: 'error' })
      return
    }
    return run(() => api.post('/transactions/deposit', { amount: Number(amount) }))
  }

  const handleWithdraw = () => {
    if (!amount.trim()) {
      setDialog({ title: 'Error', message: 'Please enter amount', type: 'error' })
      return
    }
    if (needsOtp && (!otpId || !otp.trim())) {
      setDialog({ title: 'Error', message: 'OTP is required for withdrawals of Rs.5,000 or more', type: 'error' })
      return
    }
    return run(() => api.post('/transactions/withdraw', {
      amount: Number(amount),
      source: 'MANUAL',
      otpId: needsOtp ? otpId : null,
      otp: needsOtp ? otp.trim() : null,
    }))
  }

  const handleFastCash = (value) => run(() => api.post('/transactions/withdraw', {
    amount: value,
    source: 'FAST_CASH',
  }))

  const closeDialog = () => {
    const wasSuccess = dialog?.type === 'success'
    setDialog(null)
    if (wasSuccess) navigate('/dashboard')
  }

  const title = tab === 'deposit' ? 'Deposit'
    : tab === 'withdraw' ? 'Cash withdrawal'
      : 'Fast cash'

  return (
    <>
      <header className="admin-topbar">
        <div>
          <p className="admin-eyebrow">Cash services</p>
          <h1>{title}</h1>
        </div>
        <div className="admin-topbar-meta">
          {tab === 'deposit' && (
            <>
              <span className="admin-pill">Instant credit</span>
              <span className="admin-pill accent">No deposit fee</span>
            </>
          )}
          {tab === 'withdraw' && (
            <>
              <span className="admin-pill">Max Rs. 10,000</span>
              <span className="admin-pill accent">OTP at Rs. 5,000+</span>
            </>
          )}
          {tab === 'fast' && (
            <>
              <span className="admin-pill">1-tap presets</span>
              <span className="admin-pill accent">Up to Rs. 10,000</span>
            </>
          )}
        </div>
      </header>

      <div className="tab-bar" role="tablist" aria-label="Cash services">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={tab === t.id}
            className={`tab-btn ${tab === t.id ? 'is-on' : ''}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="content-sides">
        <aside className="side-rail" aria-label="Limits">
          <h3>Limits</h3>
          <div className="side-rail-stat">
            <span>Single withdraw</span>
            <strong>Rs. 10,000</strong>
          </div>
          <div className="side-rail-stat">
            <span>OTP threshold</span>
            <strong>Rs. 5,000</strong>
          </div>
          <div className="side-rail-stat">
            <span>Deposit fee</span>
            <strong className="ok">Free</strong>
          </div>
          <ul className="fill-list">
            <li>Daily debit cap applies across cash channels.</li>
            <li>Fast cash tiles skip manual amount entry.</li>
            <li>Credits post to your ledger immediately.</li>
          </ul>
        </aside>

        <div className="content-core">
          {tab === 'fast' ? (
            <section className="admin-panel">
              <p className="panel-hint" style={{ textAlign: 'center', marginBottom: 14 }}>
                Select a preset withdrawal amount
              </p>
              <div className="fastcash-grid">
                {PRESETS.map((v) => (
                  <button
                    key={v}
                    type="button"
                    className="fastcash-tile"
                    disabled={loading}
                    onClick={() => handleFastCash(v)}
                  >
                    Rs. {v.toLocaleString('en-IN')}
                  </button>
                ))}
              </div>
              <p className="fill-note" style={{ textAlign: 'center' }}>
                Presets map to common ATM denominations. Amounts of Rs. 5,000 or more still require OTP
                when initiated from the Withdraw tab; fast cash uses a single confirmation.
              </p>
            </section>
          ) : (
            <section className="admin-panel txn-panel">
              <p className="panel-hint">
                {tab === 'deposit'
                  ? 'Enter the amount you want to deposit'
                  : 'Enter withdrawal amount'}
              </p>

              <input
                className="amount-input"
                type="text"
                inputMode="decimal"
                value={amount}
                onChange={(e) => setAmount(e.target.value.replace(/[^\d.]/g, ''))}
                placeholder="0.00"
                autoFocus
              />

              {needsOtp && (
                <div className="otp-box">
                  {!otpState ? (
                    <button className={`admin-btn ${otpLoading ? 'is-loading' : ''}`} onClick={requestOtp} disabled={otpLoading}>
                      {otpLoading ? 'Requesting OTP…' : 'Request OTP'}
                    </button>
                  ) : (
                    <>
                      {otpState.devOtp ? (
                        <p className="panel-hint">Verification code: <strong>{otpState.devOtp}</strong></p>
                      ) : (
                        <p className="panel-hint">{otpState.message || 'OTP sent to your registered contact'}</p>
                      )}
                      <div className="form-group">
                        <label>Enter OTP</label>
                        <input
                          className="pin-input"
                          type="text"
                          inputMode="numeric"
                          value={otp}
                          onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                          maxLength={6}
                          placeholder="6-digit OTP"
                        />
                      </div>
                    </>
                  )}
                </div>
              )}

              <div className="panel-actions">
                <button
                  className={`admin-btn ${loading ? 'is-loading' : ''}`}
                  onClick={tab === 'deposit' ? handleDeposit : handleWithdraw}
                  disabled={loading}
                >
                  {loading
                    ? (tab === 'deposit' ? 'Depositing…' : 'Withdrawing…')
                    : (tab === 'deposit' ? 'Deposit' : 'Withdraw')}
                </button>
                <button className="admin-btn ghost" onClick={() => navigate('/dashboard')}>
                  Back
                </button>
              </div>
            </section>
          )}
        </div>

        <aside className="side-rail" aria-label="How it works">
          <h3>How it works</h3>
          <ol className="fill-list numbered">
            <li>Enter or select the amount you want to move.</li>
            <li>Confirm the transaction on the review dialog.</li>
            <li>If prompted, enter the 6-digit OTP before it expires.</li>
            <li>Balance updates immediately — check Statement for the entry.</li>
          </ol>
          <p className="fill-note">
            Keep amounts under Rs. 5,000 for a faster flow without OTP.
            Never share OTP codes — KodBank staff will never ask for them.
            Support: 1800-000-KODB.
          </p>
        </aside>
      </div>

      {dialog && (
        <MessageDialog title={dialog.title} message={dialog.message}
                       type={dialog.type} onClose={closeDialog} />
      )}
    </>
  )
}
