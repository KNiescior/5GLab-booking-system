package com._glab.booking_system.booking.controller;

import com._glab.booking_system.auth.config.TestJwtConfig;
import com._glab.booking_system.auth.config.TestMailConfig;
import com._glab.booking_system.auth.service.JwtService;
import com._glab.booking_system.booking.model.*;
import com._glab.booking_system.booking.repository.*;
import com._glab.booking_system.booking.request.*;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.RoleRepository;
import com._glab.booking_system.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for professor edit endpoints in ReservationController.
 * Tests the workflow where professors edit their own reservations
 * and approve/reject lab manager edits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@Import({TestJwtConfig.class, TestMailConfig.class})
class ReservationEditControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private LabRepository labRepository;

    @Autowired
    private WorkstationRepository workstationRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationWorkstationRepository reservationWorkstationRepository;

    @Autowired
    private LabManagerRepository labManagerRepository;

    @Autowired
    private ReservationEditProposalRepository editProposalRepository;

    @Autowired
    private RecurringPatternRepository recurringPatternRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User professor;
    private User otherProfessor;
    private User labManager;
    private Role professorRole;
    private Role labManagerRole;
    private Building testBuilding;
    private Lab testLab;
    private Workstation workstation1;
    private String professorToken;
    private String otherProfessorToken;

    @BeforeEach
    void setUp() {
        // Clean up in correct order
        editProposalRepository.deleteAll();
        reservationWorkstationRepository.deleteAll();
        reservationRepository.deleteAll();
        recurringPatternRepository.deleteAll();
        labManagerRepository.deleteAll();
        workstationRepository.deleteAll();
        labRepository.deleteAll();
        buildingRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        professorRole = new Role();
        professorRole.setName(RoleName.PROFESSOR);
        professorRole = roleRepository.save(professorRole);

        labManagerRole = new Role();
        labManagerRole.setName(RoleName.LAB_MANAGER);
        labManagerRole = roleRepository.save(labManagerRole);

        // Create professor user
        professor = new User();
        professor.setEmail("professor@test.com");
        professor.setUsername("professor");
        professor.setPassword(passwordEncoder.encode("password123"));
        professor.setFirstName("Test");
        professor.setLastName("Professor");
        professor.setEnabled(true);
        professor.setRole(professorRole);
        professor = userRepository.save(professor);
        professorToken = jwtService.generateAccessToken(professor);

        // Create another professor (for testing non-owner scenarios)
        otherProfessor = new User();
        otherProfessor.setEmail("other.professor@test.com");
        otherProfessor.setUsername("otherprofessor");
        otherProfessor.setPassword(passwordEncoder.encode("password123"));
        otherProfessor.setFirstName("Other");
        otherProfessor.setLastName("Professor");
        otherProfessor.setEnabled(true);
        otherProfessor.setRole(professorRole);
        otherProfessor = userRepository.save(otherProfessor);
        otherProfessorToken = jwtService.generateAccessToken(otherProfessor);

        // Create lab manager user
        labManager = new User();
        labManager.setEmail("manager@test.com");
        labManager.setUsername("manager");
        labManager.setPassword(passwordEncoder.encode("password123"));
        labManager.setFirstName("Lab");
        labManager.setLastName("Manager");
        labManager.setEnabled(true);
        labManager.setRole(labManagerRole);
        labManager = userRepository.save(labManager);

        // Create building
        testBuilding = new Building();
        testBuilding.setName("Test Building");
        testBuilding.setAddress("123 Test St");
        testBuilding = buildingRepository.save(testBuilding);

        // Create lab
        testLab = new Lab();
        testLab.setName("Test Lab");
        testLab.setBuilding(testBuilding);
        testLab.setDefaultOpenTime(LocalTime.of(8, 0));
        testLab.setDefaultCloseTime(LocalTime.of(20, 0));
        testLab = labRepository.save(testLab);

        // Create workstation
        workstation1 = new Workstation();
        workstation1.setLab(testLab);
        workstation1.setIdentifier("WS-001");
        workstation1.setDescription("Workstation 1");
        workstation1.setActive(true);
        workstation1 = workstationRepository.save(workstation1);

        // Assign lab manager to test lab
        LabManager labManagerAssignment = new LabManager();
        labManagerAssignment.setUser(labManager);
        labManagerAssignment.setLab(testLab);
        labManagerAssignment.setIsPrimary(true);
        labManagerRepository.save(labManagerAssignment);
    }

    @Nested
    @DisplayName("Professor Edit Own Reservation Tests")
    class ProfessorEditOwnReservationTests {

        @Test
        @DisplayName("Should apply edit directly when editing PENDING reservation")
        void shouldApplyEditDirectlyForPendingReservation() throws Exception {
            Reservation reservation = createPendingReservation(testLab, professor);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .description("Updated by professor")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit", reservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify changes applied directly (no edit proposal, status stays PENDING)
            Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING, updated.getStatus());
            Assertions.assertEquals("Updated by professor", updated.getDescription());

            // Verify no edit proposal was created
            Optional<ReservationEditProposal> proposal = editProposalRepository
                    .findByReservationIdAndResolution(reservation.getId(), ResolutionStatus.PENDING);
            Assertions.assertFalse(proposal.isPresent());
        }

        @Test
        @DisplayName("Should create edit proposal when editing APPROVED reservation")
        void shouldCreateEditProposalForApprovedReservation() throws Exception {
            Reservation reservation = createApprovedReservation(testLab, professor);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .description("Professor wants to change time")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit", reservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify status changed to PENDING_EDIT_APPROVAL
            Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated.getStatus());

            // Verify edit proposal was created
            Optional<ReservationEditProposal> proposal = editProposalRepository
                    .findByReservationIdAndResolution(reservation.getId(), ResolutionStatus.PENDING);
            Assertions.assertTrue(proposal.isPresent());
            Assertions.assertEquals(professor.getId(), proposal.get().getEditedBy().getId());
        }

        @Test
        @DisplayName("Should return 403 when editing someone else's reservation")
        void shouldReturn403WhenEditingOthersReservation() throws Exception {
            // Create reservation owned by professor
            Reservation reservation = createPendingReservation(testLab, professor);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .wholeLab(true)
                    .build();

            // Other professor tries to edit
            mockMvc.perform(post("/api/v1/reservations/{id}/edit", reservation.getId())
                            .header("Authorization", "Bearer " + otherProfessorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when reservation already has pending edit")
        void shouldReturn400WhenEditProposalAlreadyExists() throws Exception {
            Reservation reservation = createApprovedReservation(testLab, professor);

            // Create existing edit proposal
            createEditProposal(reservation, labManager, OffsetDateTime.now(ZoneOffset.UTC).plusDays(3));
            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit", reservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 for non-existent reservation")
        void shouldReturn404ForNonExistentReservation() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit", nonExistentId)
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Professor Approve Lab Manager Edit Tests")
    class ProfessorApproveEditTests {

        @Test
        @DisplayName("Should approve lab manager's edit")
        void shouldApproveLabManagersEdit() throws Exception {
            Reservation reservation = createApprovedReservation(testLab, professor);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            // Lab manager creates edit proposal
            createEditProposal(reservation, labManager, newStartTime);
            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            mockMvc.perform(post("/api/v1/reservations/{id}/edit/approve", reservation.getId())
                            .header("Authorization", "Bearer " + professorToken))
                    .andExpect(status().isNoContent());

            // Verify reservation is approved with new values
            Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
        }

        @Test
        @DisplayName("Should return 403 when non-owner tries to approve")
        void shouldReturn403WhenNonOwnerTriesToApprove() throws Exception {
            Reservation reservation = createApprovedReservation(testLab, professor);

            createEditProposal(reservation, labManager, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            // Other professor tries to approve
            mockMvc.perform(post("/api/v1/reservations/{id}/edit/approve", reservation.getId())
                            .header("Authorization", "Bearer " + otherProfessorToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when no edit proposal exists")
        void shouldReturn404WhenNoEditProposalExists() throws Exception {
            Reservation reservation = createApprovedReservation(testLab, professor);

            mockMvc.perform(post("/api/v1/reservations/{id}/edit/approve", reservation.getId())
                            .header("Authorization", "Bearer " + professorToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Professor Reject Lab Manager Edit Tests")
    class ProfessorRejectEditTests {

        @Test
        @DisplayName("Should reject lab manager's edit and restore original")
        void shouldRejectLabManagersEdit() throws Exception {
            Reservation reservation = createApprovedReservation(testLab, professor);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            // Lab manager creates edit proposal
            createEditProposal(reservation, labManager, newStartTime);
            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            RejectEditRequest request = RejectEditRequest.builder()
                    .reason("I prefer the original time")
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit/reject", reservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify reservation is restored
            Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
        }

        @Test
        @DisplayName("Should reject without reason")
        void shouldRejectWithoutReason() throws Exception {
            Reservation reservation = createApprovedReservation(testLab, professor);

            createEditProposal(reservation, labManager, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            mockMvc.perform(post("/api/v1/reservations/{id}/edit/reject", reservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 403 when non-owner tries to reject")
        void shouldReturn403WhenNonOwnerTriesToReject() throws Exception {
            Reservation reservation = createApprovedReservation(testLab, professor);

            createEditProposal(reservation, labManager, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            mockMvc.perform(post("/api/v1/reservations/{id}/edit/reject", reservation.getId())
                            .header("Authorization", "Bearer " + otherProfessorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Professor Edit Recurring Group Tests")
    class ProfessorEditRecurringGroupTests {

        @Test
        @DisplayName("Should apply edit directly for all PENDING reservations in group")
        void shouldApplyEditDirectlyForPendingGroup() throws Exception {
            UUID groupId = UUID.randomUUID();
            Reservation reservation1 = createPendingReservationWithGroup(testLab, professor, groupId);
            Reservation reservation2 = createPendingReservationWithGroup(testLab, professor, groupId);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .description("Group update by professor")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/recurring/{groupId}/edit", groupId)
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify changes applied directly to PENDING reservations
            Reservation updated1 = reservationRepository.findById(reservation1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(reservation2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.PENDING, updated2.getStatus());
            Assertions.assertEquals("Group update by professor", updated1.getDescription());
        }

        @Test
        @DisplayName("Should create edit proposals for APPROVED reservations in group")
        void shouldCreateEditProposalsForApprovedGroup() throws Exception {
            UUID groupId = UUID.randomUUID();
            Reservation reservation1 = createApprovedReservationWithGroup(testLab, professor, groupId);
            Reservation reservation2 = createApprovedReservationWithGroup(testLab, professor, groupId);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .description("Group update needs approval")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/recurring/{groupId}/edit", groupId)
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify edit proposals created for APPROVED reservations
            Reservation updated1 = reservationRepository.findById(reservation1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(reservation2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated2.getStatus());
        }

        @Test
        @DisplayName("Should return 403 when editing someone else's recurring group")
        void shouldReturn403WhenEditingOthersRecurringGroup() throws Exception {
            UUID groupId = UUID.randomUUID();
            createPendingReservationWithGroup(testLab, professor, groupId);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .wholeLab(true)
                    .build();

            // Other professor tries to edit
            mockMvc.perform(post("/api/v1/reservations/recurring/{groupId}/edit", groupId)
                            .header("Authorization", "Bearer " + otherProfessorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Professor Approve/Reject Recurring Group Edit Tests")
    class ProfessorApproveRejectRecurringGroupEditTests {

        @Test
        @DisplayName("Should approve lab manager's recurring group edit")
        void shouldApproveLabManagersRecurringGroupEdit() throws Exception {
            UUID groupId = UUID.randomUUID();
            Reservation reservation1 = createApprovedReservationWithGroup(testLab, professor, groupId);
            Reservation reservation2 = createApprovedReservationWithGroup(testLab, professor, groupId);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            // Lab manager creates edit proposals
            createEditProposal(reservation1, labManager, newStartTime);
            createEditProposal(reservation2, labManager, newStartTime);
            reservation1.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservation2.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation1);
            reservationRepository.save(reservation2);

            mockMvc.perform(post("/api/v1/reservations/recurring/{groupId}/edit/approve", groupId)
                            .header("Authorization", "Bearer " + professorToken))
                    .andExpect(status().isNoContent());

            // Verify all reservations approved
            Reservation updated1 = reservationRepository.findById(reservation1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(reservation2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.APPROVED, updated2.getStatus());
        }

        @Test
        @DisplayName("Should reject lab manager's recurring group edit")
        void shouldRejectLabManagersRecurringGroupEdit() throws Exception {
            UUID groupId = UUID.randomUUID();
            Reservation reservation1 = createApprovedReservationWithGroup(testLab, professor, groupId);
            Reservation reservation2 = createApprovedReservationWithGroup(testLab, professor, groupId);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            // Lab manager creates edit proposals
            createEditProposal(reservation1, labManager, newStartTime);
            createEditProposal(reservation2, labManager, newStartTime);
            reservation1.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservation2.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation1);
            reservationRepository.save(reservation2);

            RejectEditRequest request = RejectEditRequest.builder()
                    .reason("I prefer the original times")
                    .build();

            mockMvc.perform(post("/api/v1/reservations/recurring/{groupId}/edit/reject", groupId)
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify all reservations restored
            Reservation updated1 = reservationRepository.findById(reservation1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(reservation2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.APPROVED, updated2.getStatus());
        }

        @Test
        @DisplayName("Should return 403 when non-owner tries to approve group edit")
        void shouldReturn403WhenNonOwnerTriesToApproveGroupEdit() throws Exception {
            UUID groupId = UUID.randomUUID();
            Reservation reservation = createApprovedReservationWithGroup(testLab, professor, groupId);

            createEditProposal(reservation, labManager, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));
            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            mockMvc.perform(post("/api/v1/reservations/recurring/{groupId}/edit/approve", groupId)
                            .header("Authorization", "Bearer " + otherProfessorToken))
                    .andExpect(status().isForbidden());
        }
    }

    // Helper methods

    private Reservation createPendingReservation(Lab lab, User user) {
        OffsetDateTime startTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime endTime = startTime.plusHours(2);

        Reservation reservation = new Reservation();
        reservation.setLab(lab);
        reservation.setUser(user);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setDescription("Test reservation");
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setWholeLab(true);
        return reservationRepository.save(reservation);
    }

    private Reservation createApprovedReservation(Lab lab, User user) {
        Reservation reservation = createPendingReservation(lab, user);
        reservation.setStatus(ReservationStatus.APPROVED);
        return reservationRepository.save(reservation);
    }

    private Reservation createPendingReservationWithGroup(Lab lab, User user, UUID groupId) {
        Reservation reservation = createPendingReservation(lab, user);
        reservation.setRecurringGroupId(groupId);
        return reservationRepository.save(reservation);
    }

    private Reservation createApprovedReservationWithGroup(Lab lab, User user, UUID groupId) {
        Reservation reservation = createApprovedReservation(lab, user);
        reservation.setRecurringGroupId(groupId);
        return reservationRepository.save(reservation);
    }

    private ReservationEditProposal createEditProposal(Reservation reservation, User editor, OffsetDateTime proposedStartTime) {
        ReservationEditProposal proposal = new ReservationEditProposal();
        proposal.setReservation(reservation);
        proposal.setEditedBy(editor);
        proposal.setOriginalStatus(reservation.getStatus());
        proposal.setOriginalStartTime(reservation.getStartTime());
        proposal.setOriginalEndTime(reservation.getEndTime());
        proposal.setOriginalDescription(reservation.getDescription());
        proposal.setOriginalWholeLab(reservation.getWholeLab());
        proposal.setOriginalWorkstationIds(new ArrayList<>());
        proposal.setProposedStartTime(proposedStartTime);
        proposal.setProposedEndTime(proposedStartTime.plusHours(2));
        proposal.setProposedDescription("Updated by editor");
        proposal.setProposedWholeLab(true);
        proposal.setProposedWorkstationIds(new ArrayList<>());
        proposal.setResolution(ResolutionStatus.PENDING);
        return editProposalRepository.save(proposal);
    }
}


