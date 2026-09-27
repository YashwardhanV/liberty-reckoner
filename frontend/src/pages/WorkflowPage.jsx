import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errorMessage } from '../api/client'
import { useToast } from '../state/ToastContext'
import PageHeader from '../components/PageHeader'
import LoadingState from '../components/LoadingState'
import EmptyState from '../components/EmptyState'
import { formatDateTime, words } from '../utils/format'

const taskIcons = { VERIFY_RECORDS: 'bi-patch-check', PREPARE_APPLICATION: 'bi-file-earmark-medical', FILE_APPLICATION: 'bi-send-check', SCHEDULE_HEARING: 'bi-calendar2-event', RECORD_ORDER: 'bi-bank', SATISFY_BOND: 'bi-pen', EXECUTE_RELEASE: 'bi-door-open', RESOLVE_DATA_CONFLICT: 'bi-database-exclamation' }

export default function WorkflowPage() {
  const { notify } = useToast()
  const [tasks, setTasks] = useState(null)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)
  const [notes, setNotes] = useState('Verified against the available source record.')
  const [outcome, setOutcome] = useState('PROCEED')
  const [busy, setBusy] = useState(false)
  const load = useCallback(() => api.get('/workflows/queue').then(({ data }) => setTasks(data)).catch(err => setError(errorMessage(err))), [])
  useEffect(() => { load() }, [load])
  const grouped = useMemo(() => ({ overdue: tasks?.filter(t => new Date(t.dueAt) < new Date()) || [], upcoming: tasks?.filter(t => new Date(t.dueAt) >= new Date()) || [] }), [tasks])

  const start = async task => {
    try { await api.post(`/workflows/${task.id}/start`); notify('Task assigned and marked in progress.'); await load() }
    catch (err) { notify(errorMessage(err), 'danger') }
  }
  const openOutcome = task => {
    setSelected(task)
    setOutcome(task.type === 'RECORD_ORDER' ? 'BAIL_GRANTED' : 'PROCEED')
    setNotes('Verified against the available source record.')
  }
  const complete = async event => {
    event.preventDefault(); setBusy(true)
    try { await api.post(`/workflows/${selected.id}/complete`, { notes, outcome, effectiveDate: new Date().toISOString().slice(0, 10) }); notify('Outcome recorded and the next accountable hand-off was created.'); setSelected(null); await load() }
    catch (err) { notify(errorMessage(err), 'danger') } finally { setBusy(false) }
  }

  if (!tasks && !error) return <LoadingState label="Loading accountable hand-offs" />
  return (
    <div className="page-enter">
      <PageHeader eyebrow="Action workflow" title="Nothing disappears after an alert."
        description="Each statutory step has an owner, deadline, outcome and next hand-off." />
      <section className="workflow-overview"><div><strong>{tasks?.length || 0}</strong><span>Open hand-offs</span></div><div><strong>{grouped.overdue.length}</strong><span>Past service deadline</span></div><div><strong>{new Set(tasks?.map(task => task.prisonerCaseId)).size || 0}</strong><span>People represented</span></div><aside><i className="bi bi-shield-check" /><span>Completion events are written to the immutable audit history.</span></aside></section>
      {error ? <EmptyState icon="bi-cloud-slash" title="Workflow unavailable" message={error} /> : tasks?.length === 0 ? <EmptyState icon="bi-check2-circle" title="The queue is clear" message="No statutory hand-offs are waiting for action." /> : <>
        {grouped.overdue.length > 0 && <TaskSection title="Deadline crossed" eyebrow="Escalation lane" tasks={grouped.overdue} start={start} select={openOutcome} />}
        {grouped.upcoming.length > 0 && <TaskSection title="Upcoming commitments" eyebrow="Planned lane" tasks={grouped.upcoming} start={start} select={openOutcome} />}
      </>}
      {selected && <div className="modal-layer" role="dialog" aria-modal="true" aria-labelledby="complete-title"><button className="modal-backdrop" onClick={() => setSelected(null)} aria-label="Close" /><form className="action-modal" onSubmit={complete}><header><div><span className="eyebrow">Record accountable outcome</span><h2 id="complete-title">{selected.title}</h2></div><button type="button" className="icon-button" onClick={() => setSelected(null)}><i className="bi bi-x-lg" /></button></header><div className="action-subject"><strong>{selected.prisonerName}</strong><span>{selected.prisonNumber} · {selected.cnrNumber}</span></div>{selected.type === 'RECORD_ORDER' && <label className="field-label">Judicial outcome<select value={outcome} onChange={e => setOutcome(e.target.value)}><option value="BAIL_GRANTED">Bail granted</option><option value="CONTINUED_DETENTION">Continued detention with recorded reasons</option><option value="REJECTED">Application rejected</option></select></label>}<label className="field-label">Verification note<textarea rows="4" value={notes} onChange={e => setNotes(e.target.value)} required /></label><p className="modal-caution"><i className="bi bi-info-circle" />This creates the next role-specific task automatically. It does not issue a judicial order.</p><footer><button type="button" className="btn btn-light" onClick={() => setSelected(null)}>Cancel</button><button className="btn btn-brand" disabled={busy}>{busy ? 'Recording…' : 'Complete and hand off'}<i className="bi bi-arrow-right" /></button></footer></form></div>}
    </div>
  )
}

function TaskSection({ title, eyebrow, tasks, start, select }) {
  return <section className="workflow-lane"><div className="lane-heading"><div><span className="eyebrow">{eyebrow}</span><h2>{title}</h2></div><span>{tasks.length} tasks</span></div><div className="task-grid">{tasks.map(task => <article className={`task-card ${new Date(task.dueAt) < new Date() ? 'task-overdue' : ''}`} key={task.id}><div className="task-icon"><i className={`bi ${taskIcons[task.type] || 'bi-check2-square'}`} /></div><div className="task-body"><span className="task-type">{words(task.type)}</span><h3>{task.title}</h3><Link to={`/prisoners/${task.prisonerId}`} className="task-person">{task.prisonerName}<small>{task.prisonNumber} · {task.cnrNumber}</small></Link><div className="task-meta"><span><i className="bi bi-person-badge" />{words(task.assignedRole)}</span><span><i className="bi bi-clock" />{formatDateTime(task.dueAt)}</span></div></div><footer>{task.status === 'OPEN' ? <button className="btn btn-outline-brand" onClick={() => start(task)}>Take ownership</button> : <span className="in-progress"><i />In progress</span>}<button className="btn btn-brand" onClick={() => select(task)}>Record outcome<i className="bi bi-arrow-right" /></button></footer></article>)}</div></section>
}
