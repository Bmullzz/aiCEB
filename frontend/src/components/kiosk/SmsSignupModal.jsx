export default function SmsSignupModal({ isOpen, onClose, event }) {
  if (!isOpen) return null

  return (
    <div data-testid="sms-signup-modal">
      <button onClick={onClose}>Close</button>
    </div>
  )
}
