export default function LoadingState({ fullscreen = false, label = 'Loading verified records' }) {
  return (
    <div className={`loading-state ${fullscreen ? 'loading-fullscreen' : ''}`} role="status">
      <div className="pulse-loader"><span /><span /><span /></div>
      <p>{label}</p>
    </div>
  )
}

