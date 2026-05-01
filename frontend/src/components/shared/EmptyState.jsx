export default function EmptyState({ message, icon }) {
  return (
    <div data-testid="empty-state" style={{ textAlign: 'center', padding: '32px', color: '#6B7280' }}>
      {icon && <div style={{ fontSize: '2rem', marginBottom: '8px' }}>{icon}</div>}
      <p>{message}</p>
    </div>
  )
}
