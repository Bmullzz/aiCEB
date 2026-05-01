import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import * as api from '../../api/api'
import SmsSignupModal, { computeReminderTimes, formatAlertOffsets } from './SmsSignupModal'

vi.mock('../../api/api')

const testEvent = {
  id: 'event-uuid',
  title: 'Test Workshop',
  alertOffsets: [60, 15],
  startTime: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
}

function renderModal(props = {}) {
  const defaults = {
    isOpen: true,
    onClose: vi.fn(),
    event: testEvent,
  }
  return render(<SmsSignupModal {...defaults} {...props} />)
}

// ─── formatAlertOffsets ───────────────────────────────────────────────────────

describe('formatAlertOffsets', () => {
  test('[60, 15] → "60 and 15 minutes before this event"', () => {
    expect(formatAlertOffsets([60, 15])).toBe('60 and 15 minutes before this event')
  })

  test('[60] → "60 minutes before this event"', () => {
    expect(formatAlertOffsets([60])).toBe('60 minutes before this event')
  })

  test('[] → "before this event starts"', () => {
    expect(formatAlertOffsets([])).toBe('before this event starts')
  })

  test('null → "before this event starts"', () => {
    expect(formatAlertOffsets(null)).toBe('before this event starts')
  })
})

// ─── computeReminderTimes ─────────────────────────────────────────────────────

describe('computeReminderTimes', () => {
  test('returns array of time strings for each offset', () => {
    const event = {
      startTime: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
      alertOffsets: [60, 15],
    }
    const times = computeReminderTimes(event)
    expect(times).toHaveLength(2)
    // Each entry should look like h:mm a (e.g. "1:00 PM")
    times.forEach((t) => expect(t).toMatch(/^\d{1,2}:\d{2} (AM|PM)$/))
  })

  test('returns empty array when event has no alertOffsets', () => {
    expect(computeReminderTimes({ startTime: new Date().toISOString() })).toEqual([])
  })

  test('returns empty array when event is null', () => {
    expect(computeReminderTimes(null)).toEqual([])
  })
})

// ─── Visibility ───────────────────────────────────────────────────────────────

test('renders nothing when isOpen is false', () => {
  renderModal({ isOpen: false })
  expect(screen.queryByTestId('sms-modal-form')).not.toBeInTheDocument()
  expect(screen.queryByTestId('modal-overlay')).not.toBeInTheDocument()
})

test('renders FORM mode when isOpen is true', () => {
  renderModal()
  expect(screen.getByTestId('sms-modal-form')).toBeInTheDocument()
  expect(screen.getByTestId('phone-input')).toBeInTheDocument()
  expect(screen.getByTestId('submit-button')).toBeInTheDocument()
  expect(screen.getByText('Test Workshop')).toBeInTheDocument()
  expect(screen.getByText(/60 and 15 minutes before this event/)).toBeInTheDocument()
})

// ─── Phone input ──────────────────────────────────────────────────────────────

test('submit button is disabled when phone input is empty', () => {
  renderModal()
  expect(screen.getByTestId('submit-button')).toBeDisabled()
})

test('submit button is enabled when phone input has content', async () => {
  renderModal()
  await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
  expect(screen.getByTestId('submit-button')).not.toBeDisabled()
})

test('typing in phone input clears inline error', async () => {
  api.subscribe.mockRejectedValueOnce({ code: 'INVALID_PHONE_NUMBER', message: 'Invalid' })
  renderModal()
  await userEvent.type(screen.getByTestId('phone-input'), '123')
  fireEvent.click(screen.getByTestId('submit-button'))
  await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
  await userEvent.type(screen.getByTestId('phone-input'), '4')
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
})

// ─── Auto-focus ───────────────────────────────────────────────────────────────

test('phone input receives focus when modal opens', async () => {
  vi.useFakeTimers()
  renderModal()
  await act(async () => { vi.advanceTimersByTime(100) })
  expect(document.activeElement).toBe(screen.getByTestId('phone-input'))
  vi.useRealTimers()
})

// ─── LOADING mode ─────────────────────────────────────────────────────────────

test('shows LoadingSpinner while submit is in-flight', async () => {
  api.subscribe.mockReturnValue(new Promise(() => {}))
  renderModal()
  await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
  fireEvent.click(screen.getByTestId('submit-button'))
  expect(screen.getByTestId('sms-modal-loading')).toBeInTheDocument()
  expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
  expect(screen.queryByTestId('phone-input')).not.toBeInTheDocument()
  expect(screen.queryByTestId('submit-button')).not.toBeInTheDocument()
})

// ─── SUCCESS mode ─────────────────────────────────────────────────────────────

