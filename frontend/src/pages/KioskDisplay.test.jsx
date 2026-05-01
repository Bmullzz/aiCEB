import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import * as api from '../api/api'
import KioskDisplay from './KioskDisplay'

vi.mock('../api/api')

const POLL_INTERVAL = parseInt(import.meta.env.VITE_POLL_INTERVAL_MS) || 30000

const emptyPage = { content: [], totalPages: 0, totalElements: 0 }
const singlePage = (events) => ({ content: events, totalPages: 1, totalElements: events.length })
const multiPage = (events, totalPages) => ({
  content: events,
  totalPages,
  totalElements: events.length * totalPages,
})

function renderKiosk() {
  return render(
    <MemoryRouter>
      <KioskDisplay />
    </MemoryRouter>
  )
}

beforeEach(() => {
  api.getEvents.mockResolvedValue(emptyPage)
  api.getCategories.mockResolvedValue([])
})

afterEach(() => {
  vi.clearAllMocks()
  vi.useRealTimers()
})

test('renders LoadingSpinner during initial load', () => {
  api.getEvents.mockReturnValue(new Promise(() => {})) // never resolves
  renderKiosk()
  expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
})

test('renders event list after successful load', async () => {
  const events = [{ id: '1', title: 'Event One' }, { id: '2', title: 'Event Two' }]
  api.getEvents.mockResolvedValue(singlePage(events))
  renderKiosk()
  await waitFor(() => expect(screen.getAllByTestId('event-card')).toHaveLength(2))
})

test('renders EmptyState when totalElements is 0', async () => {
  renderKiosk()
  await waitFor(() => expect(screen.getByTestId('empty-state')).toBeInTheDocument())
})

test('renders ErrorMessage when initial load fails', async () => {
  api.getEvents.mockRejectedValue({ message: 'Network error', code: 'UNKNOWN', status: 500 })
  renderKiosk()
  await waitFor(() => expect(screen.getByTestId('error-message')).toBeInTheDocument())
})

test('renders category filter buttons from getCategories response', async () => {
  api.getCategories.mockResolvedValue([
    { id: 'c1', name: 'Workshop', color: '#5DCAA5' },
    { id: 'c2', name: 'Lecture', color: '#7F77DD' },
  ])
  renderKiosk()
  await waitFor(() => expect(screen.getByRole('button', { name: 'Workshop' })).toBeInTheDocument())
  expect(screen.getByRole('button', { name: 'Lecture' })).toBeInTheDocument()
})

test('clicking a category filter button updates the active filter', async () => {
  api.getCategories.mockResolvedValue([{ id: 'c1', name: 'Workshop', color: '#5DCAA5' }])
  renderKiosk()
  await waitFor(() => screen.getByRole('button', { name: 'Workshop' }))
  await userEvent.click(screen.getByRole('button', { name: 'Workshop' }))
  expect(screen.getByRole('button', { name: 'Workshop' })).toHaveClass('active')
})

test('clicking a category filter resets page to 0', async () => {
  api.getCategories.mockResolvedValue([{ id: 'c1', name: 'Workshop', color: '#5DCAA5' }])
  api.getEvents.mockResolvedValue(multiPage([{ id: '1', title: 'E1' }], 3))
  renderKiosk()
  await waitFor(() => screen.getByRole('button', { name: 'Next' }))
  await userEvent.click(screen.getByRole('button', { name: 'Next' }))
  await waitFor(() => expect(screen.getByText('2 / 3')).toBeInTheDocument())
  await userEvent.click(screen.getByRole('button', { name: 'Workshop' }))
  await waitFor(() => expect(screen.getByText('1 / 3')).toBeInTheDocument())
})

test('pagination Next button increments page', async () => {
  api.getEvents.mockResolvedValue(multiPage([{ id: '1', title: 'E1' }], 3))
  renderKiosk()
  await waitFor(() => screen.getByRole('button', { name: 'Next' }))
  await userEvent.click(screen.getByRole('button', { name: 'Next' }))
  await waitFor(() => expect(screen.getByText('2 / 3')).toBeInTheDocument())
})

test('pagination Previous button is disabled on page 0', async () => {
  api.getEvents.mockResolvedValue(multiPage([{ id: '1', title: 'E1' }], 3))
  renderKiosk()
  await waitFor(() => screen.getByRole('button', { name: 'Previous' }))
  expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
})

test('pagination controls are hidden when totalPages is 1', async () => {
  api.getEvents.mockResolvedValue(singlePage([{ id: '1', title: 'E1' }]))
  renderKiosk()
  await waitFor(() => expect(screen.getByTestId('event-card')).toBeInTheDocument())
  expect(screen.queryByRole('button', { name: 'Next' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Previous' })).not.toBeInTheDocument()
})

describe('polling', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  test('polling re-fetches after interval', async () => {
    renderKiosk()
    await act(async () => {})
    expect(api.getEvents).toHaveBeenCalledTimes(1)
    await act(async () => { vi.advanceTimersByTime(POLL_INTERVAL + 1) })
    expect(api.getEvents).toHaveBeenCalledTimes(2)
  })

  test('poll failure does not clear the displayed events', async () => {
    const events = [{ id: '1', title: 'Existing Event' }]
    api.getEvents
      .mockResolvedValueOnce(singlePage(events))
      .mockRejectedValueOnce({ message: 'Timeout', code: 'UNKNOWN', status: 500 })
    renderKiosk()
    await act(async () => {})
    expect(screen.getByTestId('event-card')).toBeInTheDocument()
    await act(async () => { vi.advanceTimersByTime(POLL_INTERVAL + 1) })
    expect(screen.getByTestId('event-card')).toBeInTheDocument()
    expect(screen.queryByTestId('error-message')).not.toBeInTheDocument()
  })

  test('clearInterval is called when component unmounts', async () => {
    vi.useRealTimers()
    const spy = vi.spyOn(global, 'clearInterval')
    renderKiosk()
    const { unmount } = renderKiosk()
    await waitFor(() => expect(api.getEvents).toHaveBeenCalled())
    unmount()
    expect(spy).toHaveBeenCalled()
    spy.mockRestore()
  })
})
