import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errorMessage } from '../api/client'
import PageHeader from '../components/PageHeader'
import MetricTile from '../components/MetricTile'
import StatusBadge from '../components/StatusBadge'
import LoadingState from '../components/LoadingState'
import EmptyState from '../components/EmptyState'
import { daysText, formatDate } from '../utils/format'

export default function DashboardPage() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => { api.get('/dashboard/summary').then(({ data }) => setData(data)).catch(err => setError(errorMessage(err))) }, [])
  if (!data && !error) return <LoadingState label="Reconciling custody and court records" />
  if (error) return <EmptyState icon="bi-cloud-slash" title="Dashboard unavailable" message={error} />

  const totalStatus = data.statusDistribution.reduce((sum, item) => sum + item.count, 0) || 1
  return (
    <div className="page-enter">
      <PageHeader eyebrow="Liberty operations command" title="Good morning. Here is what needs action."
        description="Verified custody thresholds, legal blockers and release workflows across the pilot network.">
        <Link to="/workflow" className="btn btn-brand"><i className="bi bi-intersect" />Open action queue<span className="button-count">{data.openTasks}</span></Link>
      </PageHeader>

      <section className="command-hero">
        <div className="hero-copy">
          <span className="live-label"><i />Live statutory watch</span>
          <h2><strong>{data.actionDue}</strong> cases require immediate statutory action.</h2>
          <p>{data.overdueTasks} operational tasks have crossed their service deadline. Each item remains assigned until a recorded outcome closes the loop.</p>
          <div className="hero-actions"><Link to="/prisoners" className="btn btn-light">Review custody registry</Link><span><i className="bi bi-arrow-repeat" />Auto-refresh at 02:15 IST</span></div>
        </div>
        <div className="hero-pulse" aria-label={`${data.actionDue} actions due`}>
          <div className="pulse-arc arc-a" /><div className="pulse-arc arc-b" /><div className="pulse-core"><strong>{data.actionDue}</strong><span>action due</span></div>
          <span className="pulse-tag tag-a">479(3)</span><span className="pulse-tag tag-b">Verified</span>
        </div>
      </section>

      <section className="metric-grid">
        <MetricTile label="Undertrials monitored" value={data.totalUndertrials} hint="Across connected prisons" icon="bi-people" tone="violet" />
        <MetricTile label="Threshold approaching" value={data.dueSoon} hint="Inside 90-day preparation window" icon="bi-hourglass-split" tone="amber" />
        <MetricTile label="Legal review" value={data.legalReview} hint="Multiple case or offence restriction" icon="bi-signpost-split" tone="blue" />
        <MetricTile label="Network occupancy" value={`${data.overallOccupancyPercentage}%`} hint="Against recorded capacity" icon="bi-building-lock" tone="coral" />
      </section>

      <div className="dashboard-grid">
        <section className="panel urgent-panel">
          <div className="panel-heading"><div><span className="eyebrow">Prioritised by legal urgency</span><h2>Cases needing attention</h2></div><Link to="/prisoners">View all <i className="bi bi-arrow-right" /></Link></div>
          <div className="case-queue">
            {data.urgentCases.map(item => (
              <Link key={item.prisonerCaseId} to={`/prisoners/${item.prisonerId}`} className="queue-item">
                <div className="queue-priority" /><div className="queue-person"><strong>{item.prisonerName}</strong><span>{item.prisonNumber} · {item.cnrNumber}</span></div>
                <div className="queue-status"><StatusBadge status={item.status} compact /><small>{daysText(item.daysRemaining)}</small></div>
                <div className="queue-hearing"><span>Next hearing</span><strong>{formatDate(item.nextHearingDate)}</strong></div>
                <i className="bi bi-chevron-right queue-chevron" />
              </Link>
            ))}
          </div>
        </section>

        <aside className="panel signal-panel">
          <div className="panel-heading"><div><span className="eyebrow">Portfolio signal</span><h2>Status pulse</h2></div></div>
          <div className="status-composition" aria-label="Eligibility status distribution">
            {data.statusDistribution.map((item, index) => <span key={item.status} className={`composition-segment segment-${index}`} style={{ width: `${(item.count / totalStatus) * 100}%` }} />)}
          </div>
          <div className="status-legend">
            {data.statusDistribution.map(item => <div key={item.status}><StatusBadge status={item.status} compact /><strong>{item.count}</strong></div>)}
          </div>
          <div className="signal-callout"><i className="bi bi-lightning-charge-fill" /><div><strong>Preparation beats escalation</strong><span>{data.dueSoon} records can be verified before the threshold arrives.</span></div></div>
        </aside>
      </div>

      <section className="panel occupancy-panel">
        <div className="panel-heading"><div><span className="eyebrow">Capacity intelligence</span><h2>Prison occupancy</h2></div><span className="data-source">Source · e-Prisons demo adapter</span></div>
        <div className="occupancy-grid">
          {data.occupancy.map(item => (
            <article key={item.prisonName} className="occupancy-row"><div><strong>{item.prisonName}</strong><span>{item.district}</span></div><div className="occupancy-track"><span style={{ width: `${Math.min(100, item.percentage)}%` }} /><i style={{ left: `${Math.min(97, item.percentage)}%` }}>{item.percentage}%</i></div><small>{item.population} / {item.capacity}</small></article>
          ))}
        </div>
      </section>
    </div>
  )
}
