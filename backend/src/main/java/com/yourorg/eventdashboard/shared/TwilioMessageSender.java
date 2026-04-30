package com.yourorg.eventdashboard.shared;

/**
 * Package-private functional interface that wraps the Twilio {@code Message.creator().create()} call.
 * <p>
 * Extracted to allow unit testing of {@link TwilioSmsService} without {@code Mockito.mockStatic},
 * which is fragile against Twilio SDK version changes. Tests inject a lambda via the package-private
 * test constructor instead. See {@code TwilioSmsServiceTest} for usage.
 */
@FunctionalInterface
interface TwilioMessageSender {

    /**
     * @param toPhoneNumber  E.164 recipient number
     * @param fromPhoneNumber E.164 sender number
     * @param messageBody    SMS text content
     * @return provider message SID
     */
    String send(String toPhoneNumber, String fromPhoneNumber, String messageBody);
}
