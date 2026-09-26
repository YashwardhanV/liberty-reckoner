import { labelStatus } from '../utils/format'

export default function StatusBadge({ status, compact = false }) {
  return <span className={`status-badge status-${String(status).toLowerCase()} ${compact ? 'status-compact' : ''}`}><i />{labelStatus(status)}</span>
}

