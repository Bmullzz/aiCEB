import './LoadingSpinner.css'

export default function LoadingSpinner({ size = 'md' }) {
  return (
    <div className="spinner-wrapper" aria-label="Loading" data-testid="loading-spinner">
      <div className={`spinner spinner-${size}`} />
    </div>
  )
}
