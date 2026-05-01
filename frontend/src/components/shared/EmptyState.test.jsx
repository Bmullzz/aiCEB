import { render, screen } from '@testing-library/react'
import EmptyState from './EmptyState'

test('renders message text', () => {
  render(<EmptyState message="No events found." />)
  expect(screen.getByText('No events found.')).toBeInTheDocument()
})

test('renders icon when provided', () => {
  render(<EmptyState message="Nothing here." icon="📭" />)
  expect(screen.getByText('📭')).toBeInTheDocument()
})

test('renders without icon when not provided', () => {
  const { container } = render(<EmptyState message="Nothing here." />)
  expect(screen.getByTestId('empty-state')).toBeInTheDocument()
  // No icon div rendered — only the message p
  expect(container.querySelectorAll('p')).toHaveLength(1)
})
