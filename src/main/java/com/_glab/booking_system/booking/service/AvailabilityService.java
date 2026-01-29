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

import com._glab.booking_system.booking.model.BuildingClosedDay;
import com._glab.booking_system.booking.model.BuildingOperatingHours;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabClosedDay;
import com._glab.booking_system.booking.model.LabOperatingHours;
import com._glab.booking_system.booking.model.Reservation;
import com._glab.booking_system.booking.model.ReservationStatus;
import com._glab.booking_system.booking.model.SpecialOperatingHours;
import com._glab.booking_system.booking.model.Workstation;
import com._glab.booking_system.booking.repository.BuildingClosedDayRepository;
import com._glab.booking_system.booking.repository.BuildingOperatingHoursRepository;
import com._glab.booking_system.booking.repository.LabClosedDayRepository;
import com._glab.booking_system.booking.repository.LabOperatingHoursRepository;
import com._glab.booking_system.booking.repository.LabRepository;
import com._glab.booking_system.booking.repository.ReservationRepository;
import com._glab.booking_system.booking.repository.ReservationWorkstationRepository;
import com._glab.booking_system.booking.repository.SpecialOperatingHoursRepository;
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
    private final BuildingOperatingHoursRepository buildingOperatingHoursRepository;
    private final LabClosedDayRepository closedDayRepository;
    private final BuildingClosedDayRepository buildingClosedDayRepository;
    private final SpecialOperatingHoursRepository specialOperatingHoursRepository;
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
        log.debug("Getting weekly availability for lab {} starting week {}", labId, weekStart);
        
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> {
                    log.warn("Lab not found when fetching weekly availability: {}", labId);
                    return new IllegalArgumentException("Lab not found: " + labId);
                });

        // Normalize to Monday of the week
        if (weekStart == null) {
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else {
            weekStart = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        LocalDate weekEnd = weekStart.plusDays(6); // Sunday
        
        log.debug("Normalized week range: {} to {}", weekStart, weekEnd);

        // Get operating hours
        List<OperatingHoursResponse> operatingHours = getOperatingHours(labId, lab);
        log.debug("Retrieved {} operating hours entries for lab {}", operatingHours.size(), labId);

        // Get closed days for this week
        List<ClosedDayResponse> closedDays = getClosedDaysInRange(labId, weekStart, weekEnd);
        log.debug("Found {} closed days in range for lab {}", closedDays.size(), labId);

        // Get reservations for this week
        OffsetDateTime startDateTime = weekStart.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endDateTime = weekEnd.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        
        List<ReservationSummaryResponse> reservations = getReservationsInRange(labId, startDateTime, endDateTime);
        log.debug("Found {} reservations in range for lab {}", reservations.size(), labId);

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
        log.debug("Getting current availability for lab {}", labId);
        
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> {
                    log.warn("Lab not found when fetching current availability: {}", labId);
                    return new IllegalArgumentException("Lab not found: " + labId);
                });

        OffsetDateTime now = OffsetDateTime.now();
        
        // Check if lab is currently open
        boolean isOpen = isLabOpenAt(labId, lab, now);
        log.debug("Lab {} is currently {}", labId, isOpen ? "open" : "closed");

        // Get current APPROVED reservations only
        List<Reservation> currentReservations = reservationRepository.findCurrentReservations(
                labId, now, ReservationStatus.APPROVED);
        log.debug("Found {} current approved reservations for lab {}", currentReservations.size(), labId);

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
        log.debug("Getting workstations for lab {}", labId);
        
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> {
                    log.warn("Lab not found when fetching workstations: {}", labId);
                    return new IllegalArgumentException("Lab not found: " + labId);
                });

        List<Workstation> workstations = workstationRepository.findByLabId(labId);
        log.debug("Found {} workstations for lab {} ({})", workstations.size(), labId, lab.getName());

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
                .labName(lab.getName())
                .workstations(workstationResponses)
                .build();
    }

    // === Private helper methods ===

    private List<OperatingHoursResponse> getOperatingHours(Integer labId, Lab lab) {
        List<LabOperatingHours> labHours = operatingHoursRepository.findByLabId(labId);
        Integer buildingId = lab.getBuilding() != null ? lab.getBuilding().getId() : null;
        List<BuildingOperatingHours> buildingHours = buildingId != null
                ? buildingOperatingHoursRepository.findByBuildingId(buildingId)
                : List.of();

        List<OperatingHoursResponse> result = new ArrayList<>();
        for (int day = 0; day <= 6; day++) {
            int d = day;
            LabOperatingHours labEntry = labHours.stream().filter(h -> h.getDayOfWeek() == d).findFirst().orElse(null);
            if (labEntry != null) {
                result.add(OperatingHoursResponse.builder()
                        .dayOfWeek(day)
                        .open(labEntry.getOpenTime())
                        .close(labEntry.getCloseTime())
                        .closed(labEntry.getIsClosed())
                        .build());
                continue;
            }
            BuildingOperatingHours buildingEntry = buildingHours.stream().filter(h -> h.getDayOfWeek() == d).findFirst().orElse(null);
            if (buildingEntry != null) {
                result.add(OperatingHoursResponse.builder()
                        .dayOfWeek(day)
                        .open(buildingEntry.getOpenTime())
                        .close(buildingEntry.getCloseTime())
                        .closed(buildingEntry.getIsClosed())
                        .build());
                continue;
            }
            boolean isSunday = (day == 0);
            result.add(OperatingHoursResponse.builder()
                    .dayOfWeek(day)
                    .open(isSunday ? null : lab.getDefaultOpenTime())
                    .close(isSunday ? null : lab.getDefaultCloseTime())
                    .closed(isSunday)
                    .build());
        }
        return result;
    }

    private List<ClosedDayResponse> getClosedDaysInRange(Integer labId, LocalDate start, LocalDate end) {
        List<ClosedDayResponse> result = new ArrayList<>();

        // University + lab closed days (LabClosedDay: lab=null or lab=labId)
        List<LabClosedDay> specificClosures = closedDayRepository.findSpecificClosuresInRange(labId, start, end);
        List<LabClosedDay> recurringClosures = closedDayRepository.findRecurringClosures(labId);

        for (LabClosedDay closure : specificClosures) {
            if (closure.getSpecificDate() != null) {
                result.add(ClosedDayResponse.builder()
                        .date(closure.getSpecificDate())
                        .reason(closure.getReason())
                        .build());
            }
        }

        for (LabClosedDay recurring : recurringClosures) {
            LocalDate current = start;
            while (!current.isAfter(end)) {
                int javaDayValue = current.getDayOfWeek().getValue();
                int ourDayValue = javaDayValue == 7 ? 0 : javaDayValue;
                if (recurring.getRecurringDayOfWeek() != null && ourDayValue == recurring.getRecurringDayOfWeek()) {
                    result.add(ClosedDayResponse.builder()
                            .date(current)
                            .reason(recurring.getReason())
                            .build());
                }
                current = current.plusDays(1);
            }
        }

        // Building closed days (lab's building)
        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab != null && lab.getBuilding() != null) {
            Integer buildingId = lab.getBuilding().getId();
            List<BuildingClosedDay> buildingClosures = buildingClosedDayRepository.findByBuildingId(buildingId);
            for (BuildingClosedDay b : buildingClosures) {
                if (b.getSpecificDate() != null && !b.getSpecificDate().isBefore(start) && !b.getSpecificDate().isAfter(end)) {
                    result.add(ClosedDayResponse.builder()
                            .date(b.getSpecificDate())
                            .reason(b.getReason())
                            .build());
                }
                if (b.getRecurringDayOfWeek() != null) {
                    LocalDate current = start;
                    while (!current.isAfter(end)) {
                        int javaDayValue = current.getDayOfWeek().getValue();
                        int ourDayValue = javaDayValue == 7 ? 0 : javaDayValue;
                        if (ourDayValue == b.getRecurringDayOfWeek()) {
                            result.add(ClosedDayResponse.builder()
                                    .date(current)
                                    .reason(b.getReason())
                                    .build());
                        }
                        current = current.plusDays(1);
                    }
                }
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
        int javaDayValue = date.getDayOfWeek().getValue();
        int dayOfWeek = javaDayValue == 7 ? 0 : javaDayValue;

        // Closed days: university → building → lab
        if (closedDayRepository.isLabClosedOnDate(labId, date, dayOfWeek)) {
            return false;
        }
        if (lab.getBuilding() != null
                && buildingClosedDayRepository.isBuildingClosedOnDate(lab.getBuilding().getId(), date, dayOfWeek)) {
            return false;
        }

        // Operating hours: Special (lab) → Special (building) → Lab → Building → Default
        java.util.Optional<SpecialOperatingHours> labSpecial = specialOperatingHoursRepository
                .findByLabIdAndSpecificDate(labId, date);
        if (labSpecial.isPresent()) {
            SpecialOperatingHours s = labSpecial.get();
            if (Boolean.TRUE.equals(s.getIsClosed())) {
                return false;
            }
            if (s.getOpenTime() != null && s.getCloseTime() != null) {
                return !time.isBefore(s.getOpenTime()) && time.isBefore(s.getCloseTime());
            }
        }
        if (lab.getBuilding() != null) {
            java.util.Optional<SpecialOperatingHours> buildingSpecial = specialOperatingHoursRepository
                    .findByBuildingIdAndSpecificDate(lab.getBuilding().getId(), date);
            if (buildingSpecial.isPresent()) {
                SpecialOperatingHours s = buildingSpecial.get();
                if (Boolean.TRUE.equals(s.getIsClosed())) {
                    return false;
                }
                if (s.getOpenTime() != null && s.getCloseTime() != null) {
                    return !time.isBefore(s.getOpenTime()) && time.isBefore(s.getCloseTime());
                }
            }
        }

        // Regular lab operating hours
        LabOperatingHours labHours = operatingHoursRepository.findByLabIdAndDayOfWeek(labId, dayOfWeek)
                .orElse(null);
        if (labHours != null) {
            if (labHours.getIsClosed()) {
                return false;
            }
            return !time.isBefore(labHours.getOpenTime()) && time.isBefore(labHours.getCloseTime());
        }

        // Fall back to building operating hours
        if (lab.getBuilding() != null) {
            BuildingOperatingHours buildingHours = buildingOperatingHoursRepository
                    .findByBuildingIdAndDayOfWeek(lab.getBuilding().getId(), dayOfWeek)
                    .orElse(null);
            if (buildingHours != null) {
                if (buildingHours.getIsClosed()) {
                    return false;
                }
                return !time.isBefore(buildingHours.getOpenTime()) && time.isBefore(buildingHours.getCloseTime());
            }
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

