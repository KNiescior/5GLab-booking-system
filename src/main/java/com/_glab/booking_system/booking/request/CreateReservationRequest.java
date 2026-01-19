package com._glab.booking_system.booking.request;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationRequest {

    @NotNull(message = "Lab ID is required")
    private Integer labId;

    @NotNull(message = "Start time is required")
    private OffsetDateTime startTime;

    @NotNull(message = "End time is required")
    private OffsetDateTime endTime;

    private String description;

    @Builder.Default
    private Boolean wholeLab = false;

    /**
     * List of workstation IDs to reserve.
     * Required if wholeLab is false.
     */
    private List<Integer> workstationIds;

    /**
     * Optional recurring pattern configuration.
     */
    private RecurringConfig recurring;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecurringConfig {
        private String patternType; // WEEKLY, BIWEEKLY, MONTHLY, CUSTOM
        private Integer intervalDays; // For CUSTOM pattern
        private String endDate; // ISO date string
        private Integer occurrences;
    }
}
