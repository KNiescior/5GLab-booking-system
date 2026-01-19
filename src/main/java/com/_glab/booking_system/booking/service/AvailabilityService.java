package com._glab.booking_system.booking.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabClosedDay;
import com._glab.booking_system.booking.model.LabOperatingHours;
import com._glab.booking_system.booking.model.Reservation;
import com._glab.booking_system.booking.model.ReservationStatus;
import com._glab.booking_system.booking.model.Workstation;
import com._glab.booking_system.booking.repository.LabClosedDayRepository;
import com._glab.booking_system.booking.repository.LabOperatingHoursRepository;
import com._glab.booking_system.booking.repository.LabRepository;
import com._glab.booking_system.booking.repository.ReservationRepository;
import com._glab.booking_system.booking.repository.ReservationWorkstationRepository;
import com._glab.booking_system.booking.repository.WorkstationRepository;
import com._glab.booking_system.booking.response.ClosedDayResponse;
import com._glab.booking_system.booking.response.CurrentAvailabilityResponse;
import com._glab.booking_system.booking.response.LabAvailabilityResponse;
import com._glab.booking_system.booking.response.LabWorkstationsResponse;
import com._glab.booking_system.booking.response.OperatingHoursResponse;
import com._glab.booking_system.booking.response.ReservationSummaryResponse;
import com._glab.booking_system.booking.response.WorkstationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    private final LabRepository labRepository;
    private final LabOperatingHoursRepository operatingHoursRepository;
    private final LabClosedDayRepository closedDayRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationWorkstationRepository reservationWorkstationRepository;
    private final WorkstationRepository workstationRepository;

    /**
     * Get weekly availability for a lab.
     * 
     * @param labId ID of the lab
     * @param weekStart Start date of the week (Monday). If null, uses current week.
     * @return Availability data including operating hours, closed days, and reservations
     */
    public LabAvailabilityResponse getWeeklyAvailability(Integer labId, LocalDate weekStart) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new IllegalArgumentException("Lab not found: " + labId));

        // Normalize to Monday of the week
        if (weekStart == null) {
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else {
            weekStart = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        LocalDate weekEnd = weekStart.plusDays(6); // Sunday

        // Get operating hours
        List<OperatingHoursResponse> operatingHours = getOperatingHours(labId, lab);

        // Get closed days for this week
        List<ClosedDayResponse> closedDays = getClosedDaysInRange(labId, weekStart, weekEnd);

        // Get reservations for this week
        OffsetDateTime startDateTime = weekStart.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endDateTime = weekEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        
        List<ReservationSummaryResponse> reservations = getReservationsInRange(labId, startDateTime, endDateTime);

        return LabAvailabilityResponse.builder()
                .labId(labId)
                .labName(lab.getName())
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .operatingHours(operatingHours)
                .closedDays(closedDays)
                .reservations(reservations)
                .build();
    }

    /**
     * Get current availability - what's happening right now.
     */
    public CurrentAvailabilityResponse getCurrentAvailability(Integer labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new IllegalArgumentException("Lab not found: " + labId));

        OffsetDateTime now = OffsetDateTime.now();
        
        // Check if lab is currently open
        boolean isOpen = isLabOpenAt(labId, lab, now);

        // Get current APPROVED reservations only
        List<Reservation> currentReservations = reservationRepository.findCurrentReservations(
                labId, now, ReservationStatus.APPROVED);

        List<ReservationSummaryResponse> reservationSummaries = currentReservations.stream()
                .map(this::toReservationSummary)
                .collect(Collectors.toList());

        return CurrentAvailabilityResponse.builder()
                .labId(labId)
                .labName(lab.getName())
                .isOpen(isOpen)
                .currentReservations(reservationSummaries)
                .build();
    }

    /**
     * Get all workstations for a lab.
     */
    public LabWorkstationsResponse getLabWorkstations(Integer labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new IllegalArgumentException("Lab not found: " + labId));

        List<Workstation> workstations = workstationRepository.findByLabId(labId);

        List<WorkstationResponse> workstationResponses = workstations.stream()
                .map(ws -> WorkstationResponse.builder()
                        .id(ws.getId())
                        .identifier(ws.getIdentifier())
                        .description(ws.getDescription())
                        .active(ws.getActive())
                        .build())
                .collect(Collectors.toList());

        return LabWorkstationsResponse.builder()
                .labId(labId)
                .workstations(workstationResponses)
                .build();
    }

    // === Private helper methods ===

    private List<OperatingHoursResponse> getOperatingHours(Integer labId, Lab lab) {
        List<LabOperatingHours> hours = operatingHoursRepository.findByLabId(labId);
        
        // If no specific hours defined, use lab defaults for all days
        if (hours.isEmpty()) {
            List<OperatingHoursResponse> defaultHours = new ArrayList<>();
            for (int day = 0; day <= 6; day++) {
                boolean isSunday = (day == 0);
                defaultHours.add(OperatingHoursResponse.builder()
                        .dayOfWeek(day)
                        .open(isSunday ? null : lab.getDefaultOpenTime())
                        .close(isSunday ? null : lab.getDefaultCloseTime())
                        .closed(isSunday)
                        .build());
            }
            return defaultHours;
        }

        return hours.stream()
                .map(h -> OperatingHoursResponse.builder()
                        .dayOfWeek(h.getDayOfWeek())
                        .open(h.getOpenTime())
                        .close(h.getCloseTime())
                        .closed(h.getIsClosed())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ClosedDayResponse> getClosedDaysInRange(Integer labId, LocalDate start, LocalDate end) {
        List<LabClosedDay> specificClosures = closedDayRepository.findSpecificClosuresInRange(labId, start, end);
        List<LabClosedDay> recurringClosures = closedDayRepository.findRecurringClosures(labId);

        List<ClosedDayResponse> result = new ArrayList<>();

        // Add specific closures
        for (LabClosedDay closure : specificClosures) {
            result.add(ClosedDayResponse.builder()
                    .date(closure.getSpecificDate())
                    .reason(closure.getReason())
                    .build());
        }

        // Add recurring closures for each matching day in the range
        for (LabClosedDay recurring : recurringClosures) {
            LocalDate current = start;
            while (!current.isAfter(end)) {
                // Java DayOfWeek: MONDAY=1, SUNDAY=7. Our DB: SUNDAY=0, MONDAY=1
                int javaDayValue = current.getDayOfWeek().getValue();
                int ourDayValue = javaDayValue == 7 ? 0 : javaDayValue;
                
                if (ourDayValue == recurring.getRecurringDayOfWeek()) {
                    result.add(ClosedDayResponse.builder()
                            .date(current)
                            .reason(recurring.getReason())
                            .build());
                }
                current = current.plusDays(1);
            }
        }

        return result;
    }

    private List<ReservationSummaryResponse> getReservationsInRange(
            Integer labId, OffsetDateTime start, OffsetDateTime end) {
        
        // Get both PENDING and APPROVED reservations
        List<ReservationStatus> statuses = List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);
        List<Reservation> reservations = reservationRepository.findByLabIdAndTimeRangeAndStatusIn(
                labId, start, end, statuses);

        return reservations.stream()
                .map(this::toReservationSummary)
                .collect(Collectors.toList());
    }

    private ReservationSummaryResponse toReservationSummary(Reservation reservation) {
        List<Integer> workstationIds = reservationWorkstationRepository
                .findWorkstationIdsByReservationId(reservation.getId());

        String userName = reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName();

        return ReservationSummaryResponse.builder()
                .id(reservation.getId())
                .date(reservation.getStartTime().toLocalDate())
                .startTime(reservation.getStartTime().toLocalTime())
                .endTime(reservation.getEndTime().toLocalTime())
                .status(reservation.getStatus())
                .wholeLab(reservation.getWholeLab())
                .workstationIds(workstationIds)
                .userName(userName)
                .build();
    }

    private boolean isLabOpenAt(Integer labId, Lab lab, OffsetDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        
        // Check if it's a closed day
        int javaDayValue = date.getDayOfWeek().getValue();
        int dayOfWeek = javaDayValue == 7 ? 0 : javaDayValue;
        
        if (closedDayRepository.isLabClosedOnDate(labId, date, dayOfWeek)) {
            return false;
        }

        // Check operating hours
        LabOperatingHours hours = operatingHoursRepository.findByLabIdAndDayOfWeek(labId, dayOfWeek)
                .orElse(null);

        if (hours != null) {
            if (hours.getIsClosed()) {
                return false;
            }
            return !time.isBefore(hours.getOpenTime()) && time.isBefore(hours.getCloseTime());
        }

        // Use lab defaults
        if (dayOfWeek == 0) { // Sunday
            return false;
        }
        
        LocalTime openTime = lab.getDefaultOpenTime() != null ? lab.getDefaultOpenTime() : LocalTime.of(8, 0);
        LocalTime closeTime = lab.getDefaultCloseTime() != null ? lab.getDefaultCloseTime() : LocalTime.of(20, 0);
        
        return !time.isBefore(openTime) && time.isBefore(closeTime);
    }
}
