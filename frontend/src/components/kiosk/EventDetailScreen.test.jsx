import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, test, vi } from 'vitest'
import * as api from '../../api/api'
import EventDetailScreen from './EventDetailScreen'

vi.mock('../../api/api')

function makeEvent(overrides = {}) {
  return {
    id: overrides.id ?? 'abc-123',
    title: overrides.title ?? 'Test Event',
    description: overrides.description ?? 'A description.',
    location: overrides.location ?? 'Room A',
    startTime: overrides.startTime ?? '2026-05-01T14:00:00Z',
    endTime: overrides.endTime ?? '2026-05-01T15:30:00Z',
    category: overrides.category ?? { id: 'c1', name: 'Workshop', color: '#5DCAA5' },
    status: overrides.status ?? 'UPCOMING',
    subscriberCount: overrides.subscriberCount ?? 5,
    ...overrides,
  }
}

function renderDetail(id = 'abc-123') {
  return render(
    <MemoryRouter initialEntries={[`/events/${id}`]}>
      <Routes>
        <Route path="/events/:id" element={<EventDetailScreen />} />
      </Routes>
    </MemoryRouter>
  )
}

beforeEach(() => {
  api.getEvent.mockResolvedValue(makeEvent())
})

test('renders data-testid="event-detail-screen"', async () => {
  renderDetail()
  expect(screen.getByTestId('event-detail-screen')).toBeInTheDocument()
})

test('renders LoadingSpinner during initial fetch', () => {
  api.getEvent.mockReturnValue(new Promise(() => {}))
  renderDetail()
  expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
})

test('renders ErrorMessage when fetch fails', async () => {
  api.getEvent.mockRejectedValue({ message: 'Not found', code: 'EVENT_NOT_FOUND', status: 404 })
  renderDetail()
  await waitFor(() => expect(screen.getByTestId('error-message')).toBeInTheDocument())
})

test('renders event title after load', async () => {
  api.getEvent.mockResolvedValue(makeEvent({ title: 'Spring Workshop' }))
  renderDetail()
  await waitFor(() => expect(screen.getByText('Spring Workshop')).toBeInTheDocument())
})

test('renders event location after load', async () => {
  api.getEvent.mockResolvedValue(makeEvent({ location: 'Lab 202' }))
  renderDetail()
  await waitFor(() => expect(screen.getByText(/Lab 202/)).toBeInTheDocument())
})

test('renders event description when present', async () => {
  api.getEvent.mockResolvedValue(makeEvent({ description: 'Join us for a workshop.' }))
  renderDetail()
  await waitFor(() => expect(screen.getByText('Join us for a workshop.')).toBeInTheDocument())
})

test('renders subscriber count', async () => {
  api.getEvent.mockResolvedValue(makeEvent({ subscriberCount: 7 }))
  renderDetail()
  await waitFor(() => expect(screen.getByText(/7 subscribers/)).toBeInTheDocument())
})

test('renders category badge', async () => {
  renderDetail()
  await waitFor(() => expect(screen.getByTestId('category-badge')).toBeInTheDocument())
})

test('renders status badge', async () => {
  renderDetail()
  await waitFor(() => expect(screen.getByTestId('status-badge')).toBeInTheDocument())
})

test('renders "Get SMS Alerts" button', async () => {
  renderDetail()
  await waitFor(() => expect(screen.getByRole('button', { name: /Get SMS Alerts/i })).toBeInTheDocument())
})

test('Get SMS Alerts button is disabled for CANCELLED events', async () => {
  api.getEvent.mockResolvedValue(makeEvent({ status: 'CANCELLED' }))
  renderDetail()
  await waitFor(() => expect(screen.getByRole('button', { name: /Get SMS Alerts/i })).toBeDisabled())
})

test('opens SmsSignupModal when Get SMS Alerts is clicked', async () => {
  renderDetail()
  await waitFor(() => screen.getByRole('button', { name: /Get SMS Alerts/i }))
  await userEvent.click(screen.getByRole('button', { name: /Get SMS Alerts/i }))
  expect(screen.getByTestId('sms-modal-form')).toBeInTheDocument()
})

describe('subscriber count local update', () => {
  test('subscriber count starts at event.subscriberCount', async () => {
    api.getEvent.mockResolvedValue(makeEvent({ subscriberCount: 3 }))
    renderDetail()
    await waitFor(() => expect(screen.getByText(/3 subscribers/)).toBeInTheDocument())
  })
})

test('close button is present', async () => {
  renderDetail()
  await waitFor(() => screen.getByRole('button', { name: /Close/i }))
  expect(screen.getByRole('button', { name: /Close/i })).toBeInTheDocument()
})

test('calls getEvent with the route param id', async () => {
  renderDetail('my-event-id')
  await waitFor(() => expect(api.getEvent).toHaveBeenCalledWith('my-event-id'))
})
