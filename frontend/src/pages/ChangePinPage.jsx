import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { apiErrorMessage } from '../api/client'
import MessageDialog from '../components/MessageDialog'

export default function ChangePinPage() {
  const navigate = useNavigate()
  const [newPin, setNewPin] = useState('')
  const [confirmPin, setConfirmPin] = useState('')
  const [dialog, setDialog] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleChange = async () => {
    if (!newPin) {
      setDialog({ title: 'Error', message: 'Enter New PIN', type: 'error' })
      return
    }
    if (!confirmPin) {
      setDialog({ title: 'Error', message: 'Re-Enter New PIN', type: 'error' })
      return
    }
    if (newPin !== confirmPin) {
      setDialog({ title: 'Error', message: 'Entered PINs do not match', type: 'error' })
      return
    }

    setLoading(true)
    try {
      const { data } = await api.post('/account/change-pin', { newPin, confirmPin })
      setDialog({ title: 'Success', message: data.message, type: 'success' })
    } catch (err) {
      setDialog({ title: 'Error', message: apiErrorMessage(err), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const closeDialog = () => {
    const wasSuccess = dialog?.type === 'success'
    setDialog(null)
    if (wasSuccess) navigate('/dashboard')
  }

  return (
    <>
      <header className="admin-topbar">
        <div>
          <p className="admin-eyebrow">Security</p>
          <h1>Change PIN</h1>
        </div>
        <div className="admin-topbar-meta">
          <span className="admin-pill">4–6 digits</span>
          <span className="admin-pill accent">Update regularly</span>
        </div>
      </header>

      <div className="content-sides">
        <aside className="side-rail" aria-label="Why change PIN">
          <h3>Why change it?</h3>
          <ul className="fill-list">
            <li>After you travel or use shared ATMs</li>
            <li>If a shoulder-surfer may have seen your keypad</li>
            <li>On a regular 90-day security rhythm</li>
            <li>Immediately if you suspect exposure</li>
          </ul>
          <p className="fill-note">
            Changes take effect on your next sign-in. Old PIN is retired instantly after success.
          </p>
        </aside>

        <div className="content-core">
          <section className="admin-panel txn-panel">
            <div className="pin-form">
              <div className="form-group">
                <label>New PIN</label>
                <input
                  className="pin-input"
                  type="password"
                  value={newPin}
                  onChange={(e) => setNewPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  maxLength={6}
                  autoFocus
                />
              </div>

              <div className="form-group">
                <label>Re-enter new PIN</label>
                <input
                  className="pin-input"
                  type="password"
                  value={confirmPin}
                  onChange={(e) => setConfirmPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  maxLength={6}
                />
              </div>

              <p className="panel-hint">PIN must be 4–6 digits, no sequential digits (e.g. 1234)</p>
            </div>

            <div className="panel-actions">
              <button className={`admin-btn ${loading ? 'is-loading' : ''}`} onClick={handleChange} disabled={loading}>
                {loading ? 'Changing…' : 'Change PIN'}
              </button>
              <button className="admin-btn ghost" onClick={() => navigate('/dashboard')}>
                Back
              </button>
            </div>
          </section>
        </div>

        <aside className="side-rail" aria-label="PIN safety tips">
          <h3>PIN safety tips</h3>
          <ul className="fill-list">
            <li>Never share your PIN — including with bank staff.</li>
            <li>Avoid birth years, phone digits, 1234, or 0000.</li>
            <li>Cover the keypad at every ATM and kiosk.</li>
            <li>Do not write the PIN on the card or wallet.</li>
            <li>Report suspicious activity from your dashboard Help.</li>
          </ul>
          <p className="fill-note">
            KodBank will never ask for your full PIN by phone, email, or SMS.
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
