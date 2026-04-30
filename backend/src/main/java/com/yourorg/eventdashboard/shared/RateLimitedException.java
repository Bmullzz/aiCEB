package com.yourorg.eventdashboard.shared;

// Stub only — rate limiting is implemented in Story 10.
public class RateLimitedException extends RuntimeException {

    public RateLimitedException(String message) {
        super(message);
    }
}
