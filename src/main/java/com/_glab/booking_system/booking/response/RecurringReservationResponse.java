package com._glab.booking_system.booking.response;

import java.util.List;
import java.util.UUID;

import com._glab.booking_system.booking.model.RecurrenceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringReservationResponse {

    private UUID recurringGroupId;
    private RecurrenceType patternType;
    private Integer totalOccurrences;
    private List<ReservationResponse> reservations;
}
