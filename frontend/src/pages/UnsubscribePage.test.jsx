import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, expect, test, vi } from 'vitest'
import * as api from '../api/api'
import UnsubscribePage from './UnsubscribePage'

vi.mock('../api/api')

function renderWithParams(params = {}) {
  const search = new URLSearchParams(params).toString()
  return render(
    <MemoryRouter initialEntries={[`/unsubscribe?${search}`]}>
      <Routes>
        <Route path="/unsubscribe" element={<UnsubscribePage />} />
      </Routes>
    </MemoryRouter>
  )
}

const VALID_PARAMS = { subscriptionId: 'sub-123', phoneNumber: '+12155550123' }

beforeEach(() => {
  vi.clearAllMocks()
})

test('renders LoadingSpinner initially before optOut resolves', () => {
  api.optOut.mockReturnValue(new Promise(() => {}))
  renderWithParams(VALID_PARAMS)
  expect(screen.getByTestId('unsubscribe-loading')).toBeInTheDocument()
  expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
})

test('renders SUCCESS state after successful optOut', async () => {
  api.optOut.mockResolvedValue({})
  renderWithParams(VALID_PARAMS)
  await waitFor(() => expect(screen.getByTestId('unsubscribe-success')).toBeInTheDocument())
  expect(screen.getByText(/You have been unsubscribed/)).toBeInTheDocument()
})

test('renders SUCCESS state when API returns ALREADY_OPTED_OUT', async () => {
  api.optOut.mockRejectedValue({ code: 'ALREADY_OPTED_OUT' })
  renderWithParams(VALID_PARAMS)
  await waitFor(() => expect(screen.getByTestId('unsubscribe-success')).toBeInTheDocument())
})

test('renders NOT_FOUND state when API returns SUBSCRIPTION_NOT_FOUND', async () => {
  api.optOut.mockRejectedValue({ code: 'SUBSCRIPTION_NOT_FOUND' })
  renderWithParams(VALID_PARAMS)
  await waitFor(() => expect(screen.getByTestId('unsubscribe-not-found')).toBeInTheDocument())
  expect(screen.getByText(/We couldn't find that subscription/)).toBeInTheDocument()
})

test('renders NOT_FOUND state when subscriptionId param is missing', async () => {
  renderWithParams({ phoneNumber: '+12155550123' })
  await waitFor(() => expect(screen.getByTestId('unsubscribe-not-found')).toBeInTheDocument())
  expect(api.optOut).not.toHaveBeenCalled()
})

test('renders NOT_FOUND state when phoneNumber param is missing', async () => {
  renderWithParams({ subscriptionId: 'sub-123' })
  await waitFor(() => expect(screen.getByTestId('unsubscribe-not-found')).toBeInTheDocument())
  expect(api.optOut).not.toHaveBeenCalled()
})

test('renders ERROR state for unexpected API errors', async () => {
  api.optOut.mockRejectedValue({ code: 'SERVER_ERROR', message: 'Internal error' })
  renderWithParams(VALID_PARAMS)
  await waitFor(() => expect(screen.getByTestId('unsubscribe-error')).toBeInTheDocument())
  expect(screen.getByText('Internal error')).toBeInTheDocument()
})

test('calls optOut with correct subscriptionId and phoneNumber from query params', async () => {
  api.optOut.mockResolvedValue({})
  renderWithParams({ subscriptionId: 'sub-123', phoneNumber: '+12155550123' })
  await waitFor(() => expect(api.optOut).toHaveBeenCalledWith({
    subscriptionId: 'sub-123',
    phoneNumber: '+12155550123',
  }))
})

test('calls optOut exactly once on mount', async () => {
  api.optOut.mockResolvedValue({})
  const { rerender } = renderWithParams(VALID_PARAMS)
  await waitFor(() => screen.getByTestId('unsubscribe-success'))
  rerender(
    <MemoryRouter initialEntries={[`/unsubscribe?${new URLSearchParams(VALID_PARAMS)}`]}>
      <Routes>
        <Route path="/unsubscribe" element={<UnsubscribePage />} />
      </Routes>
    </MemoryRouter>
  )
  expect(api.optOut).toHaveBeenCalledTimes(1)
})

test('no action buttons rendered in SUCCESS state', async () => {
  api.optOut.mockResolvedValue({})
  renderWithParams(VALID_PARAMS)
  await waitFor(() => screen.getByTestId('unsubscribe-success'))
  expect(screen.queryAllByRole('button')).toHaveLength(0)
})

test('no action buttons rendered in NOT_FOUND state', async () => {
  api.optOut.mockRejectedValue({ code: 'SUBSCRIPTION_NOT_FOUND' })
  renderWithParams(VALID_PARAMS)
  await waitFor(() => screen.getByTestId('unsubscribe-not-found'))
  expect(screen.queryAllByRole('button')).toHaveLength(0)
})

test('retry hint text is present in ERROR state', async () => {
  api.optOut.mockRejectedValue({ code: 'SERVER_ERROR', message: 'Failed' })
  renderWithParams(VALID_PARAMS)
  await waitFor(() => screen.getByTestId('unsubscribe-error'))
  expect(screen.getByText(/try tapping the unsubscribe link/i)).toBeInTheDocument()
})
