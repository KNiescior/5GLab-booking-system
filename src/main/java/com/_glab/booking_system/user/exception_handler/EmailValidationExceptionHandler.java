package com._glab.booking_system.user.exception_handler;

import com._glab.booking_system.ErrorResponse;
import com._glab.booking_system.ErrorResponseCode;
import com._glab.booking_system.user.exception.InvalidEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class EmailValidationExceptionHandler {

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmailException(InvalidEmailException e) {
        ErrorResponse errorResponse = new ErrorResponse((ErrorResponseCode.USER_EMAIL_NOT_VALID), e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
    }
}
