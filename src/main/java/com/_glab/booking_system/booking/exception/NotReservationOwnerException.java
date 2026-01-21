package com._glab.booking_system.booking.exception;

import java.util.UUID;

public class NotReservationOwnerException extends RuntimeException {
    public NotReservationOwnerException(String message) {
        super(message);
    }

    public NotReservationOwnerException(UUID reservationId) {
        super("User is not the owner of reservation: " + reservationId);
    }
}
