package com._glab.booking_system.booking.response;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatingHoursResponse {

    private Integer dayOfWeek;
    private LocalTime open;
    private LocalTime close;
    private Boolean closed;
}
