package com._glab.booking_system;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponse {
    private final String TYPE = "error";
    private ErrorResponseCode status;
    private String message;
}
