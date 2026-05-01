import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ConfirmationModal from './ConfirmationModal'

const defaultProps = {
  isOpen: true,
  onClose: () => {},
  title: 'Delete event',
  body: 'Are you sure?',
  confirmLabel: 'Delete',
  cancelLabel: 'Cancel',
  onConfirm: () => {},
  onCancel: () => {},
  isLoading: false,
}

test('renders title and body', () => {
  render(<ConfirmationModal {...defaultProps} />)
  expect(screen.getByText('Delete event')).toBeInTheDocument()
  expect(screen.getByText('Are you sure?')).toBeInTheDocument()
})

test('confirm button is disabled when isLoading is true', () => {
  render(<ConfirmationModal {...defaultProps} isLoading />)
  expect(screen.getByTestId('confirm-button')).toBeDisabled()
})

test('confirm button is enabled when isLoading is false', () => {
  render(<ConfirmationModal {...defaultProps} isLoading={false} />)
  expect(screen.getByTestId('confirm-button')).not.toBeDisabled()
})

test('calls onConfirm when confirm button clicked', async () => {
  const onConfirm = vi.fn()
  render(<ConfirmationModal {...defaultProps} onConfirm={onConfirm} />)
  await userEvent.click(screen.getByTestId('confirm-button'))
  expect(onConfirm).toHaveBeenCalledOnce()
})

test('calls onCancel when cancel button clicked', async () => {
  const onCancel = vi.fn()
  render(<ConfirmationModal {...defaultProps} onCancel={onCancel} />)
  await userEvent.click(screen.getByTestId('cancel-button'))
  expect(onCancel).toHaveBeenCalledOnce()
})

test('shows LoadingSpinner inside confirm button when isLoading is true', () => {
  render(<ConfirmationModal {...defaultProps} isLoading />)
  expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
})
