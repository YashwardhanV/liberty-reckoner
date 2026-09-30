const statusLabels = {
  DATA_INCOMPLETE: 'Data incomplete',
  NOT_DUE: 'Not due',
  DUE_SOON: 'Due soon',
  ACTION_OVERDUE: 'Action overdue',
  LEGAL_REVIEW: 'Legal review',
  SECTION_479_INAPPLICABLE: 'Other bail review',
  MAXIMUM_REACHED: 'Maximum reached',
  APPLICATION_FILED: 'Application filed',
  BAIL_GRANTED: 'Bail granted',
  RELEASED: 'Released'
}

export const labelStatus = status => statusLabels[status] || String(status || 'Unknown').replaceAll('_', ' ')
export const words = value => String(value || '').toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, letter => letter.toUpperCase())
export const formatDate = value => value ? new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(`${value}T00:00:00`)) : 'Not recorded'
export const formatDateTime = value => value ? new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(value)) : 'Not recorded'
export const daysText = days => {
  if (days === null || days === undefined) return 'Review required'
  if (days < 0) return `${Math.abs(days)} days overdue`
  if (days === 0) return 'Due today'
  return `${days} days remaining`
}
export const initials = name => String(name || 'Liberty Reckoner').split(' ').slice(0, 2).map(part => part[0]).join('').toUpperCase()
