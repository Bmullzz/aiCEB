package com.yourorg.eventdashboard.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class MockSmsServiceTest {

    private final MockSmsService service = new MockSmsService();

    @Test
    void send_returnsStringStartingWithMockSid() {
        String sid = service.send("+12155550123", "Hello World");

        assertThat(sid).startsWith("MOCK-SID-");
    }

    @Test
    void send_neverThrowsWithEdgeCaseInputs() {
        assertThatNoException().isThrownBy(() -> service.send("+1", ""));
        assertThatNoException().isThrownBy(() -> service.send("+12155550123", " ".repeat(160)));
        assertThatNoException().isThrownBy(() -> service.send("+12155550123", "a".repeat(1600)));
    }

    @Test
    void send_returnsDifferentSidsAcrossCalls() {
        String sid1 = service.send("+12155550123", "Hello");
        String sid2 = service.send("+12155550123", "Hello");

        assertThat(sid1).isNotEqualTo(sid2);
    }
}
