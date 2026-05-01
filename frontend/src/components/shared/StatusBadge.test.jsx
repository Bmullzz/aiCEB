import { render, screen } from '@testing-library/react'
import StatusBadge from './StatusBadge'

test('renders UPCOMING with teal background', () => {
  render(<StatusBadge status="UPCOMING" />)
  const badge = screen.getByTestId('status-badge')
  expect(badge).toHaveTextContent('UPCOMING')
  expect(badge).toHaveStyle({ backgroundColor: '#5DCAA5' })
})

test('renders CANCELLED with red background', () => {
  render(<StatusBadge status="CANCELLED" />)
  const badge = screen.getByTestId('status-badge')
  expect(badge).toHaveStyle({ backgroundColor: '#EF4444' })
})

test('renders IN_PROGRESS with amber background', () => {
  render(<StatusBadge status="IN_PROGRESS" />)
  const badge = screen.getByTestId('status-badge')
  expect(badge).toHaveStyle({ backgroundColor: '#F59E0B' })
})

test('renders unknown status without crashing', () => {
  render(<StatusBadge status="UNKNOWN_STATUS" />)
  expect(screen.getByTestId('status-badge')).toHaveTextContent('UNKNOWN_STATUS')
})
