package com._glab.booking_system.booking.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request DTO for adding/updating a day off (specific date or recurring day of week).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DaysOffRequest {

    /**
     * Specific date (e.g. holiday). Either this or recurringDayOfWeek should be set.
     */
    private LocalDate specificDate;

    /**
     * Recurring day of week: 0 = Sunday, ..., 6 = Saturday.
     */
    @Min(0)
    @Max(6)
    private Integer recurringDayOfWeek;

    private String reason;
}
