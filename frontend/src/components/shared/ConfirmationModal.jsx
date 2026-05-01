import LoadingSpinner from './LoadingSpinner'
import Modal from './Modal'

export default function ConfirmationModal({
  isOpen,
  onClose,
  title,
  body,
  confirmLabel,
  cancelLabel,
  onConfirm,
  onCancel,
  isLoading,
}) {
  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <h2>{title}</h2>
      <div>{body}</div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '16px' }}>
        <button data-testid="cancel-button" onClick={onCancel}>
          {cancelLabel}
        </button>
        <button data-testid="confirm-button" onClick={onConfirm} disabled={isLoading}>
          {isLoading && <LoadingSpinner size="sm" />}
          {confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
