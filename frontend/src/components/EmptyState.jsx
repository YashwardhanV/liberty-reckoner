export default function EmptyState({ icon = 'bi-inbox', title, message }) {
  return <div className="empty-state"><i className={`bi ${icon}`} /><h3>{title}</h3><p>{message}</p></div>
}
