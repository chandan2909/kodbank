import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { apiErrorMessage, apiErrorDetails } from '../api/client'
import MessageDialog from '../components/MessageDialog'

const SECURITY_QUESTIONS = [
  "What was your first pet's name?",
  "What is your mother's maiden name?",
  "What is the name of your first school?",
  "What is your favorite book?",
  "What city were you born in?",
]

const STATES = [
  'Andhra Pradesh','Arunachal Pradesh','Assam','Bihar','Chhattisgarh','Goa','Gujarat',
  'Haryana','Himachal Pradesh','Jharkhand','Karnataka','Kerala','Madhya Pradesh',
  'Maharashtra','Manipur','Meghalaya','Mizoram','Nagaland','Odisha','Punjab','Rajasthan',
  'Sikkim','Tamil Nadu','Telangana','Tripura','Uttar Pradesh','Uttarakhand','West Bengal',
  'Delhi','Jammu and Kashmir','Ladakh','Puducherry',
]

const FACILITIES = [
  'Debit Card','Internet Banking','Mobile Banking','Email Alerts','Cheque Book','E-Statement',
]

const emptyForm = {
  name: '', fatherName: '', dob: '', gender: '', email: '', marital: '',
  address: '', city: '', state: '', pincode: '', religion: '', category: '',
  income: '', education: '', occupation: '', pan: '', aadhar: '',
  seniorCitizen: '', existingAccount: '', accountType: 'Saving',
  facilities: [], securityQuestion: SECURITY_QUESTIONS[0], securityAnswer: '',
}

const TABS = [
  { id: 'personal', label: 'Personal' },
  { id: 'additional', label: 'Additional' },
  { id: 'account', label: 'Account' },
  { id: 'security', label: 'Security' },
]

const ERROR_TAB = [
  ['personal', /^(name|fatherName|dob|gender|email|marital|address|city|state|pincode)$/],
  ['additional', /^(religion|category|income|education|occupation|pan|aadhar|seniorCitizen|existingAccount)$/],
  ['security', /^(securityAnswer)$/],
]

function tabForErrorKeys(keys) {
  for (const key of keys) {
    const hit = ERROR_TAB.find(([, re]) => re.test(key))
    if (hit) return hit[0]
  }
  return null
}

function FieldError({ message }) {
  if (!message) return null
  return <span className="field-error" role="alert">{message}</span>
}

