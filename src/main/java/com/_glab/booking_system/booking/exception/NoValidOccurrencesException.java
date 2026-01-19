package com._glab.booking_system.booking.exception;

public class NoValidOccurrencesException extends RuntimeException {
    public NoValidOccurrencesException() {
        super("No valid reservations could be created. All dates may be invalid.");
    }

    public NoValidOccurrencesException(String message) {
        super(message);
    }
}
