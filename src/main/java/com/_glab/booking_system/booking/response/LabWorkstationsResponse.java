package com._glab.booking_system.booking.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabWorkstationsResponse {

    private Integer labId;
    private List<WorkstationResponse> workstations;
}
