package com._glab.booking_system.booking.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com._glab.booking_system.booking.model.ReservationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private UUID id;
    private Integer labId;
    private String labName;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String description;
    private ReservationStatus status;
    private Boolean wholeLab;
    private List<Integer> workstationIds;
    private UUID recurringGroupId;
    private OffsetDateTime createdAt;
}
