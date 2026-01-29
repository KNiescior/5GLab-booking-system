package com._glab.booking_system.booking.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for special operating hours on a specific date.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialOperatingHoursRequest {

    @NotNull(message = "Specific date is required")
    private LocalDate specificDate;

    private LocalTime openTime;

    private LocalTime closeTime;

    private Boolean isClosed;
}
