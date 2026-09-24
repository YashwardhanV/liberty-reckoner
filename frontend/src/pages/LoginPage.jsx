import { useState } from 'react'
import { useAuth } from '../state/AuthContext'
import { errorMessage } from '../api/client'

export default function LoginPage() {
  const { login } = useAuth()
  const [form, setForm] = useState({ email: 'admin@libertyreckoner.gov.in', password: 'Liberty@123' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const submit = async event => {
    event.preventDefault()
    setSubmitting(true); setError('')
    try { await login(form) } catch (err) { setError(errorMessage(err)) } finally { setSubmitting(false) }
  }

  return (
    <main className="login-page">
      <section className="login-story">
        <div className="login-orbit orbit-one" /><div className="login-orbit orbit-two" />
        <div className="story-content">
          <div className="brand-lockup brand-lockup-login"><div className="brand-mark"><span /><span /></div><div><strong>Liberty Reckoner</strong><small>Custody rights register</small></div></div>
          <span className="eyebrow eyebrow-light">Custody rights, made actionable</span>
          <h1>Every lawful day<br />should be <em>counted.</em></h1>
          <p>One auditable command layer for prisons, legal services and courts—from verified custody to physical release.</p>
          <div className="story-proof">
            <div><strong>01</strong><span>Explain every eligibility result</span></div>
            <div><strong>02</strong><span>Route every statutory action</span></div>
            <div><strong>03</strong><span>Close the loop after the order</span></div>
          </div>
        </div>
      </section>
      <section className="login-panel">
        <form className="login-card" onSubmit={submit}>
          <span className="eyebrow">Authorised access</span>
          <h2>Enter the operations desk</h2>
          <p>Use your secure justice-network identity.</p>
          {error && <div className="form-alert"><i className="bi bi-exclamation-triangle" />{error}</div>}
          <label className="field-label">Official email
            <span className="input-shell"><i className="bi bi-envelope" /><input type="email" required value={form.email}
              onChange={event => setForm({ ...form, email: event.target.value })} autoComplete="username" /></span>
          </label>
          <label className="field-label">Password
            <span className="input-shell"><i className="bi bi-lock" /><input type="password" required value={form.password}
              onChange={event => setForm({ ...form, password: event.target.value })} autoComplete="current-password" /></span>
          </label>
          <button className="btn btn-brand w-100" disabled={submitting}>{submitting ? <><span className="spinner-border spinner-border-sm me-2" />Verifying</> : <>Continue securely<i className="bi bi-arrow-right" /></>}</button>
          <div className="demo-access"><i className="bi bi-info-circle" /><span>Demonstration access is pre-filled. Production deployments should disable demo seeding.</span></div>
          <div className="secure-note"><i className="bi bi-shield-lock-fill" />Protected justice-network session · RBAC enabled</div>
        </form>
      </section>
    </main>
  )
}
