import './Modal.css'

export default function Modal({ isOpen, onClose, children }) {
  if (!isOpen) return null

  return (
    <div
      className="modal-overlay"
      data-testid="modal-overlay"
      onClick={onClose}
    >
      <div
        className="modal-content"
        data-testid="modal-content"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          className="modal-close-button"
          aria-label="Close modal"
          data-testid="modal-close-button"
          onClick={onClose}
        >
          ×
        </button>
        {children}
      </div>
    </div>
  )
}
