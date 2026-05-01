import { format } from 'date-fns'
import { useEffect, useRef, useState } from 'react'
import { optOut, subscribe } from '../../api/api'
import { LoadingSpinner, Modal } from '../shared'
import './SmsSignupModal.css'

export function formatAlertOffsets(alertOffsets) {
  if (!alertOffsets || alertOffsets.length === 0) return 'before this event starts'
  if (alertOffsets.length === 1) return `${alertOffsets[0]} minutes before this event`
  return `${alertOffsets.slice(0, -1).join(', ')} and ${alertOffsets[alertOffsets.length - 1]} minutes before this event`
}

export function computeReminderTimes(event) {
  if (!event?.startTime || !event?.alertOffsets) return []
  const startMs = new Date(event.startTime).getTime()
  return event.alertOffsets.map((offset) =>
    format(new Date(startMs - offset * 60 * 1000), 'h:mm a')
  )
}

export default function SmsSignupModal({ isOpen, onClose, event }) {
  const [mode, setMode] = useState('FORM')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [inlineError, setInlineError] = useState(null)
  const [countdown, setCountdown] = useState(15)
  const [apiError, setApiError] = useState(null)
  const phoneInputRef = useRef(null)

  // Reset state when modal opens
  useEffect(() => {
    if (isOpen) {
      setMode('FORM')
      setPhoneNumber('')
      setInlineError(null)
      setApiError(null)
      setCountdown(15)
    }
  }, [isOpen])

  // Auto-focus phone input when modal opens in FORM mode
  useEffect(() => {
    if (isOpen && mode === 'FORM') {
      const t = setTimeout(() => phoneInputRef.current?.focus(), 100)
      return () => clearTimeout(t)
    }
  }, [isOpen, mode])

  // Auto-focus when inline error is set
  useEffect(() => {
    if (inlineError) phoneInputRef.current?.focus()
  }, [inlineError])

  // SUCCESS auto-dismiss countdown
  useEffect(() => {
    if (mode !== 'SUCCESS') return
    const interval = setInterval(() => {
      setCountdown((c) => {
        if (c <= 1) {
          clearInterval(interval)
          onClose(true)
          return 0
        }
        return c - 1
      })
    }, 1000)
    return () => clearInterval(interval)
  }, [mode]) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleSubmit() {
    setMode('LOADING')
    try {
      await subscribe({ eventId: event.id, phoneNumber })
      setMode('SUCCESS')
    } catch (err) {
      if (err.code === 'ALREADY_SUBSCRIBED') {
        setMode('ALREADY_SUBSCRIBED')
      } else if (err.code === 'INVALID_PHONE_NUMBER') {
        setMode('FORM')
        setInlineError('Please enter a valid phone number including area code.')
      } else {
        setApiError(err)
        setMode('ERROR')
      }
    }
  }

  async function handleOptOut() {
    try {
      await optOut({ subscriptionId: event?.subscriptionId, phoneNumber })
    } catch {
      // Opt-out errors are silent in this context
    } finally {
      onClose(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={() => onClose(false)}>
      {mode === 'FORM' && (
        <div data-testid="sms-modal-form" className="sms-modal-form">
          <h2 className="sms-modal-title">Get text alerts</h2>
          <p className="sms-modal-event-title">{event?.title}</p>
          <p className="sms-modal-schedule">
            You'll receive reminders {formatAlertOffsets(event?.alertOffsets)}
          </p>
          <div className="sms-modal-input-group">
            <input
              ref={phoneInputRef}
              type="tel"
              inputMode="numeric"
              autoComplete="tel"
              value={phoneNumber}
              onChange={(e) => { setPhoneNumber(e.target.value); setInlineError(null) }}
              placeholder="(215) 555-0123"
              className={`sms-modal-input${inlineError ? ' sms-modal-input-error' : ''}`}
              aria-label="Phone number"
              aria-describedby={inlineError ? 'phone-error' : undefined}
              data-testid="phone-input"
            />
            {inlineError && (
              <p id="phone-error" className="sms-modal-inline-error" role="alert">
                {inlineError}
              </p>
            )}
          </div>
          <button
            className="btn-primary sms-modal-submit"
            onClick={handleSubmit}
            disabled={!phoneNumber.trim()}
            data-testid="submit-button"
            style={{ minHeight: 44 }}
          >
            Send me alerts
          </button>
        </div>
      )}

      {mode === 'LOADING' && (
        <div data-testid="sms-modal-loading" className="sms-modal-loading">
          <LoadingSpinner size="md" />
          <p>Signing you up…</p>
        </div>
      )}

      {mode === 'SUCCESS' && (
        <div
          data-testid="sms-modal-success"
          className="sms-modal-success"
          onClick={() => setCountdown(15)}
        >
          <div className="sms-modal-success-icon" aria-hidden="true">✓</div>
          <h2>You're signed up!</h2>
          <p>{event?.title}</p>
          <p>
            We'll text you at{' '}
            {computeReminderTimes(event).join(' and ')}
            {' '}to remind you.
          </p>
          <p className="sms-modal-countdown" data-testid="countdown">
            Closing in {countdown}s
          </p>
          <button
            className="btn-secondary"
            onClick={() => onClose(true)}
            style={{ minHeight: 44 }}
            data-testid="done-button"
          >
            Done
          </button>
        </div>
      )}

      {mode === 'ALREADY_SUBSCRIBED' && (
        <div data-testid="sms-modal-already-subscribed" className="sms-modal-info">
          <h2>You're already signed up!</h2>
          <p>Good news — you're already signed up for alerts for {event?.title}.</p>
          <p>
            You'll receive reminders {formatAlertOffsets(event?.alertOffsets)}.
          </p>
          <div className="sms-modal-actions">
            <button
              className="btn-secondary"
              onClick={() => onClose(false)}
              style={{ minHeight: 44 }}
              data-testid="done-button"
            >
              Done
            </button>
            <button
              className="btn-text"
              onClick={handleOptOut}
              style={{ minHeight: 44 }}
              data-testid="unsubscribe-button"
            >
              Unsubscribe
            </button>
          </div>
        </div>
      )}

      {mode === 'ERROR' && (
        <div data-testid="sms-modal-error" className="sms-modal-error">
          <h2>Something went wrong</h2>
          <p>{apiError?.message ?? 'An unexpected error occurred. Please try again.'}</p>
          <div className="sms-modal-actions">
            <button
              className="btn-primary"
              onClick={() => { setMode('FORM'); setApiError(null) }}
              style={{ minHeight: 44 }}
              data-testid="try-again-button"
            >
              Try again
            </button>
            <button
              className="btn-secondary"
              onClick={() => onClose(false)}
              style={{ minHeight: 44 }}
              data-testid="close-button"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </Modal>
  )
}
