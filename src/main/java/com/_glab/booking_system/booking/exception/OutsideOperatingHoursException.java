package com._glab.booking_system.booking.exception;

public class OutsideOperatingHoursException extends RuntimeException {
    public OutsideOperatingHoursException(String message) {
        super(message);
    }
}
