package com._glab.booking_system.booking.exception;

public class BookingNotAuthorizedException extends RuntimeException {
    public BookingNotAuthorizedException(String message) {
        super(message);
    }
}
