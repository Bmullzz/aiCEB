package com.yourorg.eventdashboard.shared;

/**
 * Thrown when an SMS cannot be delivered. This is the only SMS-related exception that escapes
 * the shared package — no Twilio-specific exceptions leak into calling code.
 */
public class SmsDeliveryException extends RuntimeException {

    public SmsDeliveryException(String message) {
        super(message);
    }

    public SmsDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
