package com._glab.booking_system.booking.exception;

public class BuildingNotFoundException extends RuntimeException {
    public BuildingNotFoundException(String message) {
        super(message);
    }

    public BuildingNotFoundException(Integer buildingId) {
        super("Building not found: " + buildingId);
    }
}
