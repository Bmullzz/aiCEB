import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach } from 'vitest'
import Toast from './Toast'

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

test('renders message text', () => {
  render(<Toast message="Saved!" type="success" onDismiss={() => {}} />)
  expect(screen.getByTestId('toast')).toHaveTextContent('Saved!')
})

test('calls onDismiss when × button clicked', () => {
  const onDismiss = vi.fn()
  render(<Toast message="Info" type="info" onDismiss={onDismiss} />)
  fireEvent.click(screen.getByRole('button', { name: /dismiss/i }))
  expect(onDismiss).toHaveBeenCalledOnce()
})

test('calls onDismiss automatically after 4 seconds', () => {
  const onDismiss = vi.fn()
  render(<Toast message="Auto" type="success" onDismiss={onDismiss} />)
  vi.advanceTimersByTime(4000)
  expect(onDismiss).toHaveBeenCalledOnce()
})

test('cleans up timer on unmount — onDismiss not called when unmounted before 4s', () => {
  const onDismiss = vi.fn()
  const { unmount } = render(<Toast message="Short" type="info" onDismiss={onDismiss} />)
  vi.advanceTimersByTime(2000)
  unmount()
  vi.advanceTimersByTime(4000)
  expect(onDismiss).not.toHaveBeenCalled()
})
