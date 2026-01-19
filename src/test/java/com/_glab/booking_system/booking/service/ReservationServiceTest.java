package com._glab.booking_system.booking.service;

import com._glab.booking_system.auth.service.EmailService;
import com._glab.booking_system.booking.exception.*;
import com._glab.booking_system.booking.model.*;
import com._glab.booking_system.booking.repository.*;
import com._glab.booking_system.booking.request.CreateReservationRequest;
import com._glab.booking_system.booking.response.RecurringReservationResponse;
import com._glab.booking_system.booking.response.ReservationResponse;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private LabRepository labRepository;
    @Mock
    private LabOperatingHoursRepository labOperatingHoursRepository;
    @Mock
    private LabClosedDayRepository labClosedDayRepository;
    @Mock
    private WorkstationRepository workstationRepository;
    @Mock
    private ReservationWorkstationRepository reservationWorkstationRepository;
    @Mock
    private RecurringPatternRepository recurringPatternRepository;
    @Mock
    private LabManagerRepository labManagerRepository;
    @Mock
    private EmailService emailService;

    private ReservationService reservationService;

    private User testUser;
    private Lab testLab;
    private Building testBuilding;
    private Workstation testWorkstation;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                labRepository,
                labOperatingHoursRepository,
                labClosedDayRepository,
                workstationRepository,
                reservationWorkstationRepository,
                recurringPatternRepository,
                labManagerRepository,
                emailService
        );

        // Set up test user
        Role professorRole = new Role();
        professorRole.setId(1);
        professorRole.setName(RoleName.PROFESSOR);

        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("professor@example.com");
        testUser.setUsername("professor");
        testUser.setFirstName("Test");
        testUser.setLastName("Professor");
        testUser.setRole(professorRole);

        // Set up test building
        testBuilding = new Building();
        testBuilding.setId(1);
        testBuilding.setName("Test Building");

        // Set up test lab
        testLab = new Lab();
        testLab.setId(1);
        testLab.setName("Test Lab");
        testLab.setBuilding(testBuilding);
        testLab.setDefaultOpenTime(LocalTime.of(8, 0));
        testLab.setDefaultCloseTime(LocalTime.of(20, 0));

        // Set up test workstation
        testWorkstation = new Workstation();
        testWorkstation.setId(1);
        testWorkstation.setLab(testLab);
        testWorkstation.setIdentifier("WS-001");
        testWorkstation.setActive(true);
    }

    @Nested
    @DisplayName("Create Single Reservation Tests")
    class CreateSingleReservationTests {

        @Test
        @DisplayName("Should create reservation successfully with workstations")
        void shouldCreateReservationSuccessfully() {
            // Given
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime tomorrowEnd = tomorrow.plusHours(2);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrowEnd)
                    .description("Test reservation")
                    .wholeLab(false)
                    .workstationIds(List.of(1))
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty()); // Use lab defaults
            when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
            when(workstationRepository.findById(1)).thenReturn(Optional.of(testWorkstation));
            when(labManagerRepository.findByLab(testLab)).thenReturn(new ArrayList<>());

            Reservation savedReservation = new Reservation();
            savedReservation.setId(UUID.randomUUID());
            savedReservation.setLab(testLab);
            savedReservation.setUser(testUser);
            savedReservation.setStartTime(tomorrow);
            savedReservation.setEndTime(tomorrowEnd);
            savedReservation.setStatus(ReservationStatus.PENDING);
            savedReservation.setWholeLab(false);

            when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

            // When
            ReservationResponse response = reservationService.createReservation(request, testUser);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getLabId()).isEqualTo(1);
            assertThat(response.getLabName()).isEqualTo("Test Lab");
            assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(response.getWholeLab()).isFalse();

            // Verify reservation was saved
            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            Reservation captured = captor.getValue();
            assertThat(captured.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(captured.getUser()).isEqualTo(testUser);

            // Verify workstation assignment was saved
            verify(reservationWorkstationRepository).save(any(ReservationWorkstation.class));
        }

        @Test
        @DisplayName("Should create whole-lab reservation")
        void shouldCreateWholeLabReservation() {
            // Given
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime tomorrowEnd = tomorrow.plusHours(2);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrowEnd)
                    .description("Whole lab reservation")
                    .wholeLab(true)
                    .workstationIds(null)
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
            when(labManagerRepository.findByLab(testLab)).thenReturn(new ArrayList<>());

            Reservation savedReservation = new Reservation();
            savedReservation.setId(UUID.randomUUID());
            savedReservation.setLab(testLab);
            savedReservation.setUser(testUser);
            savedReservation.setStartTime(tomorrow);
            savedReservation.setEndTime(tomorrowEnd);
            savedReservation.setStatus(ReservationStatus.PENDING);
            savedReservation.setWholeLab(true);

            when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

            // When
            ReservationResponse response = reservationService.createReservation(request, testUser);

            // Then
            assertThat(response.getWholeLab()).isTrue();
            verify(reservationWorkstationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw LabNotFoundException when lab doesn't exist")
        void shouldThrowLabNotFoundWhenLabMissing() {
            // Given
            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(999)
                    .startTime(OffsetDateTime.now().plusDays(1))
                    .endTime(OffsetDateTime.now().plusDays(1).plusHours(2))
                    .build();

            when(labRepository.findById(999)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(LabNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Should throw InvalidReservationTimeException when start is after end")
        void shouldThrowWhenStartAfterEnd() {
            // Given
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(14).withMinute(0);
            OffsetDateTime tomorrowEarlier = tomorrow.minusHours(2);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrowEarlier) // End before start
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(InvalidReservationTimeException.class)
                    .hasMessageContaining("before");
        }

        @Test
        @DisplayName("Should throw InvalidReservationTimeException when start is in the past")
        void shouldThrowWhenStartInPast() {
            // Given
            OffsetDateTime yesterday = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(yesterday)
                    .endTime(yesterday.plusHours(2))
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(InvalidReservationTimeException.class)
                    .hasMessageContaining("future");
        }

        @Test
        @DisplayName("Should throw InvalidReservationTimeException when duration is too short")
        void shouldThrowWhenDurationTooShort() {
            // Given
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusMinutes(10)) // Only 10 minutes
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(InvalidReservationTimeException.class)
                    .hasMessageContaining("15 minutes");
        }

        @Test
        @DisplayName("Should throw OutsideOperatingHoursException when outside hours")
        void shouldThrowWhenOutsideOperatingHours() {
            // Given - Lab is open 8-20, request is 6-8 AM
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(6).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .wholeLab(true)
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty()); // Use lab defaults (8-20)

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(OutsideOperatingHoursException.class)
                    .hasMessageContaining("operating hours");
        }

        @Test
        @DisplayName("Should throw LabClosedException when lab is closed")
        void shouldThrowWhenLabClosed() {
            // Given
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .wholeLab(true)
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(LabClosedException.class);
        }

        @Test
        @DisplayName("Should throw NoWorkstationsSelectedException when no workstations for non-whole-lab")
        void shouldThrowWhenNoWorkstationsSelected() {
            // Given
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .wholeLab(false)
                    .workstationIds(null) // No workstations
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(NoWorkstationsSelectedException.class);
        }

        @Test
        @DisplayName("Should throw WorkstationNotFoundException when workstation doesn't exist")
        void shouldThrowWhenWorkstationNotFound() {
            // Given
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .wholeLab(false)
                    .workstationIds(List.of(999))
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
            when(workstationRepository.findById(999)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(WorkstationNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Should throw WorkstationNotInLabException when workstation belongs to different lab")
        void shouldThrowWhenWorkstationInDifferentLab() {
            // Given
            Lab otherLab = new Lab();
            otherLab.setId(2);
            otherLab.setName("Other Lab");

            Workstation otherWorkstation = new Workstation();
            otherWorkstation.setId(2);
            otherWorkstation.setLab(otherLab);
            otherWorkstation.setIdentifier("WS-OTHER");
            otherWorkstation.setActive(true);

            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1) // Requesting lab 1
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .wholeLab(false)
                    .workstationIds(List.of(2)) // But workstation 2 is in lab 2
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
            when(workstationRepository.findById(2)).thenReturn(Optional.of(otherWorkstation));

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(WorkstationNotInLabException.class);
        }

        @Test
        @DisplayName("Should throw WorkstationInactiveException when workstation is inactive")
        void shouldThrowWhenWorkstationInactive() {
            // Given
            Workstation inactiveWorkstation = new Workstation();
            inactiveWorkstation.setId(3);
            inactiveWorkstation.setLab(testLab);
            inactiveWorkstation.setIdentifier("WS-INACTIVE");
            inactiveWorkstation.setActive(false);

            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .wholeLab(false)
                    .workstationIds(List.of(3))
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
            when(workstationRepository.findById(3)).thenReturn(Optional.of(inactiveWorkstation));

            // When/Then
            assertThatThrownBy(() -> reservationService.createReservation(request, testUser))
                    .isInstanceOf(WorkstationInactiveException.class)
                    .hasMessageContaining("WS-INACTIVE");
        }
    }

    @Nested
    @DisplayName("Create Recurring Reservation Tests")
    class CreateRecurringReservationTests {

        @Test
        @DisplayName("Should create weekly recurring reservation")
        void shouldCreateWeeklyRecurringReservation() {
            // Given
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest.RecurringConfig recurringConfig = CreateReservationRequest.RecurringConfig.builder()
                    .patternType("WEEKLY")
                    .occurrences(4)
                    .build();

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .description("Weekly meeting")
                    .wholeLab(true)
                    .recurring(recurringConfig)
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
            when(labManagerRepository.findByLab(testLab)).thenReturn(new ArrayList<>());

            // Mock saving each reservation
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation r = invocation.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });

            // When
            RecurringReservationResponse response = reservationService.createRecurringReservation(request, testUser);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRecurringGroupId()).isNotNull();
            assertThat(response.getPatternType()).isEqualTo(RecurrenceType.WEEKLY);
            assertThat(response.getTotalOccurrences()).isEqualTo(4);
            assertThat(response.getReservations()).hasSize(4);

            // Verify pattern was saved
            verify(recurringPatternRepository).save(any(RecurringPattern.class));
        }

        @Test
        @DisplayName("Should throw InvalidRecurringPatternException for invalid pattern type")
        void shouldThrowWhenInvalidPatternType() {
            // Given
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest.RecurringConfig recurringConfig = CreateReservationRequest.RecurringConfig.builder()
                    .patternType("INVALID")
                    .occurrences(4)
                    .build();

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .wholeLab(true)
                    .recurring(recurringConfig)
                    .build();

            // When/Then
            assertThatThrownBy(() -> reservationService.createRecurringReservation(request, testUser))
                    .isInstanceOf(InvalidRecurringPatternException.class)
                    .hasMessageContaining("INVALID");
        }

        @Test
        @DisplayName("Should throw InvalidRecurringPatternException when config is null")
        void shouldThrowWhenRecurringConfigNull() {
            // Given
            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(1)
                    .startTime(OffsetDateTime.now().plusDays(1))
                    .endTime(OffsetDateTime.now().plusDays(1).plusHours(2))
                    .wholeLab(true)
                    .recurring(null)
                    .build();

            // When/Then
            assertThatThrownBy(() -> reservationService.createRecurringReservation(request, testUser))
                    .isInstanceOf(InvalidRecurringPatternException.class)
                    .hasMessageContaining("required");
        }
    }

    @Nested
    @DisplayName("Get Reservations Tests")
    class GetReservationsTests {

        @Test
        @DisplayName("Should get reservation by ID")
        void shouldGetReservationById() {
            // Given
            UUID reservationId = UUID.randomUUID();
            Reservation reservation = new Reservation();
            reservation.setId(reservationId);
            reservation.setLab(testLab);
            reservation.setUser(testUser);
            reservation.setStartTime(OffsetDateTime.now());
            reservation.setEndTime(OffsetDateTime.now().plusHours(2));
            reservation.setStatus(ReservationStatus.PENDING);

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
            when(reservationWorkstationRepository.findWorkstationIdsByReservationId(reservationId))
                    .thenReturn(List.of(1, 2));

            // When
            Optional<ReservationResponse> response = reservationService.getReservationById(reservationId);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().getId()).isEqualTo(reservationId);
            assertThat(response.get().getLabName()).isEqualTo("Test Lab");
            assertThat(response.get().getWorkstationIds()).containsExactly(1, 2);
        }

        @Test
        @DisplayName("Should return empty when reservation not found")
        void shouldReturnEmptyWhenReservationNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(reservationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<ReservationResponse> response = reservationService.getReservationById(nonExistentId);

            // Then
            assertThat(response).isEmpty();
        }

        @Test
        @DisplayName("Should get user reservations")
        void shouldGetUserReservations() {
            // Given
            Reservation reservation1 = new Reservation();
            reservation1.setId(UUID.randomUUID());
            reservation1.setLab(testLab);
            reservation1.setUser(testUser);
            reservation1.setStartTime(OffsetDateTime.now());
            reservation1.setEndTime(OffsetDateTime.now().plusHours(2));
            reservation1.setStatus(ReservationStatus.PENDING);

            Reservation reservation2 = new Reservation();
            reservation2.setId(UUID.randomUUID());
            reservation2.setLab(testLab);
            reservation2.setUser(testUser);
            reservation2.setStartTime(OffsetDateTime.now().plusDays(1));
            reservation2.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(2));
            reservation2.setStatus(ReservationStatus.APPROVED);

            when(reservationRepository.findByUserId(1)).thenReturn(List.of(reservation1, reservation2));
            when(reservationWorkstationRepository.findWorkstationIdsByReservationId(any()))
                    .thenReturn(new ArrayList<>());

            // When
            List<ReservationResponse> responses = reservationService.getUserReservations(1);

            // Then
            assertThat(responses).hasSize(2);
        }

        @Test
        @DisplayName("Should get user reservations by status")
        void shouldGetUserReservationsByStatus() {
            // Given
            Reservation pendingReservation = new Reservation();
            pendingReservation.setId(UUID.randomUUID());
            pendingReservation.setLab(testLab);
            pendingReservation.setUser(testUser);
            pendingReservation.setStartTime(OffsetDateTime.now());
            pendingReservation.setEndTime(OffsetDateTime.now().plusHours(2));
            pendingReservation.setStatus(ReservationStatus.PENDING);

            when(reservationRepository.findByUserIdAndStatus(1, ReservationStatus.PENDING))
                    .thenReturn(List.of(pendingReservation));
            when(reservationWorkstationRepository.findWorkstationIdsByReservationId(any()))
                    .thenReturn(new ArrayList<>());

            // When
            List<ReservationResponse> responses = reservationService.getUserReservationsByStatus(1, ReservationStatus.PENDING);

            // Then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getStatus()).isEqualTo(ReservationStatus.PENDING);
        }
    }
}
