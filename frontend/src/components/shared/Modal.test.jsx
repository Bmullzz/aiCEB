import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Modal from './Modal'

test('renders null when isOpen is false', () => {
  const { container } = render(
    <Modal isOpen={false} onClose={() => {}}>
      <p>Content</p>
    </Modal>
  )
  expect(container.firstChild).toBeNull()
})

test('renders children when isOpen is true', () => {
  render(
    <Modal isOpen onClose={() => {}}>
      <p>Modal content</p>
    </Modal>
  )
  expect(screen.getByText('Modal content')).toBeInTheDocument()
})

test('calls onClose when overlay is clicked', async () => {
  const onClose = vi.fn()
  render(
    <Modal isOpen onClose={onClose}>
      <p>Content</p>
    </Modal>
  )
  await userEvent.click(screen.getByTestId('modal-overlay'))
  expect(onClose).toHaveBeenCalledOnce()
})

test('does NOT call onClose when modal content is clicked', async () => {
  const onClose = vi.fn()
  render(
    <Modal isOpen onClose={onClose}>
      <p>Content</p>
    </Modal>
  )
  await userEvent.click(screen.getByTestId('modal-content'))
  expect(onClose).not.toHaveBeenCalled()
})

test('calls onClose when close button is clicked', async () => {
  const onClose = vi.fn()
  render(
    <Modal isOpen onClose={onClose}>
      <p>Content</p>
    </Modal>
  )
  await userEvent.click(screen.getByTestId('modal-close-button'))
  expect(onClose).toHaveBeenCalledOnce()
})
