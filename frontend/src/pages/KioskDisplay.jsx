import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCategories, getEvents } from '../api/api'
import EventCard from '../components/kiosk/EventCard'
import { EmptyState, ErrorMessage, LoadingSpinner } from '../components/shared'
import './KioskDisplay.css'

export default function KioskDisplay() {
  const navigate = useNavigate()

  const [events, setEvents] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [categories, setCategories] = useState([])
  const [activeCategoryId, setActiveCategoryId] = useState(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [initialLoading, setInitialLoading] = useState(true)
  const [initialError, setInitialError] = useState(null)

  // Tracks whether the first response has been received.
  // Using a ref avoids adding it to useCallback deps, which would cause
  // the polling interval to restart on every load completion.
  const isInitialRef = useRef(true)

  const fetchEvents = useCallback(() => {
    const treating_as_initial = isInitialRef.current
    getEvents({ page: currentPage, categoryId: activeCategoryId })
      .then((data) => {
        setEvents(data.content)
        setTotalPages(data.totalPages)
        setTotalElements(data.totalElements)
        if (treating_as_initial) {
          isInitialRef.current = false
          setInitialLoading(false)
        }
      })
      .catch((err) => {
        if (treating_as_initial) {
          isInitialRef.current = false
          setInitialError(err)
          setInitialLoading(false)
        }
        // Polling failures are silent — keep showing existing events
      })
  }, [activeCategoryId, currentPage])

  // Fetch categories once on mount — failure is always silent
  useEffect(() => {
    getCategories().then(setCategories).catch(() => {})
  }, [])

  // Fetch events immediately and on every poll interval.
  // Restarts whenever the active filter or page changes.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    fetchEvents()
    const pollInterval = parseInt(import.meta.env.VITE_POLL_INTERVAL_MS) || 30000
    const id = setInterval(fetchEvents, pollInterval)
    return () => clearInterval(id)
  }, [activeCategoryId, currentPage])

  const handleRetry = () => {
    isInitialRef.current = true
    setInitialLoading(true)
    setInitialError(null)
    fetchEvents()
  }

  if (initialLoading) return <LoadingSpinner size="lg" />
  if (initialError) return <ErrorMessage error={initialError} onRetry={handleRetry} />

  return (
    <div data-testid="kiosk-display" className="kiosk-display">
      <div className="kiosk-filters">
        <button
          className={activeCategoryId === null ? 'filter-btn active' : 'filter-btn'}
          onClick={() => { setActiveCategoryId(null); setCurrentPage(0) }}
        >
          All
        </button>
        {categories.map((cat) => (
          <button
            key={cat.id}
            className={activeCategoryId === cat.id ? 'filter-btn active' : 'filter-btn'}
            style={{ borderColor: cat.color }}
            onClick={() => { setActiveCategoryId(cat.id); setCurrentPage(0) }}
          >
            {cat.name}
          </button>
        ))}
      </div>

      {totalElements === 0
        ? <EmptyState message="No upcoming events at this time." />
        : (
          <div className="kiosk-event-list">
            {events.map((event) => (
              <EventCard
                key={event.id}
                event={event}
                onClick={() => navigate(`/events/${event.id}`)}
              />
            ))}
          </div>
        )}

      {totalPages > 1 && (
        <div className="kiosk-pagination">
          <button
            disabled={currentPage === 0}
            onClick={() => setCurrentPage((p) => p - 1)}
          >
            Previous
          </button>
          <span>{currentPage + 1} / {totalPages}</span>
          <button
            disabled={currentPage >= totalPages - 1}
            onClick={() => setCurrentPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
