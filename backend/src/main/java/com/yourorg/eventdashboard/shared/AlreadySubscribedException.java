package com.yourorg.eventdashboard.shared;

public class AlreadySubscribedException extends RuntimeException {

    public AlreadySubscribedException(String message) {
        super(message);
    }
}
