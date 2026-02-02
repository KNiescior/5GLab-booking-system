package com._glab.booking_system.booking.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for updating a workstation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorkstationRequest {

    @Size(max = 20)
    private String identifier;

    @Size(max = 500)
    private String description;

    private Boolean active;
}
