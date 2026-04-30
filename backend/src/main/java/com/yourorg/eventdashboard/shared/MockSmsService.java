package com.yourorg.eventdashboard.shared;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * No-op SMS implementation for local development and tests.
 * Active only when the {@code mock} Spring profile is active.
 * Never throws — every call succeeds and returns a unique fake SID.
 */
@Service
@Profile("mock")
@Slf4j
public class MockSmsService implements SmsService {

    @Override
    public String send(String toPhoneNumber, String messageBody) {
        log.info("MOCK SMS → [message length: {} chars]", messageBody.length());
        return "MOCK-SID-" + UUID.randomUUID();
    }
}
