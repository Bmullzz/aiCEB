export default function EventCard({ event, onClick }) {
  return (
    <div data-testid="event-card" onClick={onClick} style={{ cursor: 'pointer' }}>
      {event.title}
    </div>
  )
}
