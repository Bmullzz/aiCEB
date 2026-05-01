import { useEffect } from 'react'

const TYPE_COLORS = {
  success: '#10B981',
  error: '#EF4444',
  info: '#3B82F6',
}

export default function Toast({ message, type = 'info', onDismiss }) {
  useEffect(() => {
    const timer = setTimeout(onDismiss, 4000)
    return () => clearTimeout(timer)
  }, [onDismiss])

  const backgroundColor = TYPE_COLORS[type] ?? TYPE_COLORS.info

  return (
    <div
      data-testid="toast"
      style={{
        position: 'fixed',
        top: '16px',
        left: '50%',
        transform: 'translateX(-50%)',
        backgroundColor,
        color: '#fff',
        padding: '12px 20px',
        borderRadius: '8px',
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        zIndex: 2000,
        minWidth: '240px',
      }}
    >
      <span style={{ flex: 1 }}>{message}</span>
      <button
        onClick={onDismiss}
        style={{ background: 'none', border: 'none', color: '#fff', fontSize: '18px', cursor: 'pointer' }}
        aria-label="Dismiss"
      >
        ×
      </button>
    </div>
  )
}
