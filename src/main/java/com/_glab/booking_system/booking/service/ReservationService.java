package com._glab.booking_system.booking.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabOperatingHours;
import com._glab.booking_system.booking.model.Reservation;
import com._glab.booking_system.booking.model.ReservationStatus;
import com._glab.booking_system.booking.model.ReservationWorkstation;
import com._glab.booking_system.booking.model.Workstation;
import com._glab.booking_system.booking.repository.LabClosedDayRepository;
import com._glab.booking_system.booking.repository.LabOperatingHoursRepository;
import com._glab.booking_system.booking.repository.LabRepository;
import com._glab.booking_system.booking.repository.ReservationRepository;
import com._glab.booking_system.booking.repository.ReservationWorkstationRepository;
import com._glab.booking_system.booking.repository.WorkstationRepository;
import com._glab.booking_system.booking.request.CreateReservationRequest;
import com._glab.booking_system.booking.response.ReservationResponse;
import com._glab.booking_system.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final LabRepository labRepository;
    private final LabOperatingHoursRepository labOperatingHoursRepository;
    private final LabClosedDayRepository labClosedDayRepository;
    private final WorkstationRepository workstationRepository;
    private final ReservationWorkstationRepository reservationWorkstationRepository;

    /**
     * Get all reservations for a lab.
     */
    public List<Reservation> getReservationsByLabId(Integer labId) {
        return reservationRepository.findByLabId(labId);
    }

    /**
     * Get a reservation by ID.
     */
    public Optional<ReservationResponse> getReservationById(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .map(this::toReservationResponse);
    }

    /**
     * Get all reservations for a user.
     */
    public List<ReservationResponse> getUserReservations(Integer userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(this::toReservationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get user's reservations filtered by status.
     */
    public List<ReservationResponse> getUserReservationsByStatus(Integer userId, ReservationStatus status) {
        return reservationRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::toReservationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new reservation.
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, User user) {
        log.info("Creating reservation for user {} in lab {}", user.getEmail(), request.getLabId());

        // Validate lab exists
        Lab lab = labRepository.findById(request.getLabId())
                .orElseThrow(() -> new IllegalArgumentException("Lab not found: " + request.getLabId()));

        // Validate times
        validateTimes(request);

        // Validate operating hours
        validateOperatingHours(lab, request.getStartTime(), request.getEndTime());

        // Validate lab is not closed on this date
        validateLabNotClosed(lab.getId(), request.getStartTime().toLocalDate());

        // Validate workstations if not whole lab
        List<Workstation> workstations = new ArrayList<>();
        if (!Boolean.TRUE.equals(request.getWholeLab())) {
            workstations = validateAndGetWorkstations(lab.getId(), request.getWorkstationIds());
        }

        // Create reservation with PENDING status
        Reservation reservation = new Reservation();
        reservation.setLab(lab);
        reservation.setUser(user);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setDescription(request.getDescription());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setWholeLab(Boolean.TRUE.equals(request.getWholeLab()));

        Reservation savedReservation = reservationRepository.save(reservation);

        // Save workstation assignments
        if (!workstations.isEmpty()) {
            for (Workstation workstation : workstations) {
                ReservationWorkstation rw = new ReservationWorkstation(savedReservation, workstation);
                reservationWorkstationRepository.save(rw);
            }
        }

        log.info("Created reservation {} for user {} in lab {}", 
                savedReservation.getId(), user.getEmail(), lab.getName());

        return toReservationResponse(savedReservation, 
                workstations.stream().map(Workstation::getId).collect(Collectors.toList()));
    }

    // === Validation Methods ===

    private void validateTimes(CreateReservationRequest request) {
        OffsetDateTime now = OffsetDateTime.now();

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        if (request.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Start time must be in the future");
        }

        if (Duration.between(request.getStartTime(), request.getEndTime()).toMinutes() < 15) {
            throw new IllegalArgumentException("Reservation duration must be at least 15 minutes");
        }
    }

    private void validateOperatingHours(Lab lab, OffsetDateTime startTime, OffsetDateTime endTime) {
        // Convert Java DayOfWeek (1=Mon to 7=Sun) to our format (0=Sun to 6=Sat)
        int javaDayValue = startTime.getDayOfWeek().getValue();
        int dayOfWeek = (javaDayValue == 7) ? 0 : javaDayValue;

        Optional<LabOperatingHours> operatingHoursOpt = labOperatingHoursRepository
                .findByLabIdAndDayOfWeek(lab.getId(), dayOfWeek);

        if (operatingHoursOpt.isPresent()) {
            LabOperatingHours operatingHours = operatingHoursOpt.get();

            if (operatingHours.getIsClosed()) {
                throw new IllegalArgumentException("Lab is closed on this day");
            }

            LocalTime startLocalTime = startTime.toLocalTime();
            LocalTime endLocalTime = endTime.toLocalTime();

            if (startLocalTime.isBefore(operatingHours.getOpenTime()) || 
                endLocalTime.isAfter(operatingHours.getCloseTime())) {
                throw new IllegalArgumentException("Reservation time must be within operating hours (" 
                        + operatingHours.getOpenTime() + " - " + operatingHours.getCloseTime() + ")");
            }
        } else {
            // No specific hours defined - use lab defaults if available
            if (lab.getDefaultOpenTime() != null && lab.getDefaultCloseTime() != null) {
                LocalTime startLocalTime = startTime.toLocalTime();
                LocalTime endLocalTime = endTime.toLocalTime();

                if (startLocalTime.isBefore(lab.getDefaultOpenTime()) || 
                    endLocalTime.isAfter(lab.getDefaultCloseTime())) {
                    throw new IllegalArgumentException("Reservation time must be within operating hours (" 
                            + lab.getDefaultOpenTime() + " - " + lab.getDefaultCloseTime() + ")");
                }
            }
            
            // Check if it's Sunday (default closed)
            if (dayOfWeek == 0) {
                throw new IllegalArgumentException("Lab is closed on Sundays by default");
            }
        }
    }

    private void validateLabNotClosed(Integer labId, LocalDate date) {
        int javaDayValue = date.getDayOfWeek().getValue();
        int dayOfWeek = (javaDayValue == 7) ? 0 : javaDayValue;

        if (labClosedDayRepository.isLabClosedOnDate(labId, date, dayOfWeek)) {
            throw new IllegalArgumentException("Lab is closed on " + date);
        }
    }

    private List<Workstation> validateAndGetWorkstations(Integer labId, List<Integer> workstationIds) {
        if (workstationIds == null || workstationIds.isEmpty()) {
            throw new IllegalArgumentException("At least one workstation must be selected, or choose 'whole lab'");
        }

        List<Workstation> workstations = new ArrayList<>();
        for (Integer wsId : workstationIds) {
            Workstation ws = workstationRepository.findById(wsId)
                    .orElseThrow(() -> new IllegalArgumentException("Workstation not found: " + wsId));
            
            if (!ws.getLab().getId().equals(labId)) {
                throw new IllegalArgumentException("Workstation " + wsId + " does not belong to this lab");
            }
            
            if (!ws.getActive()) {
                throw new IllegalArgumentException("Workstation " + ws.getIdentifier() + " is not active");
            }
            
            workstations.add(ws);
        }
        return workstations;
    }

    // === Response Mapping ===

    private ReservationResponse toReservationResponse(Reservation reservation) {
        List<Integer> workstationIds = reservationWorkstationRepository
                .findWorkstationIdsByReservationId(reservation.getId());
        return toReservationResponse(reservation, workstationIds);
    }

    private ReservationResponse toReservationResponse(Reservation reservation, List<Integer> workstationIds) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .labId(reservation.getLab().getId())
                .labName(reservation.getLab().getName())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .description(reservation.getDescription())
                .status(reservation.getStatus())
                .wholeLab(reservation.getWholeLab())
                .workstationIds(workstationIds)
                .recurringGroupId(reservation.getRecurringGroupId())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
