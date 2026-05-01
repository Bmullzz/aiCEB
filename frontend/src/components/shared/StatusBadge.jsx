const STATUS_COLORS = {
  UPCOMING: '#5DCAA5',
  IN_PROGRESS: '#F59E0B',
  COMPLETED: '#9CA3AF',
  CANCELLED: '#EF4444',
}

export default function StatusBadge({ status }) {
  const backgroundColor = STATUS_COLORS[status] ?? '#9CA3AF'

  return (
    <span
      data-testid="status-badge"
      style={{
        backgroundColor,
        color: '#fff',
        padding: '2px 10px',
        borderRadius: '9999px',
        fontSize: '0.75rem',
        fontWeight: 600,
        display: 'inline-block',
      }}
    >
      {status}
    </span>
  )
}
