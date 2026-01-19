package com._glab.booking_system.booking.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkstationResponse {

    private Integer id;
    private String identifier;
    private String description;
    private Boolean active;
}
