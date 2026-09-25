export default function MetricTile({ label, value, hint, icon, tone = 'ink', trend }) {
  return (
    <article className={`metric-tile metric-${tone}`}>
      <div className="metric-icon"><i className={`bi ${icon}`} /></div>
      <div className="metric-copy"><span>{label}</span><strong>{value}</strong><small>{hint}</small></div>
      {trend && <div className="metric-trend">{trend}</div>}
    </article>
  )
}

