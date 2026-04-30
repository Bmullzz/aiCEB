package com.yourorg.eventdashboard.shared;

public class InvalidEventTimeException extends RuntimeException {

    public InvalidEventTimeException(String message) {
        super(message);
    }
}
