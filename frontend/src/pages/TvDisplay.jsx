import { format } from 'date-fns'
import { useEffect, useState } from 'react'
import { getUpcomingEvents } from '../api/api'
import { EmptyState } from '../components/shared'
import './TvDisplay.css'

export default function TvDisplay() {
  const [events, setEvents] = useState([])
  const [now, setNow] = useState(new Date())

  useEffect(() => {
    const pollInterval = parseInt(import.meta.env.VITE_POLL_INTERVAL_MS) || 30000

    getUpcomingEvents(10).then(setEvents).catch(() => {})

    const eventPollId = setInterval(() => {
      getUpcomingEvents(10).then(setEvents).catch(() => {})
    }, pollInterval)

    const clockId = setInterval(() => setNow(new Date()), 60000)

    return () => {
      clearInterval(eventPollId)
      clearInterval(clockId)
    }
  }, [])

  const isUpcoming = (event) => {
    const start = new Date(event.startTime)
    return start > now && start - now <= 30 * 60 * 1000
  }

  return (
    <div data-testid="tv-display" className="tv-display">
      <div className="tv-header">
        <span className="tv-title">Upcoming Events</span>
        <span className="tv-clock">
          {format(now, 'EEEE, MMMM d · h:mm a')}
        </span>
      </div>

      {events.length === 0
        ? <EmptyState message="No upcoming events scheduled." />
        : (
          <div className="tv-event-list">
            {events.map((event) => (
              <div
                key={event.id}
                className={`tv-event-card${isUpcoming(event) ? ' tv-event-soon' : ''}`}
                data-testid="tv-event-card"
              >
                <div
                  className="tv-event-category-accent"
                  style={{ backgroundColor: event.category?.color ?? '#9CA3AF' }}
                />
                <div className="tv-event-body">
                  <div className="tv-event-title">{event.title}</div>
                  <div className="tv-event-meta">
                    {format(new Date(event.startTime), 'h:mm a')} · {event.location}
                  </div>
                </div>
                {isUpcoming(event) && (
                  <div className="tv-event-soon-badge">Starting soon</div>
                )}
              </div>
            ))}
          </div>
        )}
    </div>
  )
}
