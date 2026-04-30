package com.yourorg.eventdashboard.shared;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Spring profile wiring selects the correct SmsService implementation.
 *
 * <p>{@code @TestPropertySource} overrides {@code spring.profiles.active} from
 * {@code application.properties} (which defaults to "twilio") so that only the "test"
 * and "mock" profiles are active. Without this override, both TwilioSmsService and
 * MockSmsService would be registered and autowiring would fail with NoUniqueBeanDefinitionException.
 *
 * <p>The equivalent test with {@code @ActiveProfiles("twilio")} is intentionally omitted from CI.
 * It requires real Twilio credentials (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER)
 * that are not present in CI environments.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "mock"})
@TestPropertySource(properties = "spring.profiles.active=test,mock")
class SmsServiceProfileTest {

    @Autowired
    private SmsService smsService;

    @Test
    void smsServiceBeanIsMockImplementationWhenMockProfileActive() {
        assertThat(smsService).isInstanceOf(MockSmsService.class);
    }
}
