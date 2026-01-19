package com._glab.booking_system.booking.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com._glab.booking_system.auth.service.EmailService;
import com._glab.booking_system.booking.exception.*;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabManager;
import com._glab.booking_system.booking.model.LabOperatingHours;
import com._glab.booking_system.booking.model.RecurrenceType;
import com._glab.booking_system.booking.model.RecurringPattern;
import com._glab.booking_system.booking.model.Reservation;
import com._glab.booking_system.booking.model.ReservationStatus;
import com._glab.booking_system.booking.model.ReservationWorkstation;
import com._glab.booking_system.booking.model.Workstation;
import com._glab.booking_system.booking.repository.LabClosedDayRepository;
import com._glab.booking_system.booking.repository.LabManagerRepository;
import com._glab.booking_system.booking.repository.LabOperatingHoursRepository;
import com._glab.booking_system.booking.repository.LabRepository;
import com._glab.booking_system.booking.repository.RecurringPatternRepository;
import com._glab.booking_system.booking.repository.ReservationRepository;
import com._glab.booking_system.booking.repository.ReservationWorkstationRepository;
import com._glab.booking_system.booking.repository.WorkstationRepository;
import com._glab.booking_system.booking.request.CreateReservationRequest;
import com._glab.booking_system.booking.response.RecurringReservationResponse;
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
    private final RecurringPatternRepository recurringPatternRepository;
    private final LabManagerRepository labManagerRepository;
    private final EmailService emailService;

    /**
     * Get all reservations for a lab.
     */
    public List<Reservation> getReservationsByLabId(Integer labId) {
        log.debug("Fetching all reservations for lab {}", labId);
        List<Reservation> reservations = reservationRepository.findByLabId(labId);
        log.debug("Found {} reservations for lab {}", reservations.size(), labId);
        return reservations;
    }

    /**
     * Get a reservation by ID.
     */
    public Optional<ReservationResponse> getReservationById(UUID reservationId) {
        log.debug("Fetching reservation by ID: {}", reservationId);
        Optional<ReservationResponse> response = reservationRepository.findById(reservationId)
                .map(this::toReservationResponse);
        if (response.isPresent()) {
            log.debug("Reservation {} found", reservationId);
        } else {
            log.debug("Reservation {} not found", reservationId);
        }
        return response;
    }

    /**
     * Get all reservations for a user.
     */
    public List<ReservationResponse> getUserReservations(Integer userId) {
        log.debug("Fetching all reservations for user {}", userId);
        List<ReservationResponse> reservations = reservationRepository.findByUserId(userId).stream()
                .map(this::toReservationResponse)
                .collect(Collectors.toList());
        log.debug("Found {} reservations for user {}", reservations.size(), userId);
        return reservations;
    }

    /**
     * Get user's reservations filtered by status.
     */
    public List<ReservationResponse> getUserReservationsByStatus(Integer userId, ReservationStatus status) {
        log.debug("Fetching reservations for user {} with status {}", userId, status);
        List<ReservationResponse> reservations = reservationRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::toReservationResponse)
                .collect(Collectors.toList());
        log.debug("Found {} {} reservations for user {}", reservations.size(), status, userId);
        return reservations;
    }

    /**
     * Create a new reservation (single or recurring).
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, User user) {
        // If recurring, delegate to recurring method
        if (request.getRecurring() != null) {
            RecurringReservationResponse recurringResponse = createRecurringReservation(request, user);
            // Return the first reservation
            return recurringResponse.getReservations().get(0);
        }

        return createSingleReservation(request, user, null);
    }

    /**
     * Create a recurring reservation series.
     */
    @Transactional
    public RecurringReservationResponse createRecurringReservation(CreateReservationRequest request, User user) {
        log.info("Creating recurring reservation for user {} in lab {}", user.getEmail(), request.getLabId());

        CreateReservationRequest.RecurringConfig recurringConfig = request.getRecurring();
        if (recurringConfig == null) {
            throw new InvalidRecurringPatternException("Recurring configuration is required");
        }

        // Parse pattern type
        RecurrenceType patternType;
        try {
            patternType = RecurrenceType.valueOf(recurringConfig.getPatternType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRecurringPatternException("Invalid pattern type: " + recurringConfig.getPatternType());
        }

        // Generate a group ID for all occurrences
        UUID recurringGroupId = UUID.randomUUID();

        // Calculate occurrence dates
        List<LocalDate> occurrenceDates = calculateOccurrenceDates(
                request.getStartTime().toLocalDate(),
                patternType,
                recurringConfig.getIntervalDays(),
                recurringConfig.getEndDate() != null ? LocalDate.parse(recurringConfig.getEndDate()) : null,
                recurringConfig.getOccurrences()
        );

        if (occurrenceDates.isEmpty()) {
            throw new NoValidOccurrencesException("No valid occurrence dates could be generated");
        }

        log.info("Generating {} occurrences for recurring reservation", occurrenceDates.size());

        // Create reservations for each occurrence
        List<ReservationResponse> reservationResponses = new ArrayList<>();
        LocalTime startTime = request.getStartTime().toLocalTime();
        LocalTime endTime = request.getEndTime().toLocalTime();
        ZoneOffset offset = request.getStartTime().getOffset();

        for (LocalDate date : occurrenceDates) {
            OffsetDateTime occurrenceStart = OffsetDateTime.of(date, startTime, offset);
            OffsetDateTime occurrenceEnd = OffsetDateTime.of(date, endTime, offset);

            // Create a modified request for this occurrence
            CreateReservationRequest occurrenceRequest = CreateReservationRequest.builder()
                    .labId(request.getLabId())
                    .startTime(occurrenceStart)
                    .endTime(occurrenceEnd)
                    .description(request.getDescription())
                    .wholeLab(request.getWholeLab())
                    .workstationIds(request.getWorkstationIds())
                    .build();

            try {
                ReservationResponse response = createSingleReservation(occurrenceRequest, user, recurringGroupId);
                reservationResponses.add(response);
            } catch (IllegalArgumentException e) {
                // Log and skip invalid dates (e.g., lab closed)
                log.warn("Skipping occurrence on {}: {}", date, e.getMessage());
            }
        }

        if (reservationResponses.isEmpty()) {
            throw new NoValidOccurrencesException();
        }

        // Save the recurring pattern
        RecurringPattern pattern = new RecurringPattern();
        pattern.setRecurringGroupId(recurringGroupId);
        pattern.setPatternType(patternType);
        pattern.setIntervalDays(getIntervalDays(patternType, recurringConfig.getIntervalDays()));
        pattern.setEndDate(recurringConfig.getEndDate() != null ? LocalDate.parse(recurringConfig.getEndDate()) : null);
        pattern.setOccurrences(recurringConfig.getOccurrences());
        recurringPatternRepository.save(pattern);

        log.info("Created recurring reservation group {} with {} occurrences", 
                recurringGroupId, reservationResponses.size());

        // Send email notifications for the recurring series
        Lab lab = labRepository.findById(request.getLabId()).orElse(null);
        if (lab != null && !reservationResponses.isEmpty()) {
            ReservationResponse firstRes = reservationResponses.get(0);
            Reservation firstReservation = reservationRepository.findById(firstRes.getId()).orElse(null);
            if (firstReservation != null) {
                sendReservationEmails(firstReservation, user, lab, true, reservationResponses.size());
            }
        }

        return RecurringReservationResponse.builder()
                .recurringGroupId(recurringGroupId)
                .patternType(patternType)
                .totalOccurrences(reservationResponses.size())
                .reservations(reservationResponses)
                .build();
    }

    /**
     * Create a single reservation (internal method).
     */
    private ReservationResponse createSingleReservation(CreateReservationRequest request, User user, UUID recurringGroupId) {
        log.info("Creating reservation for user {} in lab {}", user.getEmail(), request.getLabId());

        // Validate lab exists
        Lab lab = labRepository.findById(request.getLabId())
                .orElseThrow(() -> new LabNotFoundException(request.getLabId()));

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
        reservation.setRecurringGroupId(recurringGroupId);

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

        // Send email notifications (only for non-recurring or first occurrence)
        if (recurringGroupId == null) {
            sendReservationEmails(savedReservation, user, lab, false, 1);
        }

        return toReservationResponse(savedReservation, 
                workstations.stream().map(Workstation::getId).collect(Collectors.toList()));
    }

    // === Recurring Helper Methods ===

    private List<LocalDate> calculateOccurrenceDates(
            LocalDate startDate,
            RecurrenceType patternType,
            Integer customIntervalDays,
            LocalDate endDate,
            Integer occurrences) {

        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;
        int intervalDays = getIntervalDays(patternType, customIntervalDays);
        int maxOccurrences = occurrences != null ? occurrences : 52; // Default max 1 year of weekly

        while (dates.size() < maxOccurrences) {
            // Check end date constraint
            if (endDate != null && current.isAfter(endDate)) {
                break;
            }

            dates.add(current);

            // Calculate next occurrence
            if (patternType == RecurrenceType.MONTHLY) {
                current = current.plusMonths(1);
            } else {
                current = current.plusDays(intervalDays);
            }
        }

        return dates;
    }

    private int getIntervalDays(RecurrenceType patternType, Integer customIntervalDays) {
        return switch (patternType) {
            case WEEKLY -> 7;
            case BIWEEKLY -> 14;
            case MONTHLY -> 30; // Approximate, actual calculation uses plusMonths
            case CUSTOM -> customIntervalDays != null ? customIntervalDays : 7;
        };
    }

    // === Validation Methods ===

    private void validateTimes(CreateReservationRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        log.debug("Validating reservation times: start={}, end={}", request.getStartTime(), request.getEndTime());

        if (request.getStartTime().isAfter(request.getEndTime())) {
            log.warn("Invalid time range: start {} is after end {}", request.getStartTime(), request.getEndTime());
            throw new InvalidReservationTimeException("Start time must be before end time");
        }

        if (request.getStartTime().isBefore(now)) {
            log.warn("Invalid time: start {} is in the past (now: {})", request.getStartTime(), now);
            throw new InvalidReservationTimeException("Start time must be in the future");
        }

        long durationMinutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (durationMinutes < 15) {
            log.warn("Invalid duration: {} minutes (minimum 15)", durationMinutes);
            throw new InvalidReservationTimeException("Reservation duration must be at least 15 minutes");
        }
        
        log.debug("Time validation passed: duration {} minutes", durationMinutes);
    }

    private void validateOperatingHours(Lab lab, OffsetDateTime startTime, OffsetDateTime endTime) {
        // Convert Java DayOfWeek (1=Mon to 7=Sun) to our format (0=Sun to 6=Sat)
        int javaDayValue = startTime.getDayOfWeek().getValue();
        int dayOfWeek = (javaDayValue == 7) ? 0 : javaDayValue;
        
        log.debug("Validating operating hours for lab {} on day {} ({})", 
                lab.getId(), dayOfWeek, startTime.getDayOfWeek());

        Optional<LabOperatingHours> operatingHoursOpt = labOperatingHoursRepository
                .findByLabIdAndDayOfWeek(lab.getId(), dayOfWeek);

        if (operatingHoursOpt.isPresent()) {
            LabOperatingHours operatingHours = operatingHoursOpt.get();
            log.debug("Found specific operating hours: {} - {}, closed: {}", 
                    operatingHours.getOpenTime(), operatingHours.getCloseTime(), operatingHours.getIsClosed());

            if (operatingHours.getIsClosed()) {
                log.warn("Lab {} is closed on day {}", lab.getId(), dayOfWeek);
                throw new LabClosedException("Lab is closed on this day");
            }

            LocalTime startLocalTime = startTime.toLocalTime();
            LocalTime endLocalTime = endTime.toLocalTime();

            if (startLocalTime.isBefore(operatingHours.getOpenTime()) || 
                endLocalTime.isAfter(operatingHours.getCloseTime())) {
                log.warn("Reservation {} - {} is outside operating hours {} - {}", 
                        startLocalTime, endLocalTime, operatingHours.getOpenTime(), operatingHours.getCloseTime());
                throw new OutsideOperatingHoursException("Reservation time must be within operating hours (" 
                        + operatingHours.getOpenTime() + " - " + operatingHours.getCloseTime() + ")");
            }
        } else {
            log.debug("No specific operating hours found, using lab defaults");
            // No specific hours defined - use lab defaults if available
            if (lab.getDefaultOpenTime() != null && lab.getDefaultCloseTime() != null) {
                LocalTime startLocalTime = startTime.toLocalTime();
                LocalTime endLocalTime = endTime.toLocalTime();

                if (startLocalTime.isBefore(lab.getDefaultOpenTime()) || 
                    endLocalTime.isAfter(lab.getDefaultCloseTime())) {
                    log.warn("Reservation {} - {} is outside default operating hours {} - {}", 
                            startLocalTime, endLocalTime, lab.getDefaultOpenTime(), lab.getDefaultCloseTime());
                    throw new OutsideOperatingHoursException("Reservation time must be within operating hours (" 
                            + lab.getDefaultOpenTime() + " - " + lab.getDefaultCloseTime() + ")");
                }
            }
            
            // Check if it's Sunday (default closed)
            if (dayOfWeek == 0) {
                log.warn("Lab {} is closed on Sundays by default", lab.getId());
                throw new LabClosedException("Lab is closed on Sundays by default");
            }
        }
        
        log.debug("Operating hours validation passed");
    }

    private void validateLabNotClosed(Integer labId, LocalDate date) {
        int javaDayValue = date.getDayOfWeek().getValue();
        int dayOfWeek = (javaDayValue == 7) ? 0 : javaDayValue;
        
        log.debug("Checking if lab {} is closed on {} (day {})", labId, date, dayOfWeek);

        if (labClosedDayRepository.isLabClosedOnDate(labId, date, dayOfWeek)) {
            log.warn("Lab {} is closed on {}", labId, date);
            throw new LabClosedException("Lab is closed on " + date);
        }
        
        log.debug("Lab {} is open on {}", labId, date);
    }

    private List<Workstation> validateAndGetWorkstations(Integer labId, List<Integer> workstationIds) {
        log.debug("Validating workstations {} for lab {}", workstationIds, labId);
        
        if (workstationIds == null || workstationIds.isEmpty()) {
            log.warn("No workstations selected for non-whole-lab reservation");
            throw new NoWorkstationsSelectedException();
        }

        List<Workstation> workstations = new ArrayList<>();
        for (Integer wsId : workstationIds) {
            Workstation ws = workstationRepository.findById(wsId)
                    .orElseThrow(() -> {
                        log.warn("Workstation not found: {}", wsId);
                        return new WorkstationNotFoundException(wsId);
                    });
            
            if (!ws.getLab().getId().equals(labId)) {
                log.warn("Workstation {} belongs to lab {} but was requested for lab {}", 
                        wsId, ws.getLab().getId(), labId);
                throw new WorkstationNotInLabException(wsId, labId);
            }
            
            if (!ws.getActive()) {
                log.warn("Workstation {} ({}) is inactive", wsId, ws.getIdentifier());
                throw new WorkstationInactiveException(ws.getIdentifier());
            }
            
            workstations.add(ws);
        }
        
        log.debug("All {} workstations validated successfully", workstations.size());
        return workstations;
    }

    // === Email Notifications ===

    /**
     * Send email notifications for a new reservation.
     * Notifies the user and lab manager(s).
     */
    private void sendReservationEmails(Reservation reservation, User user, Lab lab, 
                                        boolean isRecurring, int occurrenceCount) {
        try {
            String userName = user.getFirstName() + " " + user.getLastName();
            String startTimeFormatted = reservation.getStartTime().toString();
            String endTimeFormatted = reservation.getEndTime().toString();

            // Send confirmation to user
            emailService.sendReservationSubmittedEmail(
                    user.getEmail(),
                    userName,
                    lab.getName(),
                    startTimeFormatted,
                    endTimeFormatted,
                    isRecurring,
                    occurrenceCount
            );

            // Send notification to lab manager(s)
            List<LabManager> managers = labManagerRepository.findByLab(lab);
            for (LabManager manager : managers) {
                User managerUser = manager.getUser();
                String managerName = managerUser.getFirstName() + " " + managerUser.getLastName();
                
                emailService.sendNewReservationRequestEmail(
                        managerUser.getEmail(),
                        managerName,
                        lab.getName(),
                        userName,
                        startTimeFormatted,
                        endTimeFormatted,
                        isRecurring,
                        occurrenceCount
                );
            }

            log.info("Sent reservation notification emails for reservation {} (managers notified: {})", 
                    reservation.getId(), managers.size());
        } catch (Exception e) {
            // Log but don't fail the reservation creation if email fails
            log.error("Failed to send reservation notification emails: {}", e.getMessage());
        }
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
