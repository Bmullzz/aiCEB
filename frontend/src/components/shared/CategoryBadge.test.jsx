import { render, screen } from '@testing-library/react'
import CategoryBadge from './CategoryBadge'

const category = { name: 'Workshop', color: '#5DCAA5' }

test('renders category name', () => {
  render(<CategoryBadge category={category} />)
  expect(screen.getByTestId('category-badge')).toHaveTextContent('Workshop')
})

test('applies category color as backgroundColor style', () => {
  render(<CategoryBadge category={category} />)
  expect(screen.getByTestId('category-badge')).toHaveStyle({ backgroundColor: '#5DCAA5' })
})

test('renders null when category prop is null', () => {
  const { container } = render(<CategoryBadge category={null} />)
  expect(container.firstChild).toBeNull()
})
