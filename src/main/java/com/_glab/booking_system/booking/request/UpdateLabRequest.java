package com._glab.booking_system.booking.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Request DTO for updating a lab.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLabRequest {

    private Integer buildingId;

    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    private Integer capacity;

    private LocalTime defaultOpenTime;

    private LocalTime defaultCloseTime;
}