describe('SUCCESS mode', () => {
  beforeEach(() => {
    api.subscribe.mockResolvedValue({})
  })

  async function enterSuccess() {
    renderModal()
    await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
    fireEvent.click(screen.getByTestId('submit-button'))
    await waitFor(() => expect(screen.getByTestId('sms-modal-success')).toBeInTheDocument())
  }

  test('renders success heading and Done button', async () => {
    await enterSuccess()
    expect(screen.getByText(/You're signed up!/)).toBeInTheDocument()
    expect(screen.getByTestId('done-button')).toBeInTheDocument()
  })

  test('renders countdown text', async () => {
    await enterSuccess()
    expect(screen.getByTestId('countdown')).toHaveTextContent(/Closing in 15s/)
  })

  test('Done button calls onClose(true)', async () => {
    const onClose = vi.fn()
    render(<SmsSignupModal isOpen={true} onClose={onClose} event={testEvent} />)
    await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
    fireEvent.click(screen.getByTestId('submit-button'))
    await waitFor(() => screen.getByTestId('done-button'))
    fireEvent.click(screen.getByTestId('done-button'))
    expect(onClose).toHaveBeenCalledWith(true)
  })

  test('auto-dismisses after 15 seconds', async () => {
    vi.useFakeTimers()
    const onClose = vi.fn()
    render(<SmsSignupModal isOpen={true} onClose={onClose} event={testEvent} />)
    fireEvent.change(screen.getByTestId('phone-input'), { target: { value: '2155550123' } })
    fireEvent.click(screen.getByTestId('submit-button'))
    await act(async () => {})
    expect(screen.getByTestId('sms-modal-success')).toBeInTheDocument()
    await act(async () => { vi.advanceTimersByTime(15000) })
    expect(onClose).toHaveBeenCalledWith(true)
    vi.useRealTimers()
  })

  test('countdown resets to 15 on tap', async () => {
    vi.useFakeTimers()
    const onClose = vi.fn()
    render(<SmsSignupModal isOpen={true} onClose={onClose} event={testEvent} />)
    fireEvent.change(screen.getByTestId('phone-input'), { target: { value: '2155550123' } })
    fireEvent.click(screen.getByTestId('submit-button'))
    await act(async () => {})
    expect(screen.getByTestId('sms-modal-success')).toBeInTheDocument()

    // Advance 10s (countdown now at 5)
    await act(async () => { vi.advanceTimersByTime(10000) })
    expect(onClose).not.toHaveBeenCalled()

    // Tap the success wrapper to reset countdown to 15
    fireEvent.click(screen.getByTestId('sms-modal-success'))

    // Advance another 10s (should NOT dismiss — countdown reset to 15, now at 5)
    await act(async () => { vi.advanceTimersByTime(10000) })
    expect(onClose).not.toHaveBeenCalled()

    // Advance 5 more seconds to trigger dismiss
    await act(async () => { vi.advanceTimersByTime(5000) })
    expect(onClose).toHaveBeenCalledWith(true)
    vi.useRealTimers()
  })
})

// ─── ALREADY_SUBSCRIBED mode ──────────────────────────────────────────────────

describe('ALREADY_SUBSCRIBED mode', () => {
  beforeEach(() => {
    api.subscribe.mockRejectedValue({ code: 'ALREADY_SUBSCRIBED', message: 'Already subscribed' })
  })

  async function enterAlreadySubscribed() {
    renderModal()
    await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
    fireEvent.click(screen.getByTestId('submit-button'))
    await waitFor(() => expect(screen.getByTestId('sms-modal-already-subscribed')).toBeInTheDocument())
  }

  test('renders already-subscribed heading', async () => {
    await enterAlreadySubscribed()
    expect(screen.getByText(/You're already signed up!/)).toBeInTheDocument()
  })

  test('uses .sms-modal-info class, not .sms-modal-error', async () => {
    await enterAlreadySubscribed()
    expect(screen.getByTestId('sms-modal-already-subscribed')).toHaveClass('sms-modal-info')
    expect(screen.queryByTestId('sms-modal-error')).not.toBeInTheDocument()
  })

  test('renders Done and Unsubscribe buttons', async () => {
    await enterAlreadySubscribed()
    expect(screen.getByTestId('done-button')).toBeInTheDocument()
    expect(screen.getByTestId('unsubscribe-button')).toBeInTheDocument()
  })

  test('Unsubscribe button calls optOut then onClose(false)', async () => {
    api.optOut.mockResolvedValue({})
    const onClose = vi.fn()
    render(<SmsSignupModal isOpen={true} onClose={onClose} event={testEvent} />)
    await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
    fireEvent.click(screen.getByTestId('submit-button'))
    await waitFor(() => screen.getByTestId('unsubscribe-button'))
    fireEvent.click(screen.getByTestId('unsubscribe-button'))
    await waitFor(() => expect(onClose).toHaveBeenCalledWith(false))
    expect(api.optOut).toHaveBeenCalled()
  })

  test('Done button calls onClose(false)', async () => {
    const onClose = vi.fn()
    render(<SmsSignupModal isOpen={true} onClose={onClose} event={testEvent} />)
    await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
    fireEvent.click(screen.getByTestId('submit-button'))
    await waitFor(() => screen.getByTestId('done-button'))
    fireEvent.click(screen.getByTestId('done-button'))
    expect(onClose).toHaveBeenCalledWith(false)
  })
})

// ─── INVALID_PHONE_NUMBER inline error ────────────────────────────────────────

describe('INVALID_PHONE_NUMBER inline error', () => {
  beforeEach(() => {
    api.subscribe.mockRejectedValue({ code: 'INVALID_PHONE_NUMBER', message: 'Invalid number' })
  })

  async function triggerInlineError() {
    renderModal()
    await userEvent.type(screen.getByTestId('phone-input'), 'abc')
    fireEvent.click(screen.getByTestId('submit-button'))
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
  }

  test('stays in FORM mode and shows inline error', async () => {
    await triggerInlineError()
    expect(screen.getByTestId('sms-modal-form')).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent(/valid phone number/)
  })

  test('input is re-focused after inline error', async () => {
    await triggerInlineError()
    expect(document.activeElement).toBe(screen.getByTestId('phone-input'))
  })
})

// ─── ERROR mode ───────────────────────────────────────────────────────────────

describe('ERROR mode', () => {
  beforeEach(() => {
    api.subscribe.mockRejectedValue({ code: 'SERVER_ERROR', message: 'Something broke' })
  })

  async function enterError() {
    renderModal()
    await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
    fireEvent.click(screen.getByTestId('submit-button'))
    await waitFor(() => expect(screen.getByTestId('sms-modal-error')).toBeInTheDocument())
  }

  test('renders error heading and API message', async () => {
    await enterError()
    expect(screen.getByText(/Something went wrong/)).toBeInTheDocument()
    expect(screen.getByText('Something broke')).toBeInTheDocument()
  })

  test('renders Try again and Close buttons', async () => {
    await enterError()
    expect(screen.getByTestId('try-again-button')).toBeInTheDocument()
    expect(screen.getByTestId('close-button')).toBeInTheDocument()
  })

  test('Try again resets mode to FORM', async () => {
    await enterError()
    fireEvent.click(screen.getByTestId('try-again-button'))
    expect(screen.getByTestId('sms-modal-form')).toBeInTheDocument()
    expect(screen.getByTestId('phone-input')).toBeInTheDocument()
  })

  test('Close button calls onClose(false)', async () => {
    const onClose = vi.fn()
    render(<SmsSignupModal isOpen={true} onClose={onClose} event={testEvent} />)
    await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
    fireEvent.click(screen.getByTestId('submit-button'))
    await waitFor(() => screen.getByTestId('close-button'))
    fireEvent.click(screen.getByTestId('close-button'))
    expect(onClose).toHaveBeenCalledWith(false)
  })
})

// ─── Reset on re-open ─────────────────────────────────────────────────────────

test('resets to FORM when re-opened after SUCCESS', async () => {
  api.subscribe.mockResolvedValue({})
  const onClose = vi.fn()
  const { rerender } = render(
    <SmsSignupModal isOpen={true} onClose={onClose} event={testEvent} />
  )
  await userEvent.type(screen.getByTestId('phone-input'), '2155550123')
  fireEvent.click(screen.getByTestId('submit-button'))
  await waitFor(() => screen.getByTestId('sms-modal-success'))

  // Close the modal
  rerender(<SmsSignupModal isOpen={false} onClose={onClose} event={testEvent} />)

  // Re-open
  rerender(<SmsSignupModal isOpen={true} onClose={onClose} event={testEvent} />)
  expect(screen.getByTestId('sms-modal-form')).toBeInTheDocument()
  expect(screen.getByTestId('phone-input')).toHaveValue('')
})

// ─── Existing EventDetailScreen stub compat ───────────────────────────────────

test('modal overlay closes by calling onClose(false)', () => {
  const onClose = vi.fn()
  renderModal({ onClose })
  fireEvent.click(screen.getByTestId('modal-overlay'))
  expect(onClose).toHaveBeenCalledWith(false)
})
