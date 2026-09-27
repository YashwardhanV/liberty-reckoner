import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errorMessage } from '../api/client'
import PageHeader from '../components/PageHeader'
import StatusBadge from '../components/StatusBadge'
import LoadingState from '../components/LoadingState'
import EmptyState from '../components/EmptyState'
import { daysText, formatDate } from '../utils/format'

export default function PrisonersPage() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setLoading(true); setError('')
      api.get('/prisoners', { params: { query, page, size: 20 } })
        .then(({ data }) => setResult(data)).catch(err => setError(errorMessage(err))).finally(() => setLoading(false))
    }, 250)
    return () => window.clearTimeout(timer)
  }, [query, page])

  const visibleRecords = result?.content.filter(person => filter === 'ALL'
    || (filter === 'ACTION' && ['ACTION_OVERDUE', 'MAXIMUM_REACHED', 'DATA_INCOMPLETE', 'BAIL_GRANTED'].includes(person.status))
    || (filter === 'DUE_SOON' && person.status === 'DUE_SOON')) || []

  return (
    <div className="page-enter">
      <PageHeader eyebrow="Custody registry" title="Every person. Every case. Every counted day."
        description="A reconciled person–case view across prison, police and court records." />
      <section className="registry-toolbar">
        <label className="search-box"><i className="bi bi-search" /><input aria-label="Search custody registry"
          placeholder="Search name, prison number or CNR…" value={query} onChange={event => { setQuery(event.target.value); setPage(0) }} />
          {query && <button onClick={() => setQuery('')} aria-label="Clear search"><i className="bi bi-x-lg" /></button>}</label>
        <div className="registry-filters"><button className={`filter-chip ${filter === 'ALL' ? 'active' : ''}`} onClick={() => setFilter('ALL')}>All records</button><button className={`filter-chip ${filter === 'ACTION' ? 'active' : ''}`} onClick={() => setFilter('ACTION')}>Needs action</button><button className={`filter-chip ${filter === 'DUE_SOON' ? 'active' : ''}`} onClick={() => setFilter('DUE_SOON')}>Due soon</button></div>
        <div className="record-count"><strong>{result?.totalElements ?? '—'}</strong><span>people monitored</span></div>
      </section>

      <section className="panel registry-panel">
        {loading ? <LoadingState /> : error ? <EmptyState icon="bi-cloud-slash" title="Registry unavailable" message={error} /> : visibleRecords.length === 0
          ? <EmptyState icon="bi-person-x" title="No matching records" message="Try a different name or prison number." />
          : <div className="table-responsive"><table className="table registry-table align-middle mb-0">
            <thead><tr><th>Undertrial</th><th>Custody signal</th><th>Case load</th><th>Next hearing</th><th>Current location</th><th aria-label="Open record" /></tr></thead>
            <tbody>{visibleRecords.map(person => (
              <tr key={person.id}>
                <td><Link className="person-cell" to={`/prisoners/${person.id}`}><span className="person-avatar">{person.fullName.split(' ').map(x => x[0]).slice(0, 2).join('')}</span><span><strong>{person.fullName}</strong><small>{person.prisonNumber}</small></span></Link></td>
                <td><StatusBadge status={person.status} compact /><small className="days-hint">{daysText(person.daysRemaining)} · {person.custodyDays} counted</small></td>
                <td><span className="case-count"><strong>{person.activeCases}</strong> active {person.activeCases === 1 ? 'case' : 'cases'}</span></td>
                <td><span className="date-cell"><i className="bi bi-calendar-event" />{formatDate(person.nextHearingDate)}</span></td>
                <td><span className="location-cell"><strong>{person.prisonName}</strong><small>{person.district}</small></span></td>
                <td><Link className="row-open" to={`/prisoners/${person.id}`} aria-label={`Open ${person.fullName}`}><i className="bi bi-arrow-up-right" /></Link></td>
              </tr>
            ))}</tbody>
          </table></div>}
        {result?.totalPages > 1 && <footer className="pagination-bar"><span>Page {page + 1} of {result.totalPages}</span><div><button disabled={page === 0} onClick={() => setPage(value => value - 1)}><i className="bi bi-arrow-left" />Previous</button><button disabled={page + 1 >= result.totalPages} onClick={() => setPage(value => value + 1)}>Next<i className="bi bi-arrow-right" /></button></div></footer>}
      </section>
    </div>
  )
}
