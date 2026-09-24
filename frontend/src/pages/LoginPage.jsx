import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { apiErrorMessage } from '../api/client'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [mode, setMode] = useState('customer')
  const [identifier, setIdentifier] = useState('')
  const [pin, setPin] = useState('')
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(false)

  const isAdminMode = mode === 'admin'

  const clearField = (key) => {
    setErrors((prev) => {
      if (!prev[key]) return prev
      const next = { ...prev }
      delete next[key]
      return next
    })
    if (formError) setFormError('')
  }

  const switchMode = (next) => {
    setMode(next)
    setIdentifier('')
    setPin('')
    setErrors({})
    setFormError('')
  }

  const handleIdentifierChange = (value) => {
    if (isAdminMode) {
      setIdentifier(value.replace(/[^A-Za-z0-9._-]/g, '').slice(0, 50))
    } else {
      setIdentifier(value.replace(/\D/g, '').slice(0, 16))
    }
    clearField('identifier')
  }

  const handlePinChange = (value) => {
    setPin(value)
    clearField('pin')
  }

  const validate = () => {
    const errs = {}
    const id = identifier.trim()
    if (!id) {
      errs.identifier = isAdminMode
        ? 'Please enter your username'
        : 'Please enter your card number'
    } else if (!isAdminMode && !/^\d{16}$/.test(id)) {
      errs.identifier = 'Card number must be exactly 16 digits'
    } else if (isAdminMode && id.length < 3) {
      errs.identifier = 'Username must be at least 3 characters'
    }
    if (!pin.trim()) {
      errs.pin = 'Please enter your PIN'
    } else if (pin.length < 4) {
      errs.pin = 'PIN must be at least 4 characters'
    }
    return errs
  }

  const handleSignIn = async (e) => {
    e.preventDefault()
    setFormError('')
    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setErrors(errs)
      return
    }
    setErrors({})

    setLoading(true)
    try {
      const id = identifier.trim()
      const data = await login(
        isAdminMode ? { username: id, pin } : { cardNumber: id, pin },
      )
      if (data.role === 'ADMIN') navigate('/admin')
      else navigate('/dashboard')
    } catch (err) {
      setFormError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const clearFields = () => {
    setIdentifier('')
    setPin('')
    setErrors({})
    setFormError('')
  }

  return (
    <div className="login-page register-page">
      <div className="register-header">
        <img src="/icons/bank.png" alt="bank" />
        <h1>{isAdminMode ? 'Admin Console' : 'Welcome to KodBank'}</h1>
      </div>

      <div className="login-layout">
        <aside className="login-aside" aria-label="Security notes">
          <p className="login-aside-title">Banking tips</p>
          <ul className="fill-list">
            <li>Bookmark this page — KodBank never emails login links.</li>
            <li>Your PIN is 4–6 digits and is never shared with staff.</li>
            <li>Use Reset PIN if you no longer remember your security answer.</li>
          </ul>
          <p className="login-aside-title">Need help?</p>
          <p className="fill-note">
            Call 1800-000-KODB (toll-free) or visit any branch with photo ID.
            Lost card? Lock it instantly from Account overview after you sign in.
          </p>
        </aside>

        <form className="login-card form-section" onSubmit={handleSignIn}>
        <div className="login-modes" role="tablist" aria-label="Sign-in type">
          <button
            type="button"
            role="tab"
            aria-selected={!isAdminMode}
            className={`login-mode ${!isAdminMode ? 'is-on' : ''}`}
            onClick={() => switchMode('customer')}
          >
            Customer
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={isAdminMode}
            className={`login-mode ${isAdminMode ? 'is-on' : ''}`}
            onClick={() => switchMode('admin')}
          >
            Admin
          </button>
        </div>

        <p className="login-sub">
          {isAdminMode
            ? 'Sign in with your operator username and PIN'
            : 'Sign in with your 16-digit card number and PIN'}
        </p>

        <p className="register-required-note">
          <span className="req-mark">*</span> indicates a required field
        </p>

        {formError && <div className="error-box register-error" role="alert">{formError}</div>}

        <div className={`form-row ${errors.identifier ? 'has-error' : ''}`}>
          <label>{isAdminMode ? 'Username:' : 'Card No:'} <span className="req-mark">*</span></label>
          <div className="field-wrap">
            <input
              type="text"
              value={identifier}
              onChange={(e) => handleIdentifierChange(e.target.value)}
              placeholder={isAdminMode ? 'e.g. admin' : '16-digit card number'}
              maxLength={isAdminMode ? 50 : 16}
              autoComplete={isAdminMode ? 'username' : 'off'}
              autoFocus
            />
            {errors.identifier && (
              <span className="field-error" role="alert">{errors.identifier}</span>
            )}
          </div>
        </div>

        <div className={`form-row ${errors.pin ? 'has-error' : ''}`}>
          <label>PIN: <span className="req-mark">*</span></label>
          <div className="field-wrap">
            <input
              type="password"
              className="pin-field"
              value={pin}
              onChange={(e) => handlePinChange(e.target.value)}
              placeholder="Enter PIN"
              maxLength={6}
              autoComplete={isAdminMode ? 'current-password' : 'off'}
            />
            {errors.pin && (
              <span className="field-error" role="alert">{errors.pin}</span>
            )}
          </div>
        </div>

        <div className="login-buttons">
          <button className={`atm-btn ${loading ? 'is-loading' : ''}`} type="submit" disabled={loading}>
            {loading ? 'SIGNING IN…' : 'SIGN IN'}
          </button>
          <div className="login-buttons-row">
            <button className="atm-btn btn-secondary" type="button" onClick={clearFields}>
              CLEAR
            </button>
            {!isAdminMode && (
              <button className="atm-btn btn-ghost" type="button" onClick={() => navigate('/reset-pin')}>
                RESET PIN
              </button>
            )}
          </div>
          {!isAdminMode && (
            <button className="atm-btn btn-ghost" type="button" onClick={() => navigate('/register')}>
              CREATE ACCOUNT
            </button>
          )}
        </div>
        </form>
      </div>

      <footer className="login-footer">
        <span>© 2026 KodBank Operations</span>
        <span className="login-footer-links">
          <span>Privacy</span>
          <span>Terms</span>
          <span>Report fraud</span>
        </span>
        <span>256-bit session · Never share your PIN or OTP</span>
      </footer>
    </div>
  )
}
