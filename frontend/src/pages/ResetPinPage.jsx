import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { apiErrorMessage } from '../api/client'
import MessageDialog from '../components/MessageDialog'

export default function ResetPinPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [cardNumber, setCardNumber] = useState('')
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [newPin, setNewPin] = useState('')
  const [confirmPin, setConfirmPin] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [dialog, setDialog] = useState(null)

  const fetchQuestion = async (e) => {
    e.preventDefault()
    setError('')
    if (!/^\d{16}$/.test(cardNumber)) {
      setError('Please enter a valid 16-digit Card Number')
      return
    }
    setLoading(true)
    try {
      const { data } = await api.get(`/auth/security-question/${cardNumber}`)
      setQuestion(data.securityQuestion)
      setStep(2)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const submitReset = async (e) => {
    e.preventDefault()
    setError('')

    if (!answer.trim()) {
      setError('Answer cannot be empty.')
      return
    }
    if (newPin !== confirmPin) {
      setError('PINs do not match!')
      return
    }
    if (!/^\d+$/.test(newPin)) {
      setError('PIN must contain only digits!')
      return
    }
    if (newPin.length < 4 || newPin.length > 6) {
      setError('PIN must be between 4 and 6 digits!')
      return
    }

    setLoading(true)
    try {
      const { data } = await api.post('/auth/reset-pin', {
        cardNumber,
        securityAnswer: answer.trim(),
        newPin,
        confirmPin,
      })
      setDialog({
        title: 'Success',
        message: data.message || 'PIN has been successfully reset. Please login with your new PIN.',
        type: 'success',
      })
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="reset-page">
      <form className="reset-card" onSubmit={step === 1 ? fetchQuestion : submitReset}>
        <h1>RESET YOUR PIN</h1>

        {error && <div className="error-box">{error}</div>}

        {step === 1 && (
          <>
            <p className="hint reset-intro">
              Enter the 16-digit number printed on the front of your KodBank card.
              We will ask your security question before you can set a new PIN.
            </p>

            <div className="form-group">
              <label>Card Number:</label>
              <input
                type="text"
                value={cardNumber}
                onChange={(e) => setCardNumber(e.target.value.replace(/\D/g, '').slice(0, 16))}
                placeholder="16-digit card number"
                maxLength={16}
                autoFocus
              />
            </div>
            <div className="reset-actions">
              <button className={`atm-btn ${loading ? 'is-loading' : ''}`} type="submit" disabled={loading}>
                {loading ? 'CHECKING…' : 'CONTINUE'}
              </button>
              <button className="atm-btn btn-secondary" type="button" onClick={() => navigate('/login')}>
                BACK
              </button>
            </div>

            <div className="reset-help">
              <p className="login-aside-title">What you’ll need</p>
              <ul className="fill-list">
                <li>Your physical card or the full 16-digit card number</li>
                <li>The answer you chose when you opened the account</li>
                <li>A new 4–6 digit PIN that is not sequential (avoid 1234)</li>
              </ul>
              <p className="fill-note">
                Can’t recall your security answer? Visit a branch with government ID,
                or call 1800-000-KODB for assisted reset. Never share your old PIN with anyone.
              </p>
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <div className="question">Security Question: {question}</div>

            <div className="form-group">
              <label>Answer:</label>
              <input type="text" value={answer} onChange={(e) => setAnswer(e.target.value)} autoFocus />
            </div>

            <div className="form-group">
              <label>New PIN:</label>
              <input type="password" value={newPin}
                     onChange={(e) => setNewPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
                     maxLength={6} />
            </div>

            <div className="form-group">
              <label>Confirm PIN:</label>
              <input type="password" value={confirmPin}
                     onChange={(e) => setConfirmPin(e.target.value.replace(/\D/g, '').slice(0, 6))}
                     maxLength={6} />
            </div>

            <p className="hint">
              PIN must be 4–6 digits, no sequential digits (e.g. 1234)
            </p>

            <div className="reset-actions">
              <button className={`atm-btn ${loading ? 'is-loading' : ''}`} type="submit" disabled={loading}>
                {loading ? 'RESETTING…' : 'RESET PIN'}
              </button>
              <button className="atm-btn btn-secondary" type="button" onClick={() => navigate('/login')}>
                BACK
              </button>
            </div>
          </>
        )}
      </form>

      {dialog && (
        <MessageDialog
          title={dialog.title}
          message={dialog.message}
          type={dialog.type}
          onClose={() => {
            setDialog(null)
            if (dialog.type === 'success') navigate('/login')
          }}
        />
      )}
    </div>
  )
}
