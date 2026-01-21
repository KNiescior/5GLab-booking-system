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
public class EditReservationRequest {

    @NotNull(message = "Start time is required")
    private OffsetDateTime startTime;

    @NotNull(message = "End time is required")
    private OffsetDateTime endTime;

    private String description;

    @Builder.Default
    private Boolean wholeLab = Boolean.FALSE;

    /**
     * List of workstation IDs to reserve.
     * Required if wholeLab is false.
     */
    private List<Integer> workstationIds;

    /**
     * Custom setter to ensure wholeLab is never null.
     */
    public void setWholeLab(Boolean wholeLab) {
        this.wholeLab = wholeLab != null ? wholeLab : Boolean.FALSE;
    }
}
