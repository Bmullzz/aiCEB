import { act, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import * as api from '../api/api'
import TvDisplay from './TvDisplay'

vi.mock('../api/api')

const POLL_INTERVAL = parseInt(import.meta.env.VITE_POLL_INTERVAL_MS) || 30000

// Fixed reference times for deterministic "soon" tests
const FIXED_NOW = new Date('2026-04-30T14:00:00.000Z')
const startIn15Min = new Date(FIXED_NOW.getTime() + 15 * 60 * 1000).toISOString()
const startIn2Hours = new Date(FIXED_NOW.getTime() + 2 * 60 * 60 * 1000).toISOString()

function makeEvent(overrides = {}) {
  return {
    id: overrides.id ?? '1',
    title: overrides.title ?? 'Test Event',
    startTime: overrides.startTime ?? startIn2Hours,
    location: overrides.location ?? 'Room A',
    category: overrides.category ?? { color: '#5DCAA5' },
  }
}

function renderTv() {
  return render(<TvDisplay />)
}

beforeEach(() => {
  api.getUpcomingEvents.mockResolvedValue([])
})

afterEach(() => {
  vi.clearAllMocks()
  vi.useRealTimers()
})

test('renders data-testid="tv-display"', () => {
  renderTv()
  expect(screen.getByTestId('tv-display')).toBeInTheDocument()
})

test('renders EmptyState when events array is empty', async () => {
  renderTv()
  await waitFor(() => expect(screen.getByTestId('empty-state')).toBeInTheDocument())
})

test('renders event cards when getUpcomingEvents returns events', async () => {
  api.getUpcomingEvents.mockResolvedValue([
    makeEvent({ id: '1' }),
    makeEvent({ id: '2' }),
    makeEvent({ id: '3' }),
  ])
  renderTv()
  await waitFor(() => expect(screen.getAllByTestId('tv-event-card')).toHaveLength(3))
})

test('renders event title and location in each card', async () => {
  api.getUpcomingEvents.mockResolvedValue([
    makeEvent({ title: 'Workshop on AI', location: 'Lab 101' }),
  ])
  renderTv()
  await waitFor(() => screen.getByTestId('tv-event-card'))
  expect(screen.getByText('Workshop on AI')).toBeInTheDocument()
  expect(screen.getByText(/Lab 101/)).toBeInTheDocument()
})

describe('tv-event-soon class', () => {
  beforeEach(() => { vi.useFakeTimers(); vi.setSystemTime(FIXED_NOW) })

  test('applies tv-event-soon class to events starting within 30 minutes', async () => {
    api.getUpcomingEvents.mockResolvedValue([makeEvent({ startTime: startIn15Min })])
    renderTv()
    await act(async () => {})
    expect(screen.getByTestId('tv-event-card')).toHaveClass('tv-event-soon')
  })

  test('does NOT apply tv-event-soon class to events starting in 2 hours', async () => {
    api.getUpcomingEvents.mockResolvedValue([makeEvent({ startTime: startIn2Hours })])
    renderTv()
    await act(async () => {})
    expect(screen.getByTestId('tv-event-card')).not.toHaveClass('tv-event-soon')
  })

  test('renders "Starting soon" badge on soon events only', async () => {
    api.getUpcomingEvents.mockResolvedValue([
      makeEvent({ id: '1', startTime: startIn15Min }),
      makeEvent({ id: '2', startTime: startIn2Hours }),
    ])
    renderTv()
    await act(async () => {})
    expect(screen.getAllByText('Starting soon')).toHaveLength(1)
  })
})

test('clock displays formatted date text', () => {
  renderTv()
  const clock = screen.getByTestId('tv-display').querySelector('.tv-clock')
  expect(clock).not.toBeNull()
  expect(clock.textContent.length).toBeGreaterThan(0)
})

describe('polling', () => {
  beforeEach(() => { vi.useFakeTimers() })

  test('poll re-fetches after interval', async () => {
    renderTv()
    await act(async () => {})
    expect(api.getUpcomingEvents).toHaveBeenCalledTimes(1)
    await act(async () => { vi.advanceTimersByTime(POLL_INTERVAL + 1) })
    expect(api.getUpcomingEvents).toHaveBeenCalledTimes(2)
  })

  test('poll failure does not clear displayed events', async () => {
    api.getUpcomingEvents
      .mockResolvedValueOnce([makeEvent()])
      .mockRejectedValueOnce(new Error('Network error'))
    renderTv()
    await act(async () => {})
    expect(screen.getByTestId('tv-event-card')).toBeInTheDocument()
    await act(async () => { vi.advanceTimersByTime(POLL_INTERVAL + 1) })
    expect(screen.getByTestId('tv-event-card')).toBeInTheDocument()
  })

  test('both intervals are cleaned up on unmount', async () => {
    vi.useRealTimers()
    const spy = vi.spyOn(global, 'clearInterval')
    const { unmount } = renderTv()
    await waitFor(() => expect(api.getUpcomingEvents).toHaveBeenCalled())
    unmount()
    expect(spy.mock.calls.length).toBeGreaterThanOrEqual(2)
    spy.mockRestore()
  })
})

test('no interactive elements rendered', async () => {
  api.getUpcomingEvents.mockResolvedValue([makeEvent()])
  renderTv()
  await waitFor(() => screen.getByTestId('tv-event-card'))
  expect(screen.queryAllByRole('button')).toHaveLength(0)
  expect(screen.queryAllByRole('link')).toHaveLength(0)
})
