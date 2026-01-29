package com._glab.booking_system.booking.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for updating a building.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBuildingRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;
}
