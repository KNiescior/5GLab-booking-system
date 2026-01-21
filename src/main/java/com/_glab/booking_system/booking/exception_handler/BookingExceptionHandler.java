package com._glab.booking_system.booking.exception_handler;

import com._glab.booking_system.ErrorResponse;
import com._glab.booking_system.ErrorResponseCode;
import com._glab.booking_system.booking.exception.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class BookingExceptionHandler {

    // ==================== Resource Not Found ====================

    @ExceptionHandler(LabNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLabNotFound(LabNotFoundException e) {
        log.warn("Lab not found: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_LAB_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(WorkstationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkstationNotFound(WorkstationNotFoundException e) {
        log.warn("Workstation not found: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_WORKSTATION_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFound(ReservationNotFoundException e) {
        log.warn("Reservation not found: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_RESERVATION_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BuildingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBuildingNotFound(BuildingNotFoundException e) {
        log.warn("Building not found: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_BUILDING_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // ==================== Validation Errors ====================

    @ExceptionHandler(InvalidReservationTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTime(InvalidReservationTimeException e) {
        log.warn("Invalid reservation time: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_INVALID_TIME_RANGE, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(OutsideOperatingHoursException.class)
    public ResponseEntity<ErrorResponse> handleOutsideOperatingHours(OutsideOperatingHoursException e) {
        log.warn("Reservation outside operating hours: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_OUTSIDE_OPERATING_HOURS, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(LabClosedException.class)
    public ResponseEntity<ErrorResponse> handleLabClosed(LabClosedException e) {
        log.warn("Lab is closed: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_LAB_CLOSED, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(WorkstationNotInLabException.class)
    public ResponseEntity<ErrorResponse> handleWorkstationNotInLab(WorkstationNotInLabException e) {
        log.warn("Workstation not in specified lab: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_WORKSTATION_NOT_IN_LAB, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(WorkstationInactiveException.class)
    public ResponseEntity<ErrorResponse> handleWorkstationInactive(WorkstationInactiveException e) {
        log.warn("Workstation is inactive: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_WORKSTATION_INACTIVE, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(NoWorkstationsSelectedException.class)
    public ResponseEntity<ErrorResponse> handleNoWorkstationsSelected(NoWorkstationsSelectedException e) {
        log.warn("No workstations selected: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_NO_WORKSTATIONS_SELECTED, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ==================== Recurring Errors ====================

    @ExceptionHandler(InvalidRecurringPatternException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRecurringPattern(InvalidRecurringPatternException e) {
        log.warn("Invalid recurring pattern: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_INVALID_RECURRING_PATTERN, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(NoValidOccurrencesException.class)
    public ResponseEntity<ErrorResponse> handleNoValidOccurrences(NoValidOccurrencesException e) {
        log.warn("No valid occurrences generated: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_NO_VALID_OCCURRENCES, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ==================== Authorization ====================

    @ExceptionHandler(BookingNotAuthorizedException.class)
    public ResponseEntity<ErrorResponse> handleNotAuthorized(BookingNotAuthorizedException e) {
        log.warn("Booking authorization denied: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_NOT_AUTHORIZED, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(NotLabManagerException.class)
    public ResponseEntity<ErrorResponse> handleNotLabManager(NotLabManagerException e) {
        log.warn("User is not a lab manager: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_NOT_LAB_MANAGER, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(NotReservationOwnerException.class)
    public ResponseEntity<ErrorResponse> handleNotReservationOwner(NotReservationOwnerException e) {
        log.warn("User is not the reservation owner: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_NOT_RESERVATION_OWNER, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // ==================== Edit Proposal Errors ====================

    @ExceptionHandler(EditProposalNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEditProposalNotFound(EditProposalNotFoundException e) {
        log.warn("Edit proposal not found: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_EDIT_PROPOSAL_NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InvalidEditException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEdit(InvalidEditException e) {
        log.warn("Invalid edit: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_INVALID_EDIT, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(EditAlreadyResolvedException.class)
    public ResponseEntity<ErrorResponse> handleEditAlreadyResolved(EditAlreadyResolvedException e) {
        log.warn("Edit proposal already resolved: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(ErrorResponseCode.BOOKING_EDIT_ALREADY_RESOLVED, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
