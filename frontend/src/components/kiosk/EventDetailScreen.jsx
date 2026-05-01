import { format } from 'date-fns'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getEvent } from '../../api/api'
import { CategoryBadge, ErrorMessage, LoadingSpinner, StatusBadge } from '../shared'
import SmsSignupModal from './SmsSignupModal'
import './EventDetailScreen.css'

export default function EventDetailScreen() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [event, setEvent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [localSubscriberCount, setLocalSubscriberCount] = useState(0)
  const [showSignupModal, setShowSignupModal] = useState(false)

  useEffect(() => {
    getEvent(id)
      .then((data) => {
        setEvent(data)
        setLocalSubscriberCount(data.subscriberCount ?? 0)
        setLoading(false)
      })
      .catch((err) => {
        setError(err)
        setLoading(false)
      })
  }, [id])

  function handleClose() {
    navigate(-1)
  }

  function handleModalClose(didSignUp) {
    setShowSignupModal(false)
    if (didSignUp) setLocalSubscriberCount((c) => c + 1)
  }

  const isCancelled = event?.status === 'CANCELLED'

  return (
    <div data-testid="event-detail-screen" className="event-detail-overlay" onClick={handleClose}>
      <div className="event-detail-panel" onClick={(e) => e.stopPropagation()}>
        {loading && <LoadingSpinner />}
        {error && <ErrorMessage error={error} />}
        {event && (
          <>
            <div className="event-detail-header">
              <div className="event-detail-title">{event.title}</div>
              <button
                className="event-detail-close"
                onClick={handleClose}
                aria-label="Close"
              >
                ×
              </button>
            </div>

            <div className="event-detail-badges">
              {event.category && <CategoryBadge category={event.category} />}
              {event.status && <StatusBadge status={event.status} />}
            </div>

            <div className="event-detail-meta">
              <span>{format(new Date(event.startTime), 'EEEE, MMMM d, yyyy')}</span>
              <span>
                {format(new Date(event.startTime), 'h:mm a')}
                {' – '}
                {format(new Date(event.endTime), 'h:mm a')}
              </span>
              <span>{event.location}</span>
            </div>

            {event.description && (
              <div className="event-detail-description">{event.description}</div>
            )}

            <div className="event-detail-footer">
              <span className="event-detail-subscribers">
                {localSubscriberCount} {localSubscriberCount === 1 ? 'subscriber' : 'subscribers'}
              </span>
              <button
                className="event-detail-signup-btn"
                onClick={() => setShowSignupModal(true)}
                disabled={isCancelled}
              >
                Get SMS Alerts
              </button>
            </div>
          </>
        )}
      </div>

      <SmsSignupModal
        isOpen={showSignupModal}
        onClose={handleModalClose}
        event={event}
      />
    </div>
  )
}
