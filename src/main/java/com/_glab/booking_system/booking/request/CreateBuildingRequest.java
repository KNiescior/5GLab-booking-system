package com._glab.booking_system.booking.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating a building.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBuildingRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;
}
