package com.yourorg.eventdashboard.shared;

public class EventInPastException extends RuntimeException {

    public EventInPastException(String message) {
        super(message);
    }
}
