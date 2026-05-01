import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ErrorMessage from './ErrorMessage'

test('renders error.message text', () => {
  render(<ErrorMessage error={{ message: 'Something went wrong.' }} />)
  expect(screen.getByText('Something went wrong.')).toBeInTheDocument()
})

test('renders "Try again" button when onRetry is provided', () => {
  render(<ErrorMessage error={{ message: 'Oops.' }} onRetry={() => {}} />)
  expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument()
})

test('does not render "Try again" button when onRetry is absent', () => {
  render(<ErrorMessage error={{ message: 'Oops.' }} />)
  expect(screen.queryByRole('button', { name: /try again/i })).not.toBeInTheDocument()
})

test('calls onRetry when "Try again" is clicked', async () => {
  const onRetry = vi.fn()
  render(<ErrorMessage error={{ message: 'Oops.' }} onRetry={onRetry} />)
  await userEvent.click(screen.getByRole('button', { name: /try again/i }))
  expect(onRetry).toHaveBeenCalledOnce()
})

test('renders null when error prop is null', () => {
  const { container } = render(<ErrorMessage error={null} />)
  expect(container.firstChild).toBeNull()
})
