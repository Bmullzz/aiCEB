package com.yourorg.eventdashboard.shared;

public class AlreadyOptedOutException extends RuntimeException {

    public AlreadyOptedOutException(String message) {
        super(message);
    }
}
