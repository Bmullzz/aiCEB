import { format } from 'date-fns'
import { CategoryBadge, StatusBadge } from '../shared'
import './EventCard.css'

export default function EventCard({ event, onClick }) {
  function handleKeyDown(e) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      onClick?.()
    }
  }

  return (
    <div
      data-testid="event-card"
      className="event-card"
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={handleKeyDown}
    >
      <div className="event-card-header">
        <div className="event-card-title">{event.title}</div>
        <div className="event-card-badges">
          {event.category && <CategoryBadge category={event.category} />}
          {event.status && <StatusBadge status={event.status} />}
        </div>
      </div>
      <div className="event-card-meta">
        {event.startTime && (
          <span className="event-card-time">
            {format(new Date(event.startTime), 'MMM d, yyyy · h:mm a')}
          </span>
        )}
        {event.location && <span className="event-card-location">{event.location}</span>}
      </div>
    </div>
  )
}
