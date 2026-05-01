import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, vi } from 'vitest'
import App from './App'

vi.mock('./api/api')

import * as api from './api/api'

beforeEach(() => {
  api.getEvents.mockResolvedValue({ content: [], totalPages: 0, totalElements: 0 })
  api.getCategories.mockResolvedValue([])
  api.getUpcomingEvents.mockResolvedValue([])
  api.getEvent.mockResolvedValue({ id: '1', title: 'Test', startTime: '2026-05-01T14:00:00Z', endTime: '2026-05-01T15:00:00Z', location: 'Room A', status: 'UPCOMING', subscriberCount: 0 })
})

function renderApp(initialEntries = ['/']) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <App />
    </MemoryRouter>
  )
}

test('app renders without crashing', () => {
  renderApp()
})

test('/ renders KioskDisplay placeholder', async () => {
  renderApp(['/'])
  await waitFor(() => expect(screen.getByTestId('kiosk-display')).toBeInTheDocument())
})

test('/tv renders TvDisplay placeholder', () => {
  renderApp(['/tv'])
  expect(screen.getByTestId('tv-display')).toBeInTheDocument()
})

test('/unsubscribe renders UnsubscribePage placeholder', () => {
  renderApp(['/unsubscribe'])
  expect(screen.getByTestId('unsubscribe-page')).toBeInTheDocument()
})

test('/admin/login renders AdminLogin placeholder', () => {
  renderApp(['/admin/login'])
  expect(screen.getByTestId('admin-login')).toBeInTheDocument()
})

test('/admin/events redirects to /admin/login when no token is present', () => {
  renderApp(['/admin/events'])
  expect(screen.getByTestId('admin-login')).toBeInTheDocument()
})
