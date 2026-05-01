export default function ErrorMessage({ error, onRetry }) {
  if (!error?.message) return null

  return (
    <div data-testid="error-message" style={{ color: '#EF4444', textAlign: 'center', padding: '16px' }}>
      <p>{error.message}</p>
      {onRetry && (
        <button onClick={onRetry} style={{ marginTop: '8px' }}>
          Try again
        </button>
      )}
    </div>
  )
}
