import { render, screen } from '@testing-library/react'
import LoadingSpinner from './LoadingSpinner'

test('renders with data-testid="loading-spinner"', () => {
  render(<LoadingSpinner />)
  expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
})

test('applies spinner-sm class for size="sm"', () => {
  render(<LoadingSpinner size="sm" />)
  expect(screen.getByTestId('loading-spinner').querySelector('.spinner-sm')).toBeInTheDocument()
})

test('applies spinner-lg class for size="lg"', () => {
  render(<LoadingSpinner size="lg" />)
  expect(screen.getByTestId('loading-spinner').querySelector('.spinner-lg')).toBeInTheDocument()
})

test('defaults to md when no size prop provided', () => {
  render(<LoadingSpinner />)
  expect(screen.getByTestId('loading-spinner').querySelector('.spinner-md')).toBeInTheDocument()
})
