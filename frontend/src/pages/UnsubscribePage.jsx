import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { optOut } from '../api/api'
import { LoadingSpinner } from '../components/shared'
import './UnsubscribePage.css'

export default function UnsubscribePage() {
  const [searchParams] = useSearchParams()
  const [mode, setMode] = useState('LOADING')
  const [errorMessage, setErrorMessage] = useState(null)

  useEffect(() => {
    const subscriptionId = searchParams.get('subscriptionId')
    const phoneNumber = searchParams.get('phoneNumber')

    if (!subscriptionId || !phoneNumber) {
      setMode('NOT_FOUND')
      return
    }

    optOut({ subscriptionId, phoneNumber })
      .then(() => setMode('SUCCESS'))
      .catch((err) => {
        if (err.code === 'ALREADY_OPTED_OUT') {
          setMode('SUCCESS')
        } else if (err.code === 'SUBSCRIPTION_NOT_FOUND') {
          setMode('NOT_FOUND')
        } else {
          setErrorMessage(err.message ?? 'An unexpected error occurred.')
          setMode('ERROR')
        }
      })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="unsubscribe-page" data-testid="unsubscribe-page">
      <div className="unsubscribe-card">
        <div className="unsubscribe-logo">📅</div>

        {mode === 'LOADING' && (
          <div data-testid="unsubscribe-loading">
            <LoadingSpinner size="md" />
            <p className="unsubscribe-body">Processing your request…</p>
          </div>
        )}

        {mode === 'SUCCESS' && (
          <div data-testid="unsubscribe-success">
            <h1 className="unsubscribe-heading">Unsubscribed</h1>
            <p className="unsubscribe-body">
              You have been unsubscribed and will no longer receive alerts for this event.
            </p>
          </div>
        )}

        {mode === 'NOT_FOUND' && (
          <div data-testid="unsubscribe-not-found">
            <h1 className="unsubscribe-heading">Subscription not found</h1>
            <p className="unsubscribe-body">
              We couldn't find that subscription. It may have already been removed.
            </p>
          </div>
        )}

        {mode === 'ERROR' && (
          <div data-testid="unsubscribe-error">
            <h1 className="unsubscribe-heading">Something went wrong</h1>
            <p className="unsubscribe-body">
              {errorMessage}
            </p>
            <p className="unsubscribe-retry-hint">
              Please try tapping the unsubscribe link in your SMS again.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
