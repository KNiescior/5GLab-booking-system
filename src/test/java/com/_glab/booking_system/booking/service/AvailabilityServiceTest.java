package com._glab.booking_system.booking.service;

import com._glab.booking_system.booking.model.*;
import com._glab.booking_system.booking.repository.*;
import com._glab.booking_system.booking.response.*;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private LabRepository labRepository;
    @Mock
    private LabOperatingHoursRepository operatingHoursRepository;
    @Mock
    private LabClosedDayRepository closedDayRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationWorkstationRepository reservationWorkstationRepository;
    @Mock
    private WorkstationRepository workstationRepository;

    private AvailabilityService availabilityService;

    private Lab testLab;
    private Building testBuilding;
    private User testUser;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(
                labRepository,
                operatingHoursRepository,
                closedDayRepository,
                reservationRepository,
                reservationWorkstationRepository,
                workstationRepository
        );

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

        // Set up test user
        Role professorRole = new Role();
        professorRole.setId(1);
        professorRole.setName(RoleName.PROFESSOR);

        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("professor@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("Professor");
        testUser.setRole(professorRole);
    }

    @Nested
    @DisplayName("Get Weekly Availability Tests")
    class GetWeeklyAvailabilityTests {

        @Test
        @DisplayName("Should get weekly availability with default operating hours")
        void shouldGetWeeklyAvailabilityWithDefaults() {
            // Given
            LocalDate monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(operatingHoursRepository.findByLabId(1)).thenReturn(new ArrayList<>());
            when(closedDayRepository.findSpecificClosuresInRange(anyInt(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(closedDayRepository.findRecurringClosures(1)).thenReturn(new ArrayList<>());
            when(reservationRepository.findByLabIdAndTimeRangeAndStatusIn(anyInt(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            LabAvailabilityResponse response = availabilityService.getWeeklyAvailability(1, monday);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getLabId()).isEqualTo(1);
            assertThat(response.getLabName()).isEqualTo("Test Lab");
            assertThat(response.getWeekStart()).isEqualTo(monday);
            assertThat(response.getWeekEnd()).isEqualTo(monday.plusDays(6));
            assertThat(response.getOperatingHours()).hasSize(7); // All 7 days
            assertThat(response.getClosedDays()).isEmpty();
            assertThat(response.getReservations()).isEmpty();
        }

        @Test
        @DisplayName("Should normalize week start to Monday")
        void shouldNormalizeWeekStartToMonday() {
            // Given - Pass a Wednesday
            LocalDate wednesday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
            LocalDate expectedMonday = wednesday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(operatingHoursRepository.findByLabId(1)).thenReturn(new ArrayList<>());
            when(closedDayRepository.findSpecificClosuresInRange(anyInt(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(closedDayRepository.findRecurringClosures(1)).thenReturn(new ArrayList<>());
            when(reservationRepository.findByLabIdAndTimeRangeAndStatusIn(anyInt(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            LabAvailabilityResponse response = availabilityService.getWeeklyAvailability(1, wednesday);

            // Then
            assertThat(response.getWeekStart()).isEqualTo(expectedMonday);
        }

        @Test
        @DisplayName("Should use current week when weekStart is null")
        void shouldUseCurrentWeekWhenNull() {
            // Given
            LocalDate expectedMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(operatingHoursRepository.findByLabId(1)).thenReturn(new ArrayList<>());
            when(closedDayRepository.findSpecificClosuresInRange(anyInt(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(closedDayRepository.findRecurringClosures(1)).thenReturn(new ArrayList<>());
            when(reservationRepository.findByLabIdAndTimeRangeAndStatusIn(anyInt(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            LabAvailabilityResponse response = availabilityService.getWeeklyAvailability(1, null);

            // Then
            assertThat(response.getWeekStart()).isEqualTo(expectedMonday);
        }

        @Test
        @DisplayName("Should include specific operating hours when defined")
        void shouldIncludeSpecificOperatingHours() {
            // Given
            LabOperatingHours mondayHours = new LabOperatingHours();
            mondayHours.setLab(testLab);
            mondayHours.setDayOfWeek(1); // Monday
            mondayHours.setOpenTime(LocalTime.of(9, 0));
            mondayHours.setCloseTime(LocalTime.of(18, 0));
            mondayHours.setIsClosed(false);

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(operatingHoursRepository.findByLabId(1)).thenReturn(List.of(mondayHours));
            when(closedDayRepository.findSpecificClosuresInRange(anyInt(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(closedDayRepository.findRecurringClosures(1)).thenReturn(new ArrayList<>());
            when(reservationRepository.findByLabIdAndTimeRangeAndStatusIn(anyInt(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            LabAvailabilityResponse response = availabilityService.getWeeklyAvailability(1, null);

            // Then
            assertThat(response.getOperatingHours()).hasSize(1);
            OperatingHoursResponse mondayResponse = response.getOperatingHours().get(0);
            assertThat(mondayResponse.getDayOfWeek()).isEqualTo(1);
            assertThat(mondayResponse.getOpen()).isEqualTo(LocalTime.of(9, 0));
            assertThat(mondayResponse.getClose()).isEqualTo(LocalTime.of(18, 0));
            assertThat(mondayResponse.getClosed()).isFalse();
        }

        @Test
        @DisplayName("Should include closed days")
        void shouldIncludeClosedDays() {
            // Given
            LocalDate monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            LocalDate tuesday = monday.plusDays(1);

            LabClosedDay closedDay = new LabClosedDay();
            closedDay.setLab(testLab);
            closedDay.setSpecificDate(tuesday);
            closedDay.setReason("Maintenance");

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(operatingHoursRepository.findByLabId(1)).thenReturn(new ArrayList<>());
            when(closedDayRepository.findSpecificClosuresInRange(eq(1), eq(monday), eq(monday.plusDays(6))))
                    .thenReturn(List.of(closedDay));
            when(closedDayRepository.findRecurringClosures(1)).thenReturn(new ArrayList<>());
            when(reservationRepository.findByLabIdAndTimeRangeAndStatusIn(anyInt(), any(), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            LabAvailabilityResponse response = availabilityService.getWeeklyAvailability(1, monday);

            // Then
            assertThat(response.getClosedDays()).hasSize(1);
            assertThat(response.getClosedDays().get(0).getDate()).isEqualTo(tuesday);
            assertThat(response.getClosedDays().get(0).getReason()).isEqualTo("Maintenance");
        }

        @Test
        @DisplayName("Should include reservations in range")
        void shouldIncludeReservationsInRange() {
            // Given
            LocalDate monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            OffsetDateTime reservationStart = monday.atTime(10, 0).atOffset(ZoneOffset.UTC);
            OffsetDateTime reservationEnd = monday.atTime(12, 0).atOffset(ZoneOffset.UTC);

            Reservation reservation = new Reservation();
            reservation.setId(UUID.randomUUID());
            reservation.setLab(testLab);
            reservation.setUser(testUser);
            reservation.setStartTime(reservationStart);
            reservation.setEndTime(reservationEnd);
            reservation.setStatus(ReservationStatus.APPROVED);
            reservation.setWholeLab(false);

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(operatingHoursRepository.findByLabId(1)).thenReturn(new ArrayList<>());
            when(closedDayRepository.findSpecificClosuresInRange(anyInt(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(closedDayRepository.findRecurringClosures(1)).thenReturn(new ArrayList<>());
            when(reservationRepository.findByLabIdAndTimeRangeAndStatusIn(anyInt(), any(), any(), any()))
                    .thenReturn(List.of(reservation));
            when(reservationWorkstationRepository.findWorkstationIdsByReservationId(any()))
                    .thenReturn(List.of(1, 2));

            // When
            LabAvailabilityResponse response = availabilityService.getWeeklyAvailability(1, monday);

            // Then
            assertThat(response.getReservations()).hasSize(1);
            ReservationSummaryResponse summary = response.getReservations().get(0);
            assertThat(summary.getStatus()).isEqualTo(ReservationStatus.APPROVED);
            assertThat(summary.getUserName()).isEqualTo("Test Professor");
            assertThat(summary.getWorkstationIds()).containsExactly(1, 2);
        }

        @Test
        @DisplayName("Should throw exception when lab not found")
        void shouldThrowWhenLabNotFound() {
            // Given
            when(labRepository.findById(999)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> availabilityService.getWeeklyAvailability(999, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Lab not found");
        }
    }

    @Nested
    @DisplayName("Get Current Availability Tests")
    class GetCurrentAvailabilityTests {

        @Test
        @DisplayName("Should get current availability when lab is open")
        void shouldGetCurrentAvailabilityWhenOpen() {
            // Given - Use a time during lab hours
            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(closedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
            when(operatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty()); // Use defaults
            when(reservationRepository.findCurrentReservations(anyInt(), any(), eq(ReservationStatus.APPROVED)))
                    .thenReturn(new ArrayList<>());

            // When
            CurrentAvailabilityResponse response = availabilityService.getCurrentAvailability(1);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getLabId()).isEqualTo(1);
            assertThat(response.getLabName()).isEqualTo("Test Lab");
            // Note: isOpen depends on current time, can't assert reliably
            assertThat(response.getCurrentReservations()).isEmpty();
        }

        @Test
        @DisplayName("Should include current approved reservations")
        void shouldIncludeCurrentApprovedReservations() {
            // Given
            Reservation currentReservation = new Reservation();
            currentReservation.setId(UUID.randomUUID());
            currentReservation.setLab(testLab);
            currentReservation.setUser(testUser);
            currentReservation.setStartTime(OffsetDateTime.now().minusHours(1));
            currentReservation.setEndTime(OffsetDateTime.now().plusHours(1));
            currentReservation.setStatus(ReservationStatus.APPROVED);
            currentReservation.setWholeLab(true);

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(closedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
            when(operatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            when(reservationRepository.findCurrentReservations(anyInt(), any(), eq(ReservationStatus.APPROVED)))
                    .thenReturn(List.of(currentReservation));
            when(reservationWorkstationRepository.findWorkstationIdsByReservationId(any()))
                    .thenReturn(new ArrayList<>());

            // When
            CurrentAvailabilityResponse response = availabilityService.getCurrentAvailability(1);

            // Then
            assertThat(response.getCurrentReservations()).hasSize(1);
            assertThat(response.getCurrentReservations().get(0).getWholeLab()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when lab not found")
        void shouldThrowWhenLabNotFound() {
            // Given
            when(labRepository.findById(999)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> availabilityService.getCurrentAvailability(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Lab not found");
        }
    }

    @Nested
    @DisplayName("Get Lab Workstations Tests")
    class GetLabWorkstationsTests {

        @Test
        @DisplayName("Should get all workstations for lab")
        void shouldGetAllWorkstationsForLab() {
            // Given
            Workstation ws1 = new Workstation();
            ws1.setId(1);
            ws1.setLab(testLab);
            ws1.setIdentifier("WS-001");
            ws1.setDescription("Workstation 1");
            ws1.setActive(true);

            Workstation ws2 = new Workstation();
            ws2.setId(2);
            ws2.setLab(testLab);
            ws2.setIdentifier("WS-002");
            ws2.setDescription("Workstation 2");
            ws2.setActive(false);

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(workstationRepository.findByLabId(1)).thenReturn(List.of(ws1, ws2));

            // When
            LabWorkstationsResponse response = availabilityService.getLabWorkstations(1);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getLabId()).isEqualTo(1);
            assertThat(response.getLabName()).isEqualTo("Test Lab");
            assertThat(response.getWorkstations()).hasSize(2);

            WorkstationResponse wsResponse1 = response.getWorkstations().get(0);
            assertThat(wsResponse1.getId()).isEqualTo(1);
            assertThat(wsResponse1.getIdentifier()).isEqualTo("WS-001");
            assertThat(wsResponse1.getActive()).isTrue();

            WorkstationResponse wsResponse2 = response.getWorkstations().get(1);
            assertThat(wsResponse2.getId()).isEqualTo(2);
            assertThat(wsResponse2.getActive()).isFalse();
        }

        @Test
        @DisplayName("Should return empty list when lab has no workstations")
        void shouldReturnEmptyWhenNoWorkstations() {
            // Given
            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(workstationRepository.findByLabId(1)).thenReturn(new ArrayList<>());

            // When
            LabWorkstationsResponse response = availabilityService.getLabWorkstations(1);

            // Then
            assertThat(response.getWorkstations()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when lab not found")
        void shouldThrowWhenLabNotFound() {
            // Given
            when(labRepository.findById(999)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> availabilityService.getLabWorkstations(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Lab not found");
        }
    }
}
