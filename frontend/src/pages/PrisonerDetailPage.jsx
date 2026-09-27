import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api, { errorMessage } from '../api/client'
import { useToast } from '../state/ToastContext'
import LoadingState from '../components/LoadingState'
import EmptyState from '../components/EmptyState'
import StatusBadge from '../components/StatusBadge'
import CustodyRing from '../components/CustodyRing'
import ApplicationModal from '../components/ApplicationModal'
import { daysText, formatDate, formatDateTime, words } from '../utils/format'

export default function PrisonerDetailPage() {
  const { id } = useParams()
  const { notify } = useToast()
  const [person, setPerson] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState('')
  const [packet, setPacket] = useState(null)

  const load = useCallback(() => api.get(`/prisoners/${id}`).then(({ data }) => setPerson(data)).catch(err => setError(errorMessage(err))), [id])
  useEffect(() => { load() }, [load])
  if (!person && !error) return <LoadingState label="Opening the verified custody ledger" />
  if (error) return <EmptyState icon="bi-person-x" title="Record unavailable" message={error} />

  const evaluate = async caseId => {
    setBusy(caseId)
    try { await api.post(`/cases/${caseId}/evaluate`); await load(); notify('Eligibility assessment refreshed from verified records.') }
    catch (err) { notify(errorMessage(err), 'danger') } finally { setBusy('') }
  }
  const generatePacket = async caseId => {
    setBusy(caseId)
    try { const { data } = await api.get(`/cases/${caseId}/application-packet`); setPacket(data) }
    catch (err) { notify(errorMessage(err), 'danger') } finally { setBusy('') }
  }

  return (
    <div className="page-enter detail-page">
      <Link to="/prisoners" className="back-link"><i className="bi bi-arrow-left" />Custody registry</Link>
      <header className="person-hero">
        <div className="person-identity"><span className="person-avatar person-avatar-lg">{person.fullName.split(' ').map(x => x[0]).slice(0, 2).join('')}</span><div><span className="eyebrow">Undertrial record · {person.prisonNumber}</span><h1>{person.fullName}</h1><p>{person.gender.toLowerCase()} · {person.nationality} · Preferred language: {person.preferredLanguage}</p></div></div>
        <div className="identity-trust"><i className={`bi ${person.convictionHistoryVerified ? 'bi-patch-check-fill' : 'bi-exclamation-diamond-fill'}`} /><div><span>Conviction history</span><strong>{person.convictionHistoryVerified ? `${person.previousConvictions} previous · verified` : 'Verification required'}</strong></div></div>
      </header>

      <div className="detail-meta-grid">
        <article><i className="bi bi-building-lock" /><span>Current prison</span><strong>{person.prison.name}</strong><small>{person.prison.district}, {person.prison.state}</small></article>
        <article><i className="bi bi-collection" /><span>Linked matters</span><strong>{person.cases.length} court {person.cases.length === 1 ? 'record' : 'records'}</strong><small>{person.cases.filter(item => item.active).length} currently active</small></article>
        <article><i className="bi bi-fingerprint" /><span>Identity key</span><strong>{person.prisonNumber}</strong><small>Prison system source record</small></article>
      </div>

      {person.cases.map((caseItem, index) => {
        const assessment = caseItem.assessment
        const canGenerate = ['ACTION_OVERDUE', 'MAXIMUM_REACHED', 'APPLICATION_FILED'].includes(assessment?.status)
        return <section className="case-workspace" key={caseItem.id}>
          <header className="case-workspace-head"><div><span className="case-index">Matter {String(index + 1).padStart(2, '0')}</span><h2>{caseItem.cnrNumber}</h2><p>{caseItem.courtName} · FIR {caseItem.firNumber}</p></div><div className="case-head-actions"><button className="btn btn-outline-brand" disabled={busy === caseItem.id} onClick={() => evaluate(caseItem.id)}><i className="bi bi-arrow-repeat" />Recalculate</button><button className="btn btn-brand" disabled={!canGenerate || busy === caseItem.id} onClick={() => generatePacket(caseItem.id)}><i className="bi bi-file-earmark-text" />Application packet</button></div></header>
          <div className="assessment-layout">
            <article className="assessment-card">
              <div className="assessment-top"><div><span className="eyebrow">Current assessment</span><StatusBadge status={assessment?.status || 'DATA_INCOMPLETE'} /></div><CustodyRing custodyDays={assessment?.creditedCustodyDays} thresholdDays={assessment?.thresholdDays} size="lg" /></div>
              <h3>{assessment?.explanation || 'No current assessment is available.'}</h3>
              <div className="assessment-numbers"><div><span>Counted custody</span><strong>{assessment?.creditedCustodyDays ?? '—'} days</strong></div><div><span>Applicable threshold</span><strong>{assessment?.thresholdDays ?? '—'} days</strong></div><div><span>Threshold date</span><strong>{formatDate(assessment?.projectedThresholdDate)}</strong></div><div><span>Current signal</span><strong>{daysText(assessment?.daysRemaining)}</strong></div></div>
              {assessment?.blockers?.length > 0 && <div className="blocker-list"><strong><i className="bi bi-signpost-split" />Legal or data blockers</strong>{assessment.blockers.map(blocker => <p key={blocker}>{blocker}</p>)}</div>}
              <footer className="assessment-proof"><span><i className="bi bi-shield-check" />{assessment?.verificationStatus === 'VERIFIED' ? 'Verified inputs' : 'Human verification required'}</span><span>Rule {assessment?.ruleVersion}</span><span>Assessed {formatDateTime(assessment?.assessedAt)}</span></footer>
            </article>

            <aside className="matter-card">
              <span className="eyebrow">Matter record</span>
              <dl><div><dt>Stage</dt><dd>{words(caseItem.stage)}</dd></div><div><dt>Police station</dt><dd>{caseItem.policeStation}</dd></div><div><dt>Next hearing</dt><dd>{formatDate(caseItem.nextHearingDate)}</dd></div><div><dt>Accused-caused delay</dt><dd>{caseItem.accusedDelayDays} verified days</dd></div></dl>
              <h3>Active charge</h3>{caseItem.charges.map(charge => <div className="charge-card" key={charge.id}><span>{charge.actName} · § {charge.sectionCode}</span><strong>{charge.description}</strong><small>Maximum: {charge.maximumTermLabel} · {charge.legalSource}</small></div>)}
            </aside>
          </div>

          <div className="ledger-workflow-grid">
            <section className="subpanel"><div className="subpanel-heading"><div><span className="eyebrow">Source-backed chronology</span><h3>Custody ledger</h3></div><span>{caseItem.custodyPeriods.length} period</span></div><div className="timeline">{caseItem.custodyPeriods.map(period => <div className="timeline-item" key={period.id}><i className={period.verified ? 'verified' : ''} /><div><strong>{formatDate(period.startDate)} → {period.endDate ? formatDate(period.endDate) : 'Present'}</strong><span>{period.sourceSystem}</span></div><span className={`source-chip ${period.verified ? '' : 'source-unverified'}`}>{period.verified ? 'Verified' : 'Check source'}</span></div>)}</div></section>
            <section className="subpanel"><div className="subpanel-heading"><div><span className="eyebrow">Accountable hand-offs</span><h3>Release workflow</h3></div><Link to="/workflow">Open queue</Link></div>{caseItem.tasks.length === 0 ? <EmptyState icon="bi-check2-circle" title="No active task" message="A new task will be created when an actionable state is detected." /> : <div className="workflow-mini">{caseItem.tasks.map(task => <div key={task.id} className={`workflow-mini-item ${task.status === 'COMPLETED' ? 'complete' : ''}`}><i className={`bi ${task.status === 'COMPLETED' ? 'bi-check-lg' : 'bi-arrow-right'}`} /><div><strong>{task.title}</strong><span>{words(task.assignedRole)} · due {formatDateTime(task.dueAt)}</span></div><small>{words(task.status)}</small></div>)}</div>}</section>
          </div>
        </section>
      })}
      <ApplicationModal packet={packet} onClose={() => setPacket(null)} />
    </div>
  )
}

