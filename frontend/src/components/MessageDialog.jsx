export default function MessageDialog({ title, message, type = 'info', onClose }) {
  return (
    <div className="msg-overlay" onClick={onClose}>
      <div className={`msg-dialog ${type === 'error' ? 'error' : type === 'success' ? 'success' : ''}`}
           onClick={(e) => e.stopPropagation()}>
        <h3>{title}</h3>
        <div>{message}</div>
        <button className="atm-btn" onClick={onClose}>OK</button>
      </div>
    </div>
  )
}
