package com._glab.booking_system.booking.service;

import com._glab.booking_system.auth.service.EmailService;
import com._glab.booking_system.booking.exception.BookingNotAuthorizedException;
import com._glab.booking_system.booking.exception.ReservationNotFoundException;
import com._glab.booking_system.booking.model.*;
import com._glab.booking_system.booking.repository.ReservationRepository;
import com._glab.booking_system.booking.response.ReservationResponse;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationManagementServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationService reservationService;
    @Mock
    private LabManagerAuthorizationService authorizationService;
    @Mock
    private EmailService emailService;

    private ReservationManagementService managementService;

    private User adminUser;
    private User labManagerUser;
    private User professorUser;
    private Lab testLab;
    private Reservation pendingReservation;
    private Reservation approvedReservation;
    private UUID reservationId;
    private UUID recurringGroupId;

    @BeforeEach
    void setUp() {
        managementService = new ReservationManagementService(
                reservationRepository,
                reservationService,
                authorizationService,
                emailService
        );

        // Create admin
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        adminUser = new User();
        adminUser.setId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(adminRole);

        // Create lab manager
        Role labManagerRole = new Role();
        labManagerRole.setName(RoleName.LAB_MANAGER);
        labManagerUser = new User();
        labManagerUser.setId(2);
        labManagerUser.setEmail("manager@example.com");
        labManagerUser.setFirstName("Lab");
        labManagerUser.setLastName("Manager");
        labManagerUser.setRole(labManagerRole);

        // Create professor
        Role professorRole = new Role();
        professorRole.setName(RoleName.PROFESSOR);
        professorUser = new User();
        professorUser.setId(3);
        professorUser.setEmail("professor@example.com");
        professorUser.setFirstName("Test");
        professorUser.setLastName("Professor");
        professorUser.setRole(professorRole);

        // Create test lab
        testLab = new Lab();
        testLab.setId(1);
        testLab.setName("Test Lab");

        // Create reservations
        reservationId = UUID.randomUUID();
        recurringGroupId = UUID.randomUUID();

        pendingReservation = new Reservation();
        pendingReservation.setId(reservationId);
        pendingReservation.setLab(testLab);
        pendingReservation.setUser(professorUser);
        pendingReservation.setStatus(ReservationStatus.PENDING);
        pendingReservation.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        pendingReservation.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(2));

        approvedReservation = new Reservation();
        approvedReservation.setId(UUID.randomUUID());
        approvedReservation.setLab(testLab);
        approvedReservation.setUser(professorUser);
        approvedReservation.setStatus(ReservationStatus.APPROVED);
        approvedReservation.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
        approvedReservation.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(2));
    }

    @Nested
    @DisplayName("getPendingReservationsForManager Tests")
    class GetPendingReservationsTests {

        @Test
        @DisplayName("Should return pending reservations for lab manager")
        void shouldReturnPendingReservationsForManager() {
            ReservationResponse response = ReservationResponse.builder()
                    .id(pendingReservation.getId())
                    .labId(testLab.getId())
                    .status(ReservationStatus.PENDING)
                    .build();

            when(authorizationService.getPendingReservationsForUser(labManagerUser))
                    .thenReturn(List.of(pendingReservation));
            when(reservationService.toReservationResponse(pendingReservation)).thenReturn(response);

            List<ReservationResponse> result = managementService.getPendingReservationsForManager(labManagerUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("Should return empty list when no pending reservations")
        void shouldReturnEmptyListWhenNoPendingReservations() {
            when(authorizationService.getPendingReservationsForUser(labManagerUser))
                    .thenReturn(List.of());

            List<ReservationResponse> result = managementService.getPendingReservationsForManager(labManagerUser);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("approveReservation Tests")
    class ApproveReservationTests {

        @Test
        @DisplayName("Should approve pending reservation successfully")
        void shouldApprovePendingReservation() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(labManagerUser, pendingReservation)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

            managementService.approveReservation(reservationId, labManagerUser, "Approved");

            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.APPROVED);
            verify(reservationRepository).save(pendingReservation);
            verify(emailService).sendReservationStatusChangeEmail(
                    eq(professorUser.getEmail()),
                    anyString(),
                    anyString(),
                    anyString(),
                    anyString(),
                    eq("APPROVED"),
                    eq("Approved")
            );
        }

        @Test
        @DisplayName("Should approve reservation as admin")
        void shouldApproveReservationAsAdmin() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(adminUser, pendingReservation)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

            managementService.approveReservation(reservationId, adminUser, null);

            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.APPROVED);
        }

        @Test
        @DisplayName("Should throw ReservationNotFoundException when reservation not found")
        void shouldThrowWhenReservationNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(reservationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> managementService.approveReservation(nonExistentId, labManagerUser, null))
                    .isInstanceOf(ReservationNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw BookingNotAuthorizedException when user not authorized")
        void shouldThrowWhenNotAuthorized() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(professorUser, pendingReservation)).thenReturn(false);

            assertThatThrownBy(() -> managementService.approveReservation(reservationId, professorUser, null))
                    .isInstanceOf(BookingNotAuthorizedException.class)
                    .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when reservation is not PENDING")
        void shouldThrowWhenNotPending() {
            when(reservationRepository.findById(approvedReservation.getId()))
                    .thenReturn(Optional.of(approvedReservation));
            when(authorizationService.canManageReservation(labManagerUser, approvedReservation)).thenReturn(true);

            assertThatThrownBy(() -> managementService.approveReservation(approvedReservation.getId(), labManagerUser, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("declineReservation Tests")
    class DeclineReservationTests {

        @Test
        @DisplayName("Should decline pending reservation successfully")
        void shouldDeclinePendingReservation() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(labManagerUser, pendingReservation)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

            managementService.declineReservation(reservationId, labManagerUser, "Not available");

            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
            verify(reservationRepository).save(pendingReservation);
            verify(emailService).sendReservationStatusChangeEmail(
                    eq(professorUser.getEmail()),
                    anyString(),
                    anyString(),
                    anyString(),
                    anyString(),
                    eq("REJECTED"),
                    eq("Not available")
            );
        }

        @Test
        @DisplayName("Should decline reservation without reason")
        void shouldDeclineReservationWithoutReason() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(labManagerUser, pendingReservation)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

            managementService.declineReservation(reservationId, labManagerUser, null);

            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
        }

        @Test
        @DisplayName("Should throw ReservationNotFoundException when reservation not found")
        void shouldThrowWhenReservationNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(reservationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> managementService.declineReservation(nonExistentId, labManagerUser, null))
                    .isInstanceOf(ReservationNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw BookingNotAuthorizedException when user not authorized")
        void shouldThrowWhenNotAuthorized() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(professorUser, pendingReservation)).thenReturn(false);

            assertThatThrownBy(() -> managementService.declineReservation(reservationId, professorUser, null))
                    .isInstanceOf(BookingNotAuthorizedException.class);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when reservation is not PENDING")
        void shouldThrowWhenNotPending() {
            when(reservationRepository.findById(approvedReservation.getId()))
                    .thenReturn(Optional.of(approvedReservation));
            when(authorizationService.canManageReservation(labManagerUser, approvedReservation)).thenReturn(true);

            assertThatThrownBy(() -> managementService.declineReservation(approvedReservation.getId(), labManagerUser, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("approveRecurringGroup Tests")
    class ApproveRecurringGroupTests {

        @Test
        @DisplayName("Should approve all pending reservations in recurring group")
        void shouldApproveAllPendingInRecurringGroup() {
            Reservation recurring1 = createRecurringReservation(recurringGroupId, ReservationStatus.PENDING);
            Reservation recurring2 = createRecurringReservation(recurringGroupId, ReservationStatus.PENDING);
            Reservation recurring3 = createRecurringReservation(recurringGroupId, ReservationStatus.APPROVED); // Already approved

            when(reservationRepository.findByRecurringGroupId(recurringGroupId))
                    .thenReturn(List.of(recurring1, recurring2, recurring3));
            when(authorizationService.canManageReservation(labManagerUser, recurring1)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

            managementService.approveRecurringGroup(recurringGroupId, labManagerUser, "Approved group");

            assertThat(recurring1.getStatus()).isEqualTo(ReservationStatus.APPROVED);
            assertThat(recurring2.getStatus()).isEqualTo(ReservationStatus.APPROVED);
            // recurring3 was already approved, should remain approved
            assertThat(recurring3.getStatus()).isEqualTo(ReservationStatus.APPROVED);
            
            // Should save only 2 (the ones that were PENDING)
            verify(reservationRepository, times(2)).save(any(Reservation.class));
        }

        @Test
        @DisplayName("Should throw ReservationNotFoundException when no reservations in group")
        void shouldThrowWhenNoReservationsInGroup() {
            when(reservationRepository.findByRecurringGroupId(recurringGroupId)).thenReturn(List.of());

            assertThatThrownBy(() -> managementService.approveRecurringGroup(recurringGroupId, labManagerUser, null))
                    .isInstanceOf(ReservationNotFoundException.class)
                    .hasMessageContaining("recurring group");
        }

        @Test
        @DisplayName("Should throw BookingNotAuthorizedException when user not authorized")
        void shouldThrowWhenNotAuthorized() {
            Reservation recurring = createRecurringReservation(recurringGroupId, ReservationStatus.PENDING);
            when(reservationRepository.findByRecurringGroupId(recurringGroupId)).thenReturn(List.of(recurring));
            when(authorizationService.canManageReservation(professorUser, recurring)).thenReturn(false);

            assertThatThrownBy(() -> managementService.approveRecurringGroup(recurringGroupId, professorUser, null))
                    .isInstanceOf(BookingNotAuthorizedException.class);
        }
    }

    @Nested
    @DisplayName("declineRecurringGroup Tests")
    class DeclineRecurringGroupTests {

        @Test
        @DisplayName("Should decline all pending reservations in recurring group")
        void shouldDeclineAllPendingInRecurringGroup() {
            Reservation recurring1 = createRecurringReservation(recurringGroupId, ReservationStatus.PENDING);
            Reservation recurring2 = createRecurringReservation(recurringGroupId, ReservationStatus.PENDING);

            when(reservationRepository.findByRecurringGroupId(recurringGroupId))
                    .thenReturn(List.of(recurring1, recurring2));
            when(authorizationService.canManageReservation(labManagerUser, recurring1)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

            managementService.declineRecurringGroup(recurringGroupId, labManagerUser, "Declined group");

            assertThat(recurring1.getStatus()).isEqualTo(ReservationStatus.REJECTED);
            assertThat(recurring2.getStatus()).isEqualTo(ReservationStatus.REJECTED);
            verify(reservationRepository, times(2)).save(any(Reservation.class));
        }

        @Test
        @DisplayName("Should throw ReservationNotFoundException when no reservations in group")
        void shouldThrowWhenNoReservationsInGroup() {
            when(reservationRepository.findByRecurringGroupId(recurringGroupId)).thenReturn(List.of());

            assertThatThrownBy(() -> managementService.declineRecurringGroup(recurringGroupId, labManagerUser, null))
                    .isInstanceOf(ReservationNotFoundException.class)
                    .hasMessageContaining("recurring group");
        }
    }

    @Nested
    @DisplayName("getReservation Tests")
    class GetReservationTests {

        @Test
        @DisplayName("Should return reservation response")
        void shouldReturnReservationResponse() {
            ReservationResponse response = ReservationResponse.builder()
                    .id(reservationId)
                    .labId(testLab.getId())
                    .labName(testLab.getName())
                    .status(ReservationStatus.PENDING)
                    .build();

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(reservationService.toReservationResponse(pendingReservation)).thenReturn(response);

            ReservationResponse result = managementService.getReservation(reservationId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(reservationId);
            assertThat(result.getLabName()).isEqualTo("Test Lab");
        }

        @Test
        @DisplayName("Should throw ReservationNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(reservationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> managementService.getReservation(nonExistentId))
                    .isInstanceOf(ReservationNotFoundException.class);
        }
    }

    // Helper method
    private Reservation createRecurringReservation(UUID groupId, ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setLab(testLab);
        reservation.setUser(professorUser);
        reservation.setRecurringGroupId(groupId);
        reservation.setStatus(status);
        reservation.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        reservation.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(2));
        return reservation;
    }
}
