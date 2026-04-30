package com.yourorg.eventdashboard.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TwilioSmsService.
 *
 * <p>Rather than using Mockito.mockStatic on the Twilio SDK's static Message.creator() method
 * (which is fragile against SDK version changes and requires mockito-inline configuration),
 * these tests use TwilioSmsService's package-private test constructor to inject a
 * {@link TwilioMessageSender} lambda directly. This avoids all static mocking complexity
 * while still exercising the full send() logic including logging and exception translation.
 */
@ExtendWith(MockitoExtension.class)
class TwilioSmsServiceTest {

    private static final String ACCOUNT_SID = "ACtest123";
    private static final String AUTH_TOKEN  = "test_auth_token";
    private static final String FROM_NUMBER = "+15551234567";

    @Test
    void send_returnsMessageSidOnSuccess() {
        TwilioSmsService service = new TwilioSmsService(
                ACCOUNT_SID, AUTH_TOKEN, FROM_NUMBER,
                (to, from, body) -> "SM_SUCCESS_SID_123");

        String sid = service.send("+12155550123", "Hello World");

        assertThat(sid).isEqualTo("SM_SUCCESS_SID_123");
    }

    @Test
    void send_throwsSmsDeliveryExceptionNotRawExceptionWhenSdkFails() {
        TwilioSmsService service = new TwilioSmsService(
                ACCOUNT_SID, AUTH_TOKEN, FROM_NUMBER,
                (to, from, body) -> { throw new RuntimeException("Twilio network error"); });

        assertThatThrownBy(() -> service.send("+12155550123", "Hello"))
                .isInstanceOf(SmsDeliveryException.class)
                .hasMessageContaining("Twilio network error");
    }

    @Test
    void send_preservesSmsDeliveryExceptionWithoutRewrapping() {
        // Simulates the null-return guard in the production messageSender lambda:
        // Message.creator().create() returns null → SmsDeliveryException is thrown directly
        // and must not be double-wrapped when it propagates through send().
        TwilioSmsService service = new TwilioSmsService(
                ACCOUNT_SID, AUTH_TOKEN, FROM_NUMBER,
                (to, from, body) -> { throw new SmsDeliveryException("Twilio create() returned null"); });

        assertThatThrownBy(() -> service.send("+12155550123", "Hello"))
                .isInstanceOf(SmsDeliveryException.class)
                .hasMessage("Twilio create() returned null");
    }
}
