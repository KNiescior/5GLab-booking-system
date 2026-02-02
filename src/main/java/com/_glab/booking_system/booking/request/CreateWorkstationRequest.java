package com._glab.booking_system.booking.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating a workstation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkstationRequest {

    @NotNull(message = "Lab ID is required")
    private Integer labId;

    @NotBlank(message = "Identifier is required")
    @Size(max = 20)
    private String identifier;

    @Size(max = 500)
    private String description;
}
