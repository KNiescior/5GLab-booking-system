package com._glab.booking_system;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {
    private static final ErrorResponseType TYPE = ErrorResponseType.ERROR;
    private ErrorResponseCode status;
    private String message;
}
