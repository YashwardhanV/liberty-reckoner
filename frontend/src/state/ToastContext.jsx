import { createContext, useCallback, useContext, useMemo, useState } from 'react'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const notify = useCallback((message, type = 'success') => {
    const id = crypto.randomUUID()
    setToasts(items => [...items, { id, message, type }])
    window.setTimeout(() => setToasts(items => items.filter(item => item.id !== id)), 4500)
  }, [])
  const value = useMemo(() => ({ notify }), [notify])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-stack" aria-live="polite" aria-atomic="true">
        {toasts.map(toast => (
          <div key={toast.id} className={`app-toast app-toast-${toast.type}`}>
            <i className={`bi ${toast.type === 'danger' ? 'bi-exclamation-octagon' : 'bi-check-circle-fill'}`} />
            <span>{toast.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)

