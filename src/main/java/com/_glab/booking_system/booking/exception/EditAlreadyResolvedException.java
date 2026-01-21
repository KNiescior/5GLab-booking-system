package com._glab.booking_system.booking.exception;

import java.util.UUID;

public class EditAlreadyResolvedException extends RuntimeException {
    public EditAlreadyResolvedException(String message) {
        super(message);
    }

    public EditAlreadyResolvedException(UUID proposalId) {
        super("Edit proposal has already been resolved: " + proposalId);
    }
}
