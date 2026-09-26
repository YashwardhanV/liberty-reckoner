import { useCallback, useEffect, useMemo, useState } from 'react'
import api, { errorMessage } from '../api/client'
import PageHeader from '../components/PageHeader'
import LoadingState from '../components/LoadingState'
import EmptyState from '../components/EmptyState'
import { formatDateTime, words } from '../utils/format'

const filters = ['', 'PENDING', 'PROCESSING', 'DELIVERED', 'FAILED', 'DEAD_LETTER']

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState(null)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const load = useCallback(() => {
    setError('')
    api.get('/notifications/outbox', { params: status ? { status } : {} })
      .then(({ data }) => setNotifications(data))
      .catch(err => setError(errorMessage(err)))
  }, [status])
  useEffect(() => { load() }, [load])

  const totals = useMemo(() => ({
    delivered: notifications?.filter(item => item.status === 'DELIVERED').length || 0,
    waiting: notifications?.filter(item => ['PENDING', 'PROCESSING'].includes(item.status)).length || 0,
    attention: notifications?.filter(item => ['FAILED', 'DEAD_LETTER'].includes(item.status)).length || 0
  }), [notifications])

  if (!notifications && !error) return <LoadingState label="Reading the delivery ledger" />
  return <div className="page-enter">
    <PageHeader eyebrow="Transactional outbox" title="Every statutory alert has a delivery record."
      description="Eligibility alerts are committed with the legal assessment, then delivered independently with retry and dead-letter handling." />
    <section className="delivery-overview">
      <div><i className="bi bi-check2-circle" /><span>Delivered in view</span><strong>{totals.delivered}</strong></div>
      <div><i className="bi bi-hourglass-split" /><span>Awaiting delivery</span><strong>{totals.waiting}</strong></div>
      <div className={totals.attention ? 'has-attention' : ''}><i className="bi bi-exclamation-diamond" /><span>Needs attention</span><strong>{totals.attention}</strong></div>
      <aside><label htmlFor="delivery-status">Delivery state</label><select id="delivery-status" value={status} onChange={event => setStatus(event.target.value)}>{filters.map(value => <option key={value || 'ALL'} value={value}>{value ? words(value) : 'All recent alerts'}</option>)}</select></aside>
    </section>
    {error ? <EmptyState icon="bi-cloud-slash" title="Delivery ledger unavailable" message={error} />
      : notifications.length === 0 ? <EmptyState icon="bi-inbox" title="No matching alerts" message="No delivery records match the selected state." />
        : <section className="delivery-ledger" aria-label="Notification delivery ledger">
          <header><span>Recipient</span><span>Statutory signal</span><span>Delivery</span><span>Created</span></header>
          {notifications.map(item => <article key={item.id}>
            <div className="delivery-recipient"><i className="bi bi-envelope-paper" /><div><strong>{words(item.recipientRole)}</strong><span>{item.recipientAddress}</span></div></div>
            <div className="delivery-subject"><strong>{item.subject}</strong><span>{item.prisonerNumber} · Attempt {item.attempts}</span>{item.lastError && <small>{item.lastError}</small>}</div>
            <span className={`delivery-state delivery-${item.status.toLowerCase()}`}><i />{words(item.status)}</span>
            <time>{formatDateTime(item.createdAt)}</time>
          </article>)}
        </section>}
  </div>
}
