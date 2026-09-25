export default function CustodyRing({ custodyDays = 0, thresholdDays, size = 'md' }) {
  const percentage = thresholdDays ? Math.min(100, Math.round((custodyDays / thresholdDays) * 100)) : 0
  return (
    <div className={`custody-ring ring-${size}`} style={{ '--ring-value': `${percentage * 3.6}deg` }}
      aria-label={`${percentage}% of recorded threshold`}>
      <div><strong>{percentage}%</strong><span>threshold</span></div>
    </div>
  )
}

