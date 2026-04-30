package com.yourorg.eventdashboard.shared;

/**
 * Abstraction over SMS delivery. Always inject this interface — never import Twilio classes
 * outside of {@link TwilioSmsService}.
 */
public interface SmsService {

    /**
     * Send an SMS message.
     *
     * @param toPhoneNumber must be in E.164 format e.g. {@code +12155550123}
     * @param messageBody   the text content to send
     * @return the provider message ID (Twilio message_sid or mock equivalent)
     * @throws SmsDeliveryException on any delivery failure
     */
    String send(String toPhoneNumber, String messageBody);
}