function Req() {
  return <span className="req-mark" aria-hidden="true">*</span>
}

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyForm)
  const [tab, setTab] = useState('personal')
  const [fieldErrors, setFieldErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [dialog, setDialog] = useState(null)

  const clearField = (key) => {
    setFieldErrors((prev) => {
      if (!prev[key]) return prev
      const next = { ...prev }
      delete next[key]
      return next
    })
    if (formError) setFormError('')
  }

  const set = (key) => (e) => {
    setForm({ ...form, [key]: e.target.value })
    clearField(key)
  }
  const setRadio = (key, value) => () => {
    setForm({ ...form, [key]: value })
    clearField(key)
  }

  const toggleFacility = (name) => () => {
    setForm((f) => ({
      ...f,
      facilities: f.facilities.includes(name)
        ? f.facilities.filter((x) => x !== name)
        : [...f.facilities, name],
    }))
  }

  const validatePersonal = () => {
    const errs = {}
    if (!form.name.trim()) errs.name = 'Please enter your full name'
    if (!form.fatherName.trim()) errs.fatherName = "Please enter your father's name"
    if (!form.dob) errs.dob = 'Please select your date of birth'
    else {
      const dob = new Date(form.dob)
      const min = new Date()
      min.setFullYear(min.getFullYear() - 18)
      if (dob > min) errs.dob = 'You must be at least 18 years old to open an account'
    }
    if (!form.gender) errs.gender = 'Please select your gender'
    if (!form.email.trim()) errs.email = 'Please enter your email address'
    else if (!/^[A-Za-z0-9+_.-]+@(.+)$/.test(form.email)) errs.email = 'That email doesn’t look right — please check it'
    if (!form.marital) errs.marital = 'Please select your marital status'
    if (!form.address.trim()) errs.address = 'Please enter your address'
    if (!form.city.trim()) errs.city = 'Please enter your city'
    if (!form.state) errs.state = 'Please select your state'
    if (form.pincode.length !== 6) errs.pincode = 'PIN code should be 6 digits'
    return errs
  }

  const validateAdditional = () => {
    const errs = {}
    if (!form.religion) errs.religion = 'Please select your religion'
    if (!form.category) errs.category = 'Please select your category'
    if (!form.income) errs.income = 'Please select your income range'
    if (!form.education) errs.education = 'Please select your education'
    if (!form.occupation) errs.occupation = 'Please select your occupation'
    if (!form.pan.trim()) errs.pan = 'Please enter your PAN number'
    if (!form.aadhar.trim()) errs.aadhar = 'Please enter your Aadhaar number'
    else if (!/^\d{12}$/.test(form.aadhar)) errs.aadhar = 'Aadhaar number should be 12 digits'
    if (!form.seniorCitizen) errs.seniorCitizen = 'Please tell us if you are a senior citizen'
    if (!form.existingAccount) errs.existingAccount = 'Please tell us if you already have an account'
    return errs
  }

  const validateAccount = () => ({})

  const validateSecurity = () => {
    const errs = {}
    if (!form.securityAnswer.trim()) errs.securityAnswer = 'Please enter an answer for your security question'
    return errs
  }

  const STEP_VALIDATORS = {
    personal: validatePersonal,
    additional: validateAdditional,
    account: validateAccount,
    security: validateSecurity,
  }

  const validate = () => ({
    ...validatePersonal(),
    ...validateAdditional(),
    ...validateAccount(),
    ...validateSecurity(),
  })

  const stepIndex = TABS.findIndex((t) => t.id === tab)
  const isLastStep = stepIndex === TABS.length - 1
  const allRequiredFilled = Object.keys(validate()).length === 0

  const isTabUnlocked = (index) => {
    for (let i = 0; i < index; i += 1) {
      if (Object.keys(STEP_VALIDATORS[TABS[i].id]()).length > 0) return false
    }
    return true
  }

  const applyErrors = (errs) => {
    if (typeof errs === 'string') {
      setFieldErrors({})
      setFormError(errs)
      return false
    }
    setFieldErrors(errs)
    setFormError('')
    return Object.keys(errs).length > 0
  }

  const handleNext = () => {
    const errs = STEP_VALIDATORS[tab]()
    if (applyErrors(errs)) {
      window.scrollTo({ top: 0, behavior: 'smooth' })
      return
    }
    setTab(TABS[stepIndex + 1].id)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (applyErrors(errs)) {
      const firstTab = tabForErrorKeys(Object.keys(errs))
      if (firstTab) setTab(firstTab)
      window.scrollTo({ top: 0, behavior: 'smooth' })
      return
    }

    setLoading(true)
    try {
      const payload = {
        ...form,
        facilities: form.facilities.join(','),
      }
      const { data } = await api.post('/auth/register', payload)
      setResult(data)
    } catch (err) {
      const detail = apiErrorDetails(err)
      setFormError(detail || apiErrorMessage(err))
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } finally {
      setLoading(false)
    }
  }

  const copy = (value, label) => {
    navigator.clipboard.writeText(value)
    setDialog({ title: 'Copied', message: `${label} copied to clipboard!`, type: 'success' })
  }

  if (result) {
    return (
      <div className="register-page">
        <div className="success-panel">
          <h2>Registration Successful!</h2>

          <div className="cred-row">
            <span>Card Number:</span>
            <input readOnly value={result.cardNumber} />
            <button className="copy-btn" onClick={() => copy(result.cardNumber, 'Card number')}>
              Copy
            </button>
          </div>

          <div className="cred-row">
            <span>Your PIN:</span>
            <input readOnly value={result.pin} />
            <button className="copy-btn" onClick={() => copy(result.pin, 'PIN')}>
              Copy
            </button>
          </div>

          {result.accountNumber && (
            <div className="cred-row">
              <span>Account No.:</span>
              <input readOnly value={result.accountNumber} />
              <button className="copy-btn" onClick={() => copy(result.accountNumber, 'Account number')}>
                Copy
              </button>
            </div>
          )}

          <p className="success-note">Please note these details for future login</p>

          <div className="register-submit register-submit-spaced">
            <button className="atm-btn" onClick={() => navigate('/login')}>
              GO TO LOGIN
            </button>
          </div>
        </div>

        {dialog && (
          <MessageDialog
            title={dialog.title}
            message={dialog.message}
            type={dialog.type}
            onClose={() => setDialog(null)}
          />
        )}
      </div>
    )
  }

  return (
    <div className="register-page">
      <div className="register-header">
        <img src="/icons/bank.png" alt="bank" />
        <h1>Account Registration Form</h1>
        <button
          type="button"
          className="atm-btn btn-ghost register-back"
          onClick={() => navigate('/login')}
        >
          Back to Login
        </button>
      </div>

      <p className="register-required-note">
        <span className="req-mark">*</span> indicates a required field
      </p>

      {formError && (
        <div className="error-box register-error" role="alert">
          {formError}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="tab-bar register-tabs" role="tablist" aria-label="Registration sections">
          {TABS.map((t, i) => {
            const unlocked = isTabUnlocked(i)
            return (
              <button
                key={t.id}
                type="button"
                role="tab"
                aria-selected={tab === t.id}
                aria-disabled={!unlocked}
                disabled={!unlocked}
                title={unlocked ? undefined : 'Complete previous section first'}
                className={`tab-btn ${tab === t.id ? 'is-on' : ''}`}
                onClick={() => {
                  if (!unlocked) return
                  setFieldErrors({})
                  setFormError('')
                  setTab(t.id)
                }}
              >
                {t.label}
              </button>
            )
          })}
        </div>

        {tab === 'personal' && (
        <section className="form-section">
          <h2>Personal Details</h2>
          <div className={`form-row ${fieldErrors.name ? 'has-error' : ''}`}>
            <label>Name: <Req /></label>
            <div className="field-wrap">
              <input type="text" value={form.name} onChange={set('name')} />
              <FieldError message={fieldErrors.name} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.fatherName ? 'has-error' : ''}`}>
            <label>Father's Name: <Req /></label>
            <div className="field-wrap">
              <input type="text" value={form.fatherName} onChange={set('fatherName')} />
              <FieldError message={fieldErrors.fatherName} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.dob ? 'has-error' : ''}`}>
            <label>Date of Birth: <Req /></label>
            <div className="field-wrap">
              <input type="date" value={form.dob} onChange={set('dob')} max={
                (() => { const d = new Date(); d.setFullYear(d.getFullYear() - 18); return d.toISOString().slice(0, 10) })()
              } />
              <FieldError message={fieldErrors.dob} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.gender ? 'has-error' : ''}`}>
            <label>Gender: <Req /></label>
            <div className="field-wrap">
              <div className="radio-group">
                {['Male', 'Female'].map((g) => (
                  <label key={g}>
                    <input type="radio" name="gender" checked={form.gender === g}
                           onChange={setRadio('gender', g)} /> {g}
                  </label>
                ))}
              </div>
              <FieldError message={fieldErrors.gender} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.email ? 'has-error' : ''}`}>
            <label>Email: <Req /></label>
            <div className="field-wrap">
              <input type="email" value={form.email} onChange={set('email')} />
              <FieldError message={fieldErrors.email} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.marital ? 'has-error' : ''}`}>
            <label>Marital Status: <Req /></label>
            <div className="field-wrap">
              <div className="radio-group">
                {['Married', 'Unmarried', 'Other'].map((m) => (
                  <label key={m}>
                    <input type="radio" name="marital" checked={form.marital === m}
                           onChange={setRadio('marital', m)} /> {m}
                  </label>
                ))}
              </div>
              <FieldError message={fieldErrors.marital} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.address ? 'has-error' : ''}`}>
            <label>Address: <Req /></label>
            <div className="field-wrap">
              <input type="text" value={form.address} onChange={set('address')} />
              <FieldError message={fieldErrors.address} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.city ? 'has-error' : ''}`}>
            <label>City: <Req /></label>
            <div className="field-wrap">
              <input type="text" value={form.city} onChange={set('city')} />
              <FieldError message={fieldErrors.city} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.state ? 'has-error' : ''}`}>
            <label>State: <Req /></label>
            <div className="field-wrap">
              <select value={form.state} onChange={set('state')}>
                <option value="">-- Select State --</option>
                {STATES.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
              <FieldError message={fieldErrors.state} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.pincode ? 'has-error' : ''}`}>
            <label>PIN Code: <Req /></label>
            <div className="field-wrap">
              <input type="text" value={form.pincode}
                     onChange={(e) => {
                       setForm({ ...form, pincode: e.target.value.replace(/\D/g, '').slice(0, 6) })
                       clearField('pincode')
                     }}
                     maxLength={6} />
              <FieldError message={fieldErrors.pincode} />
            </div>
          </div>
        </section>
        )}

        {tab === 'additional' && (
        <section className="form-section">
          <h2>Additional Details</h2>
          <div className={`form-row ${fieldErrors.religion ? 'has-error' : ''}`}>
            <label>Religion: <Req /></label>
            <div className="field-wrap">
              <select value={form.religion} onChange={set('religion')}>
                <option value="">-- Select --</option>
                {['Hindu', 'Muslim', 'Sikh', 'Christian', 'Other'].map((x) => (
                  <option key={x} value={x}>{x}</option>
                ))}
              </select>
              <FieldError message={fieldErrors.religion} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.category ? 'has-error' : ''}`}>
            <label>Category: <Req /></label>
            <div className="field-wrap">
              <select value={form.category} onChange={set('category')}>
                <option value="">-- Select --</option>
                {['General', 'OBC', 'SC', 'ST', 'Other'].map((x) => (
                  <option key={x} value={x}>{x}</option>
                ))}
              </select>
              <FieldError message={fieldErrors.category} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.income ? 'has-error' : ''}`}>
            <label>Income: <Req /></label>
            <div className="field-wrap">
              <select value={form.income} onChange={set('income')}>
                <option value="">-- Select --</option>
                {['<1.5L', '<2.5L', '<5L', '<10L', '>10L'].map((x) => (
                  <option key={x} value={x}>{x}</option>
                ))}
              </select>
              <FieldError message={fieldErrors.income} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.education ? 'has-error' : ''}`}>
            <label>Education: <Req /></label>
            <div className="field-wrap">
              <select value={form.education} onChange={set('education')}>
                <option value="">-- Select --</option>
                {['Non-Graduate', 'Graduate', 'Post-Graduate', 'Doctorate'].map((x) => (
                  <option key={x} value={x}>{x}</option>
                ))}
              </select>
              <FieldError message={fieldErrors.education} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.occupation ? 'has-error' : ''}`}>
            <label>Occupation: <Req /></label>
            <div className="field-wrap">
              <select value={form.occupation} onChange={set('occupation')}>
                <option value="">-- Select --</option>
                {['Salaried', 'Self-Employed', 'Business', 'Student', 'Retired'].map((x) => (
                  <option key={x} value={x}>{x}</option>
                ))}
              </select>
              <FieldError message={fieldErrors.occupation} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.pan ? 'has-error' : ''}`}>
            <label>PAN Number: <Req /></label>
            <div className="field-wrap">
              <input type="text" value={form.pan}
                     onChange={(e) => {
                       setForm({ ...form, pan: e.target.value.toUpperCase().slice(0, 10) })
                       clearField('pan')
                     }}
                     maxLength={10} />
              <FieldError message={fieldErrors.pan} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.aadhar ? 'has-error' : ''}`}>
            <label>Aadhar Number: <Req /></label>
            <div className="field-wrap">
              <input type="text" value={form.aadhar}
                     onChange={(e) => {
                       setForm({ ...form, aadhar: e.target.value.replace(/\D/g, '').slice(0, 12) })
                       clearField('aadhar')
                     }}
                     maxLength={12} />
              <FieldError message={fieldErrors.aadhar} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.seniorCitizen ? 'has-error' : ''}`}>
            <label>Senior Citizen: <Req /></label>
            <div className="field-wrap">
              <div className="radio-group">
                {['Yes', 'No'].map((v) => (
                  <label key={v}>
                    <input type="radio" name="senior" checked={form.seniorCitizen === v}
                           onChange={setRadio('seniorCitizen', v)} /> {v}
                  </label>
                ))}
              </div>
              <FieldError message={fieldErrors.seniorCitizen} />
            </div>
          </div>
          <div className={`form-row ${fieldErrors.existingAccount ? 'has-error' : ''}`}>
            <label>Existing Account: <Req /></label>
            <div className="field-wrap">
              <div className="radio-group">
                {['Yes', 'No'].map((v) => (
                  <label key={v}>
                    <input type="radio" name="existing" checked={form.existingAccount === v}
                           onChange={setRadio('existingAccount', v)} /> {v}
                  </label>
                ))}
              </div>
              <FieldError message={fieldErrors.existingAccount} />
            </div>
          </div>
        </section>
        )}

        {tab === 'account' && (
        <section className="form-section">
          <h2>Account Details</h2>
          <p className="register-section-note">
            Choose how you’ll hold funds and which free services you want on the account.
            You can update facilities later after onboarding.
          </p>
          <div className="form-row">
            <label>Account Type:</label>
            <div className="field-wrap">
              <select value={form.accountType} onChange={set('accountType')}>
                {['Saving', 'Fixed Deposit', 'Current', 'Recurring Deposit'].map((x) => (
                  <option key={x} value={x}>{x}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="form-row">
            <label>Facilities:</label>
            <div className="field-wrap">
              <div className="checkbox-grid">
                {FACILITIES.map((f) => (
                  <label key={f}>
                    <input type="checkbox"
                           checked={form.facilities.includes(f)}
                           onChange={toggleFacility(f)} /> {f}
                  </label>
                ))}
              </div>
            </div>
          </div>
          <p className="fill-note register-section-note">
            Saving suits everyday deposits and withdrawals; Current is built for business cash flow;
            Fixed and Recurring deposits earn tenure-based returns but limit instant withdrawals.
            ATM card, internet banking, and cheque book are enabled by default for eligible products.
          </p>
        </section>
        )}

        {tab === 'security' && (
        <section className="form-section">
          <h2>Security Details</h2>
          <p className="register-section-note">
            This question unlocks PIN reset if you forget your PIN. Pick something only you know.
          </p>
          <div className="form-row">
            <label>Security Question:</label>
            <div className="field-wrap">
              <select value={form.securityQuestion} onChange={set('securityQuestion')}>
                {SECURITY_QUESTIONS.map((q) => <option key={q} value={q}>{q}</option>)}
              </select>
            </div>
          </div>
          <div className={`form-row ${fieldErrors.securityAnswer ? 'has-error' : ''}`}>
            <label>Answer: <Req /></label>
            <div className="field-wrap">
              <input type="text" value={form.securityAnswer} onChange={set('securityAnswer')} />
              <FieldError message={fieldErrors.securityAnswer} />
            </div>
          </div>
          <p className="fill-note register-section-note">
            Answers are case-insensitive but must match exactly (including numbers and punctuation).
            Avoid public details (city of birth, pet names on social media).
            KodBank staff will never ask for this answer over the phone or email.
            If you lose access, visit a branch with photo ID for assisted recovery.
          </p>
        </section>
        )}

        <div className="register-submit">
          <p className="fill-note register-submit-tips">
            Tip: complete each step before the next unlocks. Required fields are marked with
            <span className="req-mark"> *</span>. You can go back without losing earlier answers.
            After submission you’ll receive a card number — copy it before leaving the success screen.
          </p>
          <div className="register-submit-actions">
            {stepIndex > 0 && (
              <button
                type="button"
                className="atm-btn btn-secondary"
                onClick={() => {
                  setFieldErrors({})
                  setFormError('')
                  setTab(TABS[stepIndex - 1].id)
                  window.scrollTo({ top: 0, behavior: 'smooth' })
                }}
              >
                Back
              </button>
            )}
            {!isLastStep ? (
              <button type="button" className="atm-btn" onClick={handleNext}>
                Next
              </button>
            ) : allRequiredFilled ? (
              <button className={`atm-btn ${loading ? 'is-loading' : ''}`} type="submit" disabled={loading}>
                {loading ? 'Submitting…' : 'Submit Application'}
              </button>
            ) : (
              <p className="panel-hint register-submit-hint">
                Fill in the remaining details to enable Submit
              </p>
            )}
          </div>
        </div>
      </form>
    </div>
  )
}
