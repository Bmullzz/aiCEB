import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, test, vi } from 'vitest'
import EventCard from './EventCard'

function makeEvent(overrides = {}) {
  return {
    id: overrides.id ?? '1',
    title: overrides.title ?? 'Test Event',
    startTime: overrides.startTime ?? '2026-05-01T14:00:00Z',
    location: overrides.location ?? 'Room A',
    status: overrides.status ?? 'UPCOMING',
    category: overrides.category ?? { id: 'c1', name: 'Workshop', color: '#5DCAA5' },
    ...overrides,
  }
}

test('renders data-testid="event-card"', () => {
  render(<EventCard event={makeEvent()} onClick={() => {}} />)
  expect(screen.getByTestId('event-card')).toBeInTheDocument()
})

test('renders event title', () => {
  render(<EventCard event={makeEvent({ title: 'Annual Summit' })} onClick={() => {}} />)
  expect(screen.getByText('Annual Summit')).toBeInTheDocument()
})

test('renders event location', () => {
  render(<EventCard event={makeEvent({ location: 'Lab 102' })} onClick={() => {}} />)
  expect(screen.getByText(/Lab 102/)).toBeInTheDocument()
})

test('renders formatted start time', () => {
  render(<EventCard event={makeEvent({ startTime: '2026-05-01T14:00:00Z' })} onClick={() => {}} />)
  // date-fns formats in local time — check that something time-like is rendered
  const card = screen.getByTestId('event-card')
  expect(card.textContent).toMatch(/May 1, 2026/)
})

test('renders category badge', () => {
  render(<EventCard event={makeEvent()} onClick={() => {}} />)
  expect(screen.getByTestId('category-badge')).toBeInTheDocument()
  expect(screen.getByText('Workshop')).toBeInTheDocument()
})

test('renders status badge', () => {
  render(<EventCard event={makeEvent({ status: 'UPCOMING' })} onClick={() => {}} />)
  expect(screen.getByTestId('status-badge')).toBeInTheDocument()
  expect(screen.getByText('UPCOMING')).toBeInTheDocument()
})

test('calls onClick when clicked', async () => {
  const onClick = vi.fn()
  render(<EventCard event={makeEvent()} onClick={onClick} />)
  await userEvent.click(screen.getByTestId('event-card'))
  expect(onClick).toHaveBeenCalledTimes(1)
})

describe('keyboard interaction', () => {
  test('calls onClick on Enter key', async () => {
    const onClick = vi.fn()
    render(<EventCard event={makeEvent()} onClick={onClick} />)
    screen.getByTestId('event-card').focus()
    await userEvent.keyboard('{Enter}')
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  test('calls onClick on Space key', async () => {
    const onClick = vi.fn()
    render(<EventCard event={makeEvent()} onClick={onClick} />)
    screen.getByTestId('event-card').focus()
    await userEvent.keyboard(' ')
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  test('has role="button" and tabIndex=0', () => {
    render(<EventCard event={makeEvent()} onClick={() => {}} />)
    const card = screen.getByRole('button')
    expect(card).toBeInTheDocument()
    expect(card).toHaveAttribute('tabindex', '0')
  })
})

test('does not render category badge when category is absent', () => {
  const event = { id: '1', title: 'No Cat', startTime: '2026-05-01T14:00:00Z', location: 'Somewhere', status: 'UPCOMING' }
  render(<EventCard event={event} onClick={() => {}} />)
  expect(screen.queryByTestId('category-badge')).not.toBeInTheDocument()
})
