package com._glab.booking_system.booking.service;

import com._glab.booking_system.auth.service.EmailService;
import com._glab.booking_system.booking.exception.BookingNotAuthorizedException;
import com._glab.booking_system.booking.exception.EditProposalNotFoundException;
import com._glab.booking_system.booking.exception.ReservationNotFoundException;
import com._glab.booking_system.booking.model.*;
import com._glab.booking_system.booking.repository.*;
import com._glab.booking_system.booking.request.EditReservationRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReservationEditServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationEditProposalRepository editProposalRepository;
    @Mock
    private ReservationWorkstationRepository reservationWorkstationRepository;
    @Mock
    private WorkstationRepository workstationRepository;
    @Mock
    private LabOperatingHoursRepository labOperatingHoursRepository;
    @Mock
    private LabClosedDayRepository labClosedDayRepository;
    @Mock
    private LabManagerAuthorizationService authorizationService;
    @Mock
    private EmailService emailService;
    @Mock
    private LabManagerRepository labManagerRepository;

    private ReservationEditService editService;

    private User adminUser;
    private User labManagerUser;
    private User professorUser;
    private Lab testLab;
    private Reservation pendingReservation;
    private Reservation approvedReservation;
    private ReservationEditProposal editProposal;
    private EditReservationRequest validEditRequest;
    private UUID reservationId;

    @BeforeEach
    void setUp() {
        editService = new ReservationEditService(
                reservationRepository,
                editProposalRepository,
                reservationWorkstationRepository,
                workstationRepository,
                labOperatingHoursRepository,
                labClosedDayRepository,
                authorizationService,
                emailService,
                labManagerRepository
        );

        // Create users
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        adminUser = new User();
        adminUser.setId(1);
        adminUser.setEmail("admin@example.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(adminRole);

        Role labManagerRole = new Role();
        labManagerRole.setName(RoleName.LAB_MANAGER);
        labManagerUser = new User();
        labManagerUser.setId(2);
        labManagerUser.setEmail("manager@example.com");
        labManagerUser.setFirstName("Lab");
        labManagerUser.setLastName("Manager");
        labManagerUser.setRole(labManagerRole);

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
        testLab.setDefaultOpenTime(LocalTime.of(8, 0));
        testLab.setDefaultCloseTime(LocalTime.of(20, 0));

        // Create reservations
        reservationId = UUID.randomUUID();
        OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);

        pendingReservation = new Reservation();
        pendingReservation.setId(reservationId);
        pendingReservation.setLab(testLab);
        pendingReservation.setUser(professorUser);
        pendingReservation.setStatus(ReservationStatus.PENDING);
        pendingReservation.setStartTime(tomorrow);
        pendingReservation.setEndTime(tomorrow.plusHours(2));
        pendingReservation.setDescription("Original description");
        pendingReservation.setWholeLab(true);

        approvedReservation = new Reservation();
        approvedReservation.setId(UUID.randomUUID());
        approvedReservation.setLab(testLab);
        approvedReservation.setUser(professorUser);
        approvedReservation.setStatus(ReservationStatus.APPROVED);
        approvedReservation.setStartTime(tomorrow.plusDays(1));
        approvedReservation.setEndTime(tomorrow.plusDays(1).plusHours(2));
        approvedReservation.setDescription("Approved description");
        approvedReservation.setWholeLab(true);

        // Create valid edit request
        validEditRequest = EditReservationRequest.builder()
                .startTime(tomorrow.plusHours(1))
                .endTime(tomorrow.plusHours(3))
                .description("Updated description")
                .wholeLab(true)
                .build();

        // Create edit proposal
        editProposal = new ReservationEditProposal();
        editProposal.setId(UUID.randomUUID());
        editProposal.setReservation(pendingReservation);
        editProposal.setEditedBy(labManagerUser);
        editProposal.setOriginalStatus(ReservationStatus.PENDING);
        editProposal.setOriginalStartTime(pendingReservation.getStartTime());
        editProposal.setOriginalEndTime(pendingReservation.getEndTime());
        editProposal.setOriginalDescription(pendingReservation.getDescription());
        editProposal.setOriginalWholeLab(true);
        editProposal.setProposedStartTime(validEditRequest.getStartTime());
        editProposal.setProposedEndTime(validEditRequest.getEndTime());
        editProposal.setProposedDescription(validEditRequest.getDescription());
        editProposal.setProposedWholeLab(true);
        editProposal.setResolution(ResolutionStatus.PENDING);
    }

    @Nested
    @DisplayName("editReservationByManager Tests")
    class EditReservationByManagerTests {

        private void setUpValidationMocks() {
            lenient().when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            lenient().when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
        }

        @Test
        @DisplayName("Should create edit proposal for pending reservation")
        void shouldCreateEditProposalForPendingReservation() {
            setUpValidationMocks();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(labManagerUser, pendingReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(reservationWorkstationRepository.findWorkstationIdsByReservationId(reservationId))
                    .thenReturn(List.of());
            when(editProposalRepository.save(any(ReservationEditProposal.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

            editService.editReservationByManager(reservationId, validEditRequest, labManagerUser);

            // Verify edit proposal was created
            ArgumentCaptor<ReservationEditProposal> proposalCaptor = ArgumentCaptor.forClass(ReservationEditProposal.class);
            verify(editProposalRepository).save(proposalCaptor.capture());
            ReservationEditProposal savedProposal = proposalCaptor.getValue();
            
            assertThat(savedProposal.getEditedBy()).isEqualTo(labManagerUser);
            assertThat(savedProposal.getOriginalStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(savedProposal.getProposedDescription()).isEqualTo("Updated description");
            assertThat(savedProposal.getResolution()).isEqualTo(ResolutionStatus.PENDING);

            // Verify reservation status changed
            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.PENDING_EDIT_APPROVAL);
        }

        @Test
        @DisplayName("Should throw ReservationNotFoundException when reservation not found")
        void shouldThrowWhenReservationNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(reservationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> editService.editReservationByManager(nonExistentId, validEditRequest, labManagerUser))
                    .isInstanceOf(ReservationNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw BookingNotAuthorizedException when user not authorized")
        void shouldThrowWhenNotAuthorized() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(professorUser, pendingReservation)).thenReturn(false);

            assertThatThrownBy(() -> editService.editReservationByManager(reservationId, validEditRequest, professorUser))
                    .isInstanceOf(BookingNotAuthorizedException.class);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when edit proposal already exists")
        void shouldThrowWhenEditProposalExists() {
            setUpValidationMocks();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(labManagerUser, pendingReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING))
                    .thenReturn(Optional.of(editProposal));

            assertThatThrownBy(() -> editService.editReservationByManager(reservationId, validEditRequest, labManagerUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("pending edit proposal");
        }
    }

    @Nested
    @DisplayName("approveEditByManager Tests")
    class ApproveEditByManagerTests {

        @Test
        @DisplayName("Should approve professor's edit successfully")
        void shouldApproveProfessorsEdit() {
            // Professor made the edit
            editProposal.setEditedBy(professorUser);
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(labManagerUser, pendingReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING))
                    .thenReturn(Optional.of(editProposal));
            when(authorizationService.isReservationOwner(professorUser, pendingReservation)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
            when(editProposalRepository.save(any(ReservationEditProposal.class))).thenAnswer(i -> i.getArgument(0));

            editService.approveEditByManager(reservationId, labManagerUser);

            // Verify reservation was updated with proposed values
            assertThat(pendingReservation.getStartTime()).isEqualTo(editProposal.getProposedStartTime());
            assertThat(pendingReservation.getEndTime()).isEqualTo(editProposal.getProposedEndTime());
            assertThat(pendingReservation.getDescription()).isEqualTo(editProposal.getProposedDescription());
            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.APPROVED);

            // Verify proposal was marked as approved
            assertThat(editProposal.getResolution()).isEqualTo(ResolutionStatus.APPROVED);
            assertThat(editProposal.getResolvedBy()).isEqualTo(labManagerUser);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when edit was made by non-owner")
        void shouldThrowWhenEditNotByOwner() {
            // Lab manager made the edit (not the professor)
            editProposal.setEditedBy(labManagerUser);
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(labManagerUser, pendingReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING))
                    .thenReturn(Optional.of(editProposal));
            when(authorizationService.isReservationOwner(labManagerUser, pendingReservation)).thenReturn(false);

            assertThatThrownBy(() -> editService.approveEditByManager(reservationId, labManagerUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not created by the reservation owner");
        }

        @Test
        @DisplayName("Should throw EditProposalNotFoundException when no active edit proposal")
        void shouldThrowWhenNoActiveEditProposal() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(labManagerUser, pendingReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> editService.approveEditByManager(reservationId, labManagerUser))
                    .isInstanceOf(EditProposalNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("rejectEditByManager Tests")
    class RejectEditByManagerTests {

        @Test
        @DisplayName("Should reject professor's edit and restore original values")
        void shouldRejectAndRestoreOriginal() {
            // Professor made the edit
            editProposal.setEditedBy(professorUser);
            editProposal.setOriginalStartTime(pendingReservation.getStartTime());
            editProposal.setOriginalEndTime(pendingReservation.getEndTime());
            editProposal.setOriginalDescription("Original description");
            editProposal.setOriginalWholeLab(true);
            editProposal.setOriginalWorkstationIds(List.of());
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.canManageReservation(labManagerUser, pendingReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING))
                    .thenReturn(Optional.of(editProposal));
            when(authorizationService.isReservationOwner(professorUser, pendingReservation)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
            when(editProposalRepository.save(any(ReservationEditProposal.class))).thenAnswer(i -> i.getArgument(0));

            editService.rejectEditByManager(reservationId, labManagerUser, "Not acceptable");

            // Verify reservation was restored to original values
            assertThat(pendingReservation.getStartTime()).isEqualTo(editProposal.getOriginalStartTime());
            assertThat(pendingReservation.getEndTime()).isEqualTo(editProposal.getOriginalEndTime());
            assertThat(pendingReservation.getDescription()).isEqualTo(editProposal.getOriginalDescription());
            // Status should be restored to original status
            assertThat(pendingReservation.getStatus()).isEqualTo(editProposal.getOriginalStatus());

            // Verify proposal was marked as rejected
            assertThat(editProposal.getResolution()).isEqualTo(ResolutionStatus.REJECTED);
            assertThat(editProposal.getResolvedBy()).isEqualTo(labManagerUser);
        }
    }

    @Nested
    @DisplayName("editReservationByProfessor Tests")
    class EditReservationByProfessorTests {

        private void setUpValidationMocks() {
            lenient().when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            lenient().when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
        }

        @Test
        @DisplayName("Should apply edit directly for PENDING reservation")
        void shouldApplyEditDirectlyForPendingReservation() {
            setUpValidationMocks();
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.isReservationOwner(professorUser, pendingReservation)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
            when(labManagerRepository.findByLab(testLab)).thenReturn(List.of());

            editService.editReservationByProfessor(reservationId, validEditRequest, professorUser);

            // Verify changes were applied directly
            assertThat(pendingReservation.getStartTime()).isEqualTo(validEditRequest.getStartTime());
            assertThat(pendingReservation.getEndTime()).isEqualTo(validEditRequest.getEndTime());
            assertThat(pendingReservation.getDescription()).isEqualTo(validEditRequest.getDescription());
            // Status should remain PENDING
            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.PENDING);

            // Should NOT create edit proposal
            verify(editProposalRepository, never()).save(any(ReservationEditProposal.class));
        }

        @Test
        @DisplayName("Should create edit proposal for APPROVED reservation")
        void shouldCreateEditProposalForApprovedReservation() {
            setUpValidationMocks();
            when(reservationRepository.findById(approvedReservation.getId())).thenReturn(Optional.of(approvedReservation));
            when(authorizationService.isReservationOwner(professorUser, approvedReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(approvedReservation.getId(), ResolutionStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(reservationWorkstationRepository.findWorkstationIdsByReservationId(approvedReservation.getId()))
                    .thenReturn(List.of());
            when(editProposalRepository.save(any(ReservationEditProposal.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
            when(labManagerRepository.findByLab(testLab)).thenReturn(List.of());

            editService.editReservationByProfessor(approvedReservation.getId(), validEditRequest, professorUser);

            // Verify edit proposal was created
            ArgumentCaptor<ReservationEditProposal> proposalCaptor = ArgumentCaptor.forClass(ReservationEditProposal.class);
            verify(editProposalRepository).save(proposalCaptor.capture());
            ReservationEditProposal savedProposal = proposalCaptor.getValue();
            
            assertThat(savedProposal.getEditedBy()).isEqualTo(professorUser);
            assertThat(savedProposal.getOriginalStatus()).isEqualTo(ReservationStatus.APPROVED);

            // Verify status changed to PENDING_EDIT_APPROVAL
            assertThat(approvedReservation.getStatus()).isEqualTo(ReservationStatus.PENDING_EDIT_APPROVAL);
        }

        @Test
        @DisplayName("Should throw BookingNotAuthorizedException when user is not owner")
        void shouldThrowWhenNotOwner() {
            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.isReservationOwner(labManagerUser, pendingReservation)).thenReturn(false);

            assertThatThrownBy(() -> editService.editReservationByProfessor(reservationId, validEditRequest, labManagerUser))
                    .isInstanceOf(BookingNotAuthorizedException.class)
                    .hasMessageContaining("only edit your own reservations");
        }

        @Test
        @DisplayName("Should throw IllegalStateException for non-editable status")
        void shouldThrowForNonEditableStatus() {
            setUpValidationMocks();
            Reservation rejectedReservation = new Reservation();
            rejectedReservation.setId(UUID.randomUUID());
            rejectedReservation.setLab(testLab);
            rejectedReservation.setUser(professorUser);
            rejectedReservation.setStatus(ReservationStatus.REJECTED);

            when(reservationRepository.findById(rejectedReservation.getId())).thenReturn(Optional.of(rejectedReservation));
            when(authorizationService.isReservationOwner(professorUser, rejectedReservation)).thenReturn(true);

            assertThatThrownBy(() -> editService.editReservationByProfessor(rejectedReservation.getId(), validEditRequest, professorUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING or APPROVED");
        }
    }

    @Nested
    @DisplayName("approveEditByProfessor Tests")
    class ApproveEditByProfessorTests {

        @Test
        @DisplayName("Should approve lab manager's edit successfully")
        void shouldApproveLabManagersEdit() {
            // Lab manager made the edit
            editProposal.setEditedBy(labManagerUser);
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.isReservationOwner(professorUser, pendingReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING))
                    .thenReturn(Optional.of(editProposal));
            // Lab manager is NOT the owner
            when(authorizationService.isReservationOwner(labManagerUser, pendingReservation)).thenReturn(false);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
            when(editProposalRepository.save(any(ReservationEditProposal.class))).thenAnswer(i -> i.getArgument(0));

            editService.approveEditByProfessor(reservationId, professorUser);

            // Verify reservation was updated with proposed values
            assertThat(pendingReservation.getStartTime()).isEqualTo(editProposal.getProposedStartTime());
            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.APPROVED);

            // Verify proposal was marked as approved
            assertThat(editProposal.getResolution()).isEqualTo(ResolutionStatus.APPROVED);
            assertThat(editProposal.getResolvedBy()).isEqualTo(professorUser);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when edit was made by owner (not lab manager)")
        void shouldThrowWhenEditMadeByOwner() {
            // Professor made the edit (not lab manager)
            editProposal.setEditedBy(professorUser);
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.isReservationOwner(professorUser, pendingReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING))
                    .thenReturn(Optional.of(editProposal));
            // Professor IS the owner AND the one who made the edit
            when(authorizationService.isReservationOwner(professorUser, pendingReservation)).thenReturn(true);

            assertThatThrownBy(() -> editService.approveEditByProfessor(reservationId, professorUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not created by a lab manager");
        }
    }

    @Nested
    @DisplayName("rejectEditByProfessor Tests")
    class RejectEditByProfessorTests {

        @Test
        @DisplayName("Should reject lab manager's edit and restore original values")
        void shouldRejectAndRestoreOriginal() {
            // Lab manager made the edit
            editProposal.setEditedBy(labManagerUser);
            editProposal.setOriginalStatus(ReservationStatus.PENDING);
            editProposal.setOriginalWorkstationIds(List.of());
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);

            when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(pendingReservation));
            when(authorizationService.isReservationOwner(professorUser, pendingReservation)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(reservationId, ResolutionStatus.PENDING))
                    .thenReturn(Optional.of(editProposal));
            when(authorizationService.isReservationOwner(labManagerUser, pendingReservation)).thenReturn(false);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
            when(editProposalRepository.save(any(ReservationEditProposal.class))).thenAnswer(i -> i.getArgument(0));

            editService.rejectEditByProfessor(reservationId, professorUser, "Don't like the changes");

            // Verify reservation was restored to original values
            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.PENDING);

            // Verify proposal was marked as rejected
            assertThat(editProposal.getResolution()).isEqualTo(ResolutionStatus.REJECTED);
            assertThat(editProposal.getResolvedBy()).isEqualTo(professorUser);
        }
    }

    @Nested
    @DisplayName("editRecurringGroupByManager Tests")
    class EditRecurringGroupByManagerTests {

        private void setUpValidationMocks() {
            lenient().when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            lenient().when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
        }

        @Test
        @DisplayName("Should create edit proposals for all reservations in group")
        void shouldCreateEditProposalsForGroup() {
            setUpValidationMocks();
            UUID recurringGroupId = UUID.randomUUID();
            Reservation recurring1 = createRecurringReservation(recurringGroupId);
            Reservation recurring2 = createRecurringReservation(recurringGroupId);

            when(reservationRepository.findByRecurringGroupId(recurringGroupId))
                    .thenReturn(List.of(recurring1, recurring2));
            when(authorizationService.canManageReservation(labManagerUser, recurring1)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(any(), eq(ResolutionStatus.PENDING)))
                    .thenReturn(Optional.empty());
            when(reservationWorkstationRepository.findWorkstationIdsByReservationId(any()))
                    .thenReturn(List.of());
            when(editProposalRepository.save(any(ReservationEditProposal.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

            editService.editRecurringGroupByManager(recurringGroupId, validEditRequest, labManagerUser);

            // Verify edit proposals were created for both reservations
            verify(editProposalRepository, times(2)).save(any(ReservationEditProposal.class));
            verify(reservationRepository, times(2)).save(any(Reservation.class));

            // Verify status changed for both
            assertThat(recurring1.getStatus()).isEqualTo(ReservationStatus.PENDING_EDIT_APPROVAL);
            assertThat(recurring2.getStatus()).isEqualTo(ReservationStatus.PENDING_EDIT_APPROVAL);
        }

        @Test
        @DisplayName("Should throw ReservationNotFoundException when no reservations in group")
        void shouldThrowWhenNoReservationsInGroup() {
            UUID recurringGroupId = UUID.randomUUID();
            when(reservationRepository.findByRecurringGroupId(recurringGroupId)).thenReturn(List.of());

            assertThatThrownBy(() -> editService.editRecurringGroupByManager(recurringGroupId, validEditRequest, labManagerUser))
                    .isInstanceOf(ReservationNotFoundException.class)
                    .hasMessageContaining("recurring group");
        }
    }

    @Nested
    @DisplayName("editRecurringGroupByProfessor Tests")
    class EditRecurringGroupByProfessorTests {

        private void setUpValidationMocks() {
            lenient().when(labOperatingHoursRepository.findByLabIdAndDayOfWeek(anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            lenient().when(labClosedDayRepository.isLabClosedOnDate(anyInt(), any(), anyInt())).thenReturn(false);
        }

        @Test
        @DisplayName("Should apply edit directly for all PENDING reservations in group")
        void shouldApplyEditDirectlyForPendingGroup() {
            setUpValidationMocks();
            UUID recurringGroupId = UUID.randomUUID();
            Reservation recurring1 = createRecurringReservation(recurringGroupId);
            recurring1.setStatus(ReservationStatus.PENDING);
            Reservation recurring2 = createRecurringReservation(recurringGroupId);
            recurring2.setStatus(ReservationStatus.PENDING);

            when(reservationRepository.findByRecurringGroupId(recurringGroupId))
                    .thenReturn(List.of(recurring1, recurring2));
            when(authorizationService.isReservationOwner(professorUser, recurring1)).thenReturn(true);
            when(authorizationService.isReservationOwner(professorUser, recurring2)).thenReturn(true);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
            when(labManagerRepository.findByLab(testLab)).thenReturn(List.of());

            editService.editRecurringGroupByProfessor(recurringGroupId, validEditRequest, professorUser);

            // Verify changes were applied directly
            assertThat(recurring1.getDescription()).isEqualTo(validEditRequest.getDescription());
            assertThat(recurring2.getDescription()).isEqualTo(validEditRequest.getDescription());
            
            // Status should remain PENDING
            assertThat(recurring1.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(recurring2.getStatus()).isEqualTo(ReservationStatus.PENDING);

            // Should NOT create edit proposals
            verify(editProposalRepository, never()).save(any(ReservationEditProposal.class));
        }

        @Test
        @DisplayName("Should create edit proposals for APPROVED reservations in mixed group")
        void shouldCreateEditProposalsForApprovedInMixedGroup() {
            setUpValidationMocks();
            UUID recurringGroupId = UUID.randomUUID();
            Reservation pendingRecurring = createRecurringReservation(recurringGroupId);
            pendingRecurring.setStatus(ReservationStatus.PENDING);
            Reservation approvedRecurring = createRecurringReservation(recurringGroupId);
            approvedRecurring.setStatus(ReservationStatus.APPROVED);

            when(reservationRepository.findByRecurringGroupId(recurringGroupId))
                    .thenReturn(List.of(pendingRecurring, approvedRecurring));
            when(authorizationService.isReservationOwner(professorUser, pendingRecurring)).thenReturn(true);
            when(authorizationService.isReservationOwner(professorUser, approvedRecurring)).thenReturn(true);
            when(editProposalRepository.findByReservationIdAndResolution(approvedRecurring.getId(), ResolutionStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(reservationWorkstationRepository.findWorkstationIdsByReservationId(approvedRecurring.getId()))
                    .thenReturn(List.of());
            when(editProposalRepository.save(any(ReservationEditProposal.class)))
                    .thenAnswer(i -> i.getArgument(0));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
            when(labManagerRepository.findByLab(testLab)).thenReturn(List.of());

            editService.editRecurringGroupByProfessor(recurringGroupId, validEditRequest, professorUser);

            // PENDING reservation should be updated directly
            assertThat(pendingRecurring.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(pendingRecurring.getDescription()).isEqualTo(validEditRequest.getDescription());

            // APPROVED reservation should have edit proposal created
            assertThat(approvedRecurring.getStatus()).isEqualTo(ReservationStatus.PENDING_EDIT_APPROVAL);
            verify(editProposalRepository, times(1)).save(any(ReservationEditProposal.class));
        }
    }

    // Helper method
    private Reservation createRecurringReservation(UUID groupId) {
        OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setLab(testLab);
        reservation.setUser(professorUser);
        reservation.setRecurringGroupId(groupId);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setStartTime(tomorrow);
        reservation.setEndTime(tomorrow.plusHours(2));
        reservation.setDescription("Recurring description");
        reservation.setWholeLab(true);
        return reservation;
    }
}
