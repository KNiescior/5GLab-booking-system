package com._glab.booking_system.booking.service;

import com._glab.booking_system.booking.model.*;
import com._glab.booking_system.booking.repository.LabManagerRepository;
import com._glab.booking_system.booking.repository.LabRepository;
import com._glab.booking_system.booking.repository.ReservationRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabManagerAuthorizationServiceTest {

    @Mock
    private LabManagerRepository labManagerRepository;
    @Mock
    private LabRepository labRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private LabManagerAuthorizationService authorizationService;

    private User adminUser;
    private User labManagerUser;
    private User professorUser;
    private Lab testLab;
    private Lab otherLab;
    private Reservation testReservation;
    private LabManager labManager;

    @BeforeEach
    void setUp() {
        authorizationService = new LabManagerAuthorizationService(
                labManagerRepository,
                labRepository,
                reservationRepository
        );

        // Create admin role and user
        Role adminRole = new Role();
        adminRole.setId(1);
        adminRole.setName(RoleName.ADMIN);

        adminUser = new User();
        adminUser.setId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(adminRole);

        // Create lab manager role and user
        Role labManagerRole = new Role();
        labManagerRole.setId(2);
        labManagerRole.setName(RoleName.LAB_MANAGER);

        labManagerUser = new User();
        labManagerUser.setId(2);
        labManagerUser.setEmail("manager@example.com");
        labManagerUser.setRole(labManagerRole);

        // Create professor role and user
        Role professorRole = new Role();
        professorRole.setId(3);
        professorRole.setName(RoleName.PROFESSOR);

        professorUser = new User();
        professorUser.setId(3);
        professorUser.setEmail("professor@example.com");
        professorUser.setRole(professorRole);

        // Create test lab
        testLab = new Lab();
        testLab.setId(1);
        testLab.setName("Test Lab");

        otherLab = new Lab();
        otherLab.setId(2);
        otherLab.setName("Other Lab");

        // Create lab manager assignment
        labManager = new LabManager();
        labManager.setId(1);
        labManager.setLab(testLab);
        labManager.setUser(labManagerUser);

        // Create test reservation
        testReservation = new Reservation();
        testReservation.setId(UUID.randomUUID());
        testReservation.setLab(testLab);
        testReservation.setUser(professorUser);
        testReservation.setStatus(ReservationStatus.PENDING);
    }

    @Nested
    @DisplayName("isAdmin Tests")
    class IsAdminTests {

        @Test
        @DisplayName("Should return true for admin user")
        void shouldReturnTrueForAdmin() {
            assertThat(authorizationService.isAdmin(adminUser)).isTrue();
        }

        @Test
        @DisplayName("Should return false for lab manager user")
        void shouldReturnFalseForLabManager() {
            assertThat(authorizationService.isAdmin(labManagerUser)).isFalse();
        }

        @Test
        @DisplayName("Should return false for professor user")
        void shouldReturnFalseForProfessor() {
            assertThat(authorizationService.isAdmin(professorUser)).isFalse();
        }

        @Test
        @DisplayName("Should return false for null user")
        void shouldReturnFalseForNullUser() {
            assertThat(authorizationService.isAdmin(null)).isFalse();
        }

        @Test
        @DisplayName("Should return false for user with null role")
        void shouldReturnFalseForNullRole() {
            User userWithNullRole = new User();
            userWithNullRole.setRole(null);
            assertThat(authorizationService.isAdmin(userWithNullRole)).isFalse();
        }
    }

    @Nested
    @DisplayName("isLabManagerForLab Tests")
    class IsLabManagerForLabTests {

        @Test
        @DisplayName("Should return true for admin (manages all labs)")
        void shouldReturnTrueForAdmin() {
            assertThat(authorizationService.isLabManagerForLab(adminUser, testLab.getId())).isTrue();
        }

        @Test
        @DisplayName("Should return true for assigned lab manager")
        void shouldReturnTrueForAssignedLabManager() {
            when(labRepository.findById(testLab.getId())).thenReturn(Optional.of(testLab));
            when(labManagerRepository.existsByLabAndUser(testLab, labManagerUser)).thenReturn(true);

            assertThat(authorizationService.isLabManagerForLab(labManagerUser, testLab.getId())).isTrue();
        }

        @Test
        @DisplayName("Should return false for lab manager of different lab")
        void shouldReturnFalseForDifferentLabManager() {
            when(labRepository.findById(otherLab.getId())).thenReturn(Optional.of(otherLab));
            when(labManagerRepository.existsByLabAndUser(otherLab, labManagerUser)).thenReturn(false);

            assertThat(authorizationService.isLabManagerForLab(labManagerUser, otherLab.getId())).isFalse();
        }

        @Test
        @DisplayName("Should return false for professor")
        void shouldReturnFalseForProfessor() {
            when(labRepository.findById(testLab.getId())).thenReturn(Optional.of(testLab));
            when(labManagerRepository.existsByLabAndUser(testLab, professorUser)).thenReturn(false);

            assertThat(authorizationService.isLabManagerForLab(professorUser, testLab.getId())).isFalse();
        }

        @Test
        @DisplayName("Should return false for null user")
        void shouldReturnFalseForNullUser() {
            assertThat(authorizationService.isLabManagerForLab(null, testLab.getId())).isFalse();
        }

        @Test
        @DisplayName("Should return false for null lab ID")
        void shouldReturnFalseForNullLabId() {
            assertThat(authorizationService.isLabManagerForLab(labManagerUser, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isLabManagerForReservation Tests")
    class IsLabManagerForReservationTests {

        @Test
        @DisplayName("Should return true for admin")
        void shouldReturnTrueForAdmin() {
            assertThat(authorizationService.isLabManagerForReservation(adminUser, testReservation)).isTrue();
        }

        @Test
        @DisplayName("Should return true for assigned lab manager")
        void shouldReturnTrueForAssignedLabManager() {
            when(labRepository.findById(testLab.getId())).thenReturn(Optional.of(testLab));
            when(labManagerRepository.existsByLabAndUser(testLab, labManagerUser)).thenReturn(true);

            assertThat(authorizationService.isLabManagerForReservation(labManagerUser, testReservation)).isTrue();
        }

        @Test
        @DisplayName("Should return false for null reservation")
        void shouldReturnFalseForNullReservation() {
            assertThat(authorizationService.isLabManagerForReservation(labManagerUser, null)).isFalse();
        }

        @Test
        @DisplayName("Should return false for reservation with null lab")
        void shouldReturnFalseForReservationWithNullLab() {
            Reservation reservationWithoutLab = new Reservation();
            reservationWithoutLab.setLab(null);
            assertThat(authorizationService.isLabManagerForReservation(labManagerUser, reservationWithoutLab)).isFalse();
        }
    }

    @Nested
    @DisplayName("canManageReservation Tests")
    class CanManageReservationTests {

        @Test
        @DisplayName("Should return true for admin")
        void shouldReturnTrueForAdmin() {
            assertThat(authorizationService.canManageReservation(adminUser, testReservation)).isTrue();
        }

        @Test
        @DisplayName("Should return true for lab manager of reservation's lab")
        void shouldReturnTrueForLabManagerOfLab() {
            when(labRepository.findById(testLab.getId())).thenReturn(Optional.of(testLab));
            when(labManagerRepository.existsByLabAndUser(testLab, labManagerUser)).thenReturn(true);

            assertThat(authorizationService.canManageReservation(labManagerUser, testReservation)).isTrue();
        }

        @Test
        @DisplayName("Should return false for professor")
        void shouldReturnFalseForProfessor() {
            when(labRepository.findById(testLab.getId())).thenReturn(Optional.of(testLab));
            when(labManagerRepository.existsByLabAndUser(testLab, professorUser)).thenReturn(false);

            assertThat(authorizationService.canManageReservation(professorUser, testReservation)).isFalse();
        }
    }

    @Nested
    @DisplayName("isReservationOwner Tests")
    class IsReservationOwnerTests {

        @Test
        @DisplayName("Should return true for reservation owner")
        void shouldReturnTrueForOwner() {
            assertThat(authorizationService.isReservationOwner(professorUser, testReservation)).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-owner")
        void shouldReturnFalseForNonOwner() {
            assertThat(authorizationService.isReservationOwner(labManagerUser, testReservation)).isFalse();
        }

        @Test
        @DisplayName("Should return false for admin (not owner)")
        void shouldReturnFalseForAdmin() {
            assertThat(authorizationService.isReservationOwner(adminUser, testReservation)).isFalse();
        }

        @Test
        @DisplayName("Should return false for null user")
        void shouldReturnFalseForNullUser() {
            assertThat(authorizationService.isReservationOwner(null, testReservation)).isFalse();
        }

        @Test
        @DisplayName("Should return false for null reservation")
        void shouldReturnFalseForNullReservation() {
            assertThat(authorizationService.isReservationOwner(professorUser, null)).isFalse();
        }

        @Test
        @DisplayName("Should return false for reservation with null user")
        void shouldReturnFalseForReservationWithNullUser() {
            Reservation reservationWithoutUser = new Reservation();
            reservationWithoutUser.setUser(null);
            assertThat(authorizationService.isReservationOwner(professorUser, reservationWithoutUser)).isFalse();
        }
    }

    @Nested
    @DisplayName("getManagedLabs Tests")
    class GetManagedLabsTests {

        @Test
        @DisplayName("Should return all labs for admin")
        void shouldReturnAllLabsForAdmin() {
            when(labRepository.findAll()).thenReturn(List.of(testLab, otherLab));

            List<Lab> managedLabs = authorizationService.getManagedLabs(adminUser);

            assertThat(managedLabs).hasSize(2);
            assertThat(managedLabs).containsExactlyInAnyOrder(testLab, otherLab);
        }

        @Test
        @DisplayName("Should return only managed labs for lab manager")
        void shouldReturnOnlyManagedLabsForLabManager() {
            when(labManagerRepository.findByUser(labManagerUser)).thenReturn(List.of(labManager));

            List<Lab> managedLabs = authorizationService.getManagedLabs(labManagerUser);

            assertThat(managedLabs).hasSize(1);
            assertThat(managedLabs).containsExactly(testLab);
        }

        @Test
        @DisplayName("Should return empty list for null user")
        void shouldReturnEmptyForNullUser() {
            List<Lab> managedLabs = authorizationService.getManagedLabs(null);
            assertThat(managedLabs).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for user with no managed labs")
        void shouldReturnEmptyForNoManagedLabs() {
            when(labManagerRepository.findByUser(professorUser)).thenReturn(List.of());

            List<Lab> managedLabs = authorizationService.getManagedLabs(professorUser);

            assertThat(managedLabs).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPendingReservationsForUser Tests")
    class GetPendingReservationsForUserTests {

        @Test
        @DisplayName("Should return all pending reservations for admin")
        void shouldReturnAllPendingForAdmin() {
            Reservation pendingReservation1 = new Reservation();
            pendingReservation1.setId(UUID.randomUUID());
            pendingReservation1.setStatus(ReservationStatus.PENDING);

            Reservation pendingReservation2 = new Reservation();
            pendingReservation2.setId(UUID.randomUUID());
            pendingReservation2.setStatus(ReservationStatus.PENDING);

            when(reservationRepository.findByStatus(ReservationStatus.PENDING))
                    .thenReturn(List.of(pendingReservation1, pendingReservation2));

            List<Reservation> pendingReservations = authorizationService.getPendingReservationsForUser(adminUser);

            assertThat(pendingReservations).hasSize(2);
        }

        @Test
        @DisplayName("Should return only managed lab reservations for lab manager")
        void shouldReturnOnlyManagedLabReservationsForLabManager() {
            when(reservationRepository.findPendingReservationsForManager(labManagerUser.getId()))
                    .thenReturn(List.of(testReservation));

            List<Reservation> pendingReservations = authorizationService.getPendingReservationsForUser(labManagerUser);

            assertThat(pendingReservations).hasSize(1);
            assertThat(pendingReservations.get(0)).isEqualTo(testReservation);
        }

        @Test
        @DisplayName("Should return empty list for null user")
        void shouldReturnEmptyForNullUser() {
            List<Reservation> pendingReservations = authorizationService.getPendingReservationsForUser(null);
            assertThat(pendingReservations).isEmpty();
        }
    }
}
