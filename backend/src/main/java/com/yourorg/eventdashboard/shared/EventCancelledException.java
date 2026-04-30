package com.yourorg.eventdashboard.shared;

public class EventCancelledException extends RuntimeException {

    public EventCancelledException(String message) {
        super(message);
    }
}
