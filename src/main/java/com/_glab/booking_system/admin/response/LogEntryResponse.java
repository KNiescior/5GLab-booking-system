package com._glab.booking_system.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for a single log entry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LogEntryResponse {

    private String timestamp;
    private String level;
    private String logger;
    private String message;
    private String raw;
}
