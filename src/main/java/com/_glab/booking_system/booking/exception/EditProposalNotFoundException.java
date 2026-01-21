package com._glab.booking_system.booking.exception;

import java.util.UUID;

public class EditProposalNotFoundException extends RuntimeException {
    public EditProposalNotFoundException(String message) {
        super(message);
    }

    public EditProposalNotFoundException(UUID reservationId) {
        super("No active edit proposal found for reservation: " + reservationId);
    }
}
