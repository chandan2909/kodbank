import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import api, { apiErrorMessage } from '../api/client'
import MessageDialog from '../components/MessageDialog'

const TABS = [
  { id: 'send', label: 'Send money' },
  { id: 'beneficiaries', label: 'Beneficiaries' },
]

function BeneficiariesPanel({ setDialog }) {
  const [list, setList] = useState([])
  const [accountNo, setAccountNo] = useState('')
  const [nickname, setNickname] = useState('')
  const [loading, setLoading] = useState(false)

  const load = () => {
    api.get('/beneficiaries')
      .then(({ data }) => setList(data))
      .catch((err) => setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' }))
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleAdd = async () => {
    if (!accountNo.trim()) {
      setDialog({ title: 'Error', message: 'Enter account number', type: 'error' })
      return
    }
    setLoading(true)
    try {
      await api.post('/beneficiaries', {
        accountNo: accountNo.trim(),
        nickname: nickname.trim() || null,
      })
      setAccountNo('')
      setNickname('')
      setDialog({ title: 'Success', message: 'Beneficiary added', type: 'success' })
      load()
    } catch (err) {
      setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const handleRemove = async (id) => {
    try {
      await api.delete(`/beneficiaries/${id}`)
      load()
    } catch (err) {
      setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' })
    }
  }

  return (
    <section className="admin-panel">
      <div className="beneficiary-add">
        <div className="form-group">
          <label>Account no.</label>
          <input
            type="text"
            inputMode="numeric"
            value={accountNo}
            onChange={(e) => setAccountNo(e.target.value.replace(/\D/g, '').slice(0, 12))}
            placeholder="10-digit account number"
            maxLength={12}
          />
        </div>
        <div className="form-group">
          <label>Nickname</label>
          <input
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value.slice(0, 100))}
            placeholder="Optional nickname"
            maxLength={100}
          />
        </div>
        <button className={`admin-btn ${loading ? 'is-loading' : ''}`} onClick={handleAdd} disabled={loading}>
          {loading ? 'Adding…' : 'Add beneficiary'}
        </button>
      </div>

      <div className="beneficiary-section">
        <p className="admin-kpi-label">Saved accounts ({list.length})</p>
        {list.length === 0 ? (
          <div className="admin-empty">
            <p>No beneficiaries yet</p>
            <p className="fill-note">
              Save frequently used account numbers above so transfers take one tap next time.
              Nicknames help you avoid sending money to the wrong person.
            </p>
          </div>
        ) : (
          <ul className="beneficiary-list">
            {list.map((b) => (
              <li key={b.id}>
                <div>
                  <strong>{b.nickname || b.name || b.accountNo}</strong>
                  <div className="txn-date">{b.accountNo}{b.name ? ` · ${b.name}` : ''}</div>
                </div>
                <button className="admin-btn danger" onClick={() => handleRemove(b.id)}>
                  Remove
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}

function SendPanel({ setDialog, navigate }) {
  const [accountNo, setAccountNo] = useState('')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [otpState, setOtpState] = useState(null)
  const [otpId, setOtpId] = useState('')
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)
  const [otpLoading, setOtpLoading] = useState(false)

  const requestOtp = async () => {
    if (!accountNo.trim() || !amount.trim()) {
      setDialog({ title: 'Error', message: 'Enter account number and amount first', type: 'error' })
      return
    }
    setOtpLoading(true)
    try {
      const { data } = await api.post('/otp/initiate', { purpose: 'TRANSFER' })
      setOtpId(data.otpId)
      setOtpState(data)
      setOtp(data.devOtp || '')
    } catch (err) {
      setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' })
    } finally {
      setOtpLoading(false)
    }
  }

  const handleTransfer = async () => {
    if (!accountNo.trim()) {
      setDialog({ title: 'Error', message: 'Please enter beneficiary account number', type: 'error' })
      return
    }
    if (!amount.trim() || Number(amount) <= 0) {
      setDialog({ title: 'Error', message: 'Please enter a valid amount', type: 'error' })
      return
    }
    if (!otpId || !otp.trim()) {
      setDialog({ title: 'Error', message: 'Please request and enter OTP', type: 'error' })
      return
    }

    setLoading(true)
    try {
      const { data } = await api.post('/transfers', {
        amount: Number(amount),
        accountNo: accountNo.trim(),
        otpId,
        otp: otp.trim(),
        description: description.trim() || null,
      })
      setDialog({ title: 'Success', message: data.message, type: 'success' })
    } catch (err) {
      setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="admin-panel txn-panel">
      <div className="transfer-form">
        <div className="form-group">
          <label>Beneficiary account no.</label>
          <input
            type="text"
            inputMode="numeric"
            value={accountNo}
            onChange={(e) => setAccountNo(e.target.value.replace(/\D/g, '').slice(0, 12))}
            placeholder="10-digit account number"
            maxLength={12}
          />
        </div>
        <div className="form-group">
          <label>Amount (Rs.)</label>
          <input
            className="amount-input"
            type="text"
            inputMode="decimal"
            value={amount}
            onChange={(e) => setAmount(e.target.value.replace(/[^\d.]/g, ''))}
            placeholder="0.00"
          />
        </div>
        <div className="form-group">
          <label>Description</label>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value.slice(0, 255))}
            placeholder="Optional note"
            maxLength={255}
          />
        </div>

        {!otpState ? (
          <button className={`admin-btn ${otpLoading ? 'is-loading' : ''}`} onClick={requestOtp} disabled={otpLoading}>
            {otpLoading ? 'Requesting OTP…' : 'Request OTP'}
          </button>
        ) : (
          <div className="otp-box">
            <p className="panel-hint">
              {otpState.devOtp
                ? <>Verification code: <strong>{otpState.devOtp}</strong> (expires in {otpState.ttlSeconds}s)</>
                : (otpState.message || 'OTP sent to your registered contact')}
            </p>
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
                autoFocus
              />
            </div>
          </div>
        )}
      </div>

      <div className="panel-actions">
        <button className={`admin-btn ${loading ? 'is-loading' : ''}`} onClick={handleTransfer} disabled={loading || !otpState}>
          {loading ? 'Transferring…' : 'Transfer'}
        </button>
        <button className="admin-btn ghost" onClick={() => navigate('/dashboard')}>
          Back
        </button>
      </div>

      <div className="fill-note transfer-foot">
              Double-check the account number before confirming — successful transfers cannot be reversed.
        OTP expires shortly after you request it; request a new code if it times out.
        Add regular payees under Beneficiaries to reduce typing errors.
      </div>
    </section>
  )
}

export default function TransferPage() {
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const tab = TABS.some((t) => t.id === params.get('tab'))
    ? params.get('tab')
    : 'send'
  const [dialog, setDialog] = useState(null)

  const setTab = (id) => setParams({ tab: id })

  const closeDialog = () => {
    const wasSuccess = dialog?.type === 'success'
    setDialog(null)
    if (wasSuccess) navigate('/dashboard')
  }

  return (
    <>
      <header className="admin-topbar">
        <div>
          <p className="admin-eyebrow">Transfers</p>
          <h1>{tab === 'send' ? 'Fund transfer' : 'Beneficiaries'}</h1>
        </div>
        <div className="admin-topbar-meta">
          {tab === 'send' && (
            <>
              <span className="admin-pill accent">OTP required</span>
              <span className="admin-pill">Instant settlement</span>
            </>
          )}
          {tab === 'beneficiaries' && (
            <>
              <span className="admin-pill">Save once, send faster</span>
              <span className="admin-pill accent">Optional nicknames</span>
            </>
          )}
        </div>
      </header>

      <div className="tab-bar" role="tablist" aria-label="Transfer services">
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
        <aside className="side-rail" aria-label="Transfer safety">
          <h3>Stay safe</h3>
          <ul className="fill-list">
            <li>Verify the account number digit-by-digit before OTP.</li>
            <li>Successful transfers cannot be reversed.</li>
            <li>Only send to people you know and trust.</li>
            <li>Nicknames reduce wrong-recipient mistakes.</li>
          </ul>
          <p className="fill-note">
            KodBank never asks for OTP by phone or chat. If someone does, stop and call 1800-000-KODB.
          </p>
        </aside>

        <div className="content-core">
          {tab === 'send' ? (
            <SendPanel setDialog={setDialog} navigate={navigate} />
          ) : (
            <BeneficiariesPanel setDialog={setDialog} />
          )}
        </div>

        <aside className="side-rail" aria-label="Transfer facts">
          <h3>Transfer facts</h3>
          <div className="side-rail-stat">
            <span>OTP</span>
            <strong>Always required</strong>
          </div>
          <div className="side-rail-stat">
            <span>Settlement</span>
            <strong className="ok">Instant</strong>
          </div>
          <div className="side-rail-stat">
            <span>Description</span>
            <strong>Optional note</strong>
          </div>
          <ol className="fill-list numbered">
            <li>Enter beneficiary account and amount.</li>
            <li>Request OTP and enter the 6-digit code.</li>
            <li>Confirm — balance updates on both sides.</li>
            <li>Find the pair of legs under Statement / Audit.</li>
          </ol>
        </aside>
      </div>

      {dialog && (
        <MessageDialog title={dialog.title} message={dialog.message}
                       type={dialog.type} onClose={closeDialog} />
      )}
    </>
  )
}
