import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api/client'
import PageHeader from '../components/PageHeader'
import LoadingState from '../components/LoadingState'
import EmptyState from '../components/EmptyState'

export default function RulesPage() {
  const [rules, setRules] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { api.get('/rules').then(({ data }) => setRules(data)).catch(err => setError(errorMessage(err))) }, [])
  if (!rules && !error) return <LoadingState label="Loading the approved rules catalogue" />
  return <div className="page-enter"><PageHeader eyebrow="Explainable rules catalogue" title="Law first. Calculation second."
    description="Every automated signal maps to a visible authority, applicability test and human-controlled system action." />
    <section className="rules-principle"><div className="principle-mark"><span /><span /></div><div><span className="eyebrow">Design boundary</span><h2>Liberty Reckoner never grants bail.</h2><p>It verifies custody inputs, applies approved deterministic rules, prepares the statutory workflow and preserves the court’s decision-making authority.</p></div><aside><i className="bi bi-cpu" /><strong>No opaque risk score</strong><span>AI may assist extraction, never legal eligibility.</span></aside></section>
    {error ? <EmptyState icon="bi-cloud-slash" title="Rules unavailable" message={error} /> : <section className="rules-grid">{rules.map((rule, index) => <article className="rule-card" key={rule.id}><header><span>{String(index + 1).padStart(2, '0')}</span><code>{rule.id}</code></header><h2>{rule.title}</h2><div className="rule-threshold"><span>Threshold</span><strong>{rule.threshold}</strong></div><dl><div><dt>Applicability</dt><dd>{rule.applicability}</dd></div><div><dt>Liberty Reckoner treatment</dt><dd>{rule.systemTreatment}</dd></div></dl><footer><i className="bi bi-journal-bookmark-fill" />{rule.authority}</footer></article>)}</section>}
    <section className="governance-strip"><i className="bi bi-diagram-3-fill" /><div><strong>Production governance requirement</strong><span>Rule changes require legal approval, effective dating, regression tests and an auditable migration before activation.</span></div><button className="btn btn-light" disabled>Rule version BNSS-479-v1.0</button></section>
  </div>
}
