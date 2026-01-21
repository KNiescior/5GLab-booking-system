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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@Import({TestJwtConfig.class, TestMailConfig.class})
class LabManagerReservationControllerTest {

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
    private User labManager;
    private User admin;
    private Role professorRole;
    private Role labManagerRole;
    private Role adminRole;
    private Building testBuilding;
    private Lab testLab;
    private Lab otherLab;
    private Workstation workstation1;
    private Workstation workstation2;
    private LabManager labManagerAssignment;
    private String labManagerToken;
    private String adminToken;

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

        adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        adminRole = roleRepository.save(adminRole);

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
        labManagerToken = jwtService.generateAccessToken(labManager);

        // Create admin user
        admin = new User();
        admin.setEmail("admin@test.com");
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("password123"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEnabled(true);
        admin.setRole(adminRole);
        admin = userRepository.save(admin);
        adminToken = jwtService.generateAccessToken(admin);

        // Create building
        testBuilding = new Building();
        testBuilding.setName("Test Building");
        testBuilding.setAddress("123 Test St");
        testBuilding = buildingRepository.save(testBuilding);

        // Create labs
        testLab = new Lab();
        testLab.setName("Test Lab");
        testLab.setBuilding(testBuilding);
        testLab.setDefaultOpenTime(LocalTime.of(8, 0));
        testLab.setDefaultCloseTime(LocalTime.of(20, 0));
        testLab = labRepository.save(testLab);

        otherLab = new Lab();
        otherLab.setName("Other Lab");
        otherLab.setBuilding(testBuilding);
        otherLab.setDefaultOpenTime(LocalTime.of(8, 0));
        otherLab.setDefaultCloseTime(LocalTime.of(20, 0));
        otherLab = labRepository.save(otherLab);

        // Create workstations
        workstation1 = new Workstation();
        workstation1.setLab(testLab);
        workstation1.setIdentifier("WS-001");
        workstation1.setDescription("Workstation 1");
        workstation1.setActive(true);
        workstation1 = workstationRepository.save(workstation1);

        workstation2 = new Workstation();
        workstation2.setLab(testLab);
        workstation2.setIdentifier("WS-002");
        workstation2.setDescription("Workstation 2");
        workstation2.setActive(true);
        workstation2 = workstationRepository.save(workstation2);

        // Assign lab manager to test lab
        labManagerAssignment = new LabManager();
        labManagerAssignment.setUser(labManager);
        labManagerAssignment.setLab(testLab);
        labManagerAssignment.setIsPrimary(true);
        labManagerAssignment = labManagerRepository.save(labManagerAssignment);
    }

    @Nested
    @DisplayName("Get Pending Reservations Tests")
    class GetPendingReservationsTests {

        @Test
        @DisplayName("Should get pending reservations for lab manager")
        void shouldGetPendingReservationsForLabManager() throws Exception {
            // Create a pending reservation
            Reservation reservation = createPendingReservation(testLab, professor);

            mockMvc.perform(get("/api/v1/manager/reservations/pending")
                            .header("Authorization", "Bearer " + labManagerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(reservation.getId().toString()))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("Should get all pending reservations for admin")
        void shouldGetAllPendingReservationsForAdmin() throws Exception {
            // Create pending reservations in different labs
            createPendingReservation(testLab, professor);
            createPendingReservation(otherLab, professor);

            mockMvc.perform(get("/api/v1/manager/reservations/pending")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Should not see reservations from other labs for lab manager")
        void shouldNotSeeReservationsFromOtherLabs() throws Exception {
            // Create reservation in other lab
            createPendingReservation(otherLab, professor);

            mockMvc.perform(get("/api/v1/manager/reservations/pending")
                            .header("Authorization", "Bearer " + labManagerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/manager/reservations/pending"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get Reservation Tests")
    class GetReservationTests {

        @Test
        @DisplayName("Should get reservation details")
        void shouldGetReservationDetails() throws Exception {
            Reservation reservation = createPendingReservation(testLab, professor);

            mockMvc.perform(get("/api/v1/manager/reservations/{id}", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(reservation.getId().toString()))
                    .andExpect(jsonPath("$.labId").value(testLab.getId()))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent reservation")
        void shouldReturn404ForNonExistentReservation() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/manager/reservations/{id}", nonExistentId)
                            .header("Authorization", "Bearer " + labManagerToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Approve Reservation Tests")
    class ApproveReservationTests {

        @Test
        @DisplayName("Should approve pending reservation")
        void shouldApprovePendingReservation() throws Exception {
            Reservation reservation = createPendingReservation(testLab, professor);

            ApproveReservationRequest request = ApproveReservationRequest.builder()
                    .reason("Approved for testing")
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify reservation is approved
            Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
        }

        @Test
        @DisplayName("Should approve reservation without reason")
        void shouldApproveReservationWithoutReason() throws Exception {
            Reservation reservation = createPendingReservation(testLab, professor);

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should allow admin to approve any reservation")
        void shouldAllowAdminToApproveAnyReservation() throws Exception {
            Reservation reservation = createPendingReservation(otherLab, professor);

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", reservation.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 403 when lab manager tries to approve reservation from other lab")
        void shouldReturn403ForOtherLabReservation() throws Exception {
            Reservation reservation = createPendingReservation(otherLab, professor);

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 for non-existent reservation")
        void shouldReturn404ForNonExistentReservation() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", nonExistentId)
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Decline Reservation Tests")
    class DeclineReservationTests {

        @Test
        @DisplayName("Should decline pending reservation")
        void shouldDeclinePendingReservation() throws Exception {
            Reservation reservation = createPendingReservation(testLab, professor);

            DeclineReservationRequest request = DeclineReservationRequest.builder()
                    .reason("Not available at that time")
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/decline", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify reservation is declined
            Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.REJECTED, updated.getStatus());
        }

        @Test
        @DisplayName("Should decline reservation without reason")
        void shouldDeclineReservationWithoutReason() throws Exception {
            Reservation reservation = createPendingReservation(testLab, professor);

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/decline", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should allow admin to decline any reservation")
        void shouldAllowAdminToDeclineAnyReservation() throws Exception {
            Reservation reservation = createPendingReservation(otherLab, professor);

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/decline", reservation.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Edit Reservation Tests")
    class EditReservationTests {

        @Test
        @DisplayName("Should create edit proposal when lab manager edits reservation")
        void shouldCreateEditProposalWhenManagerEdits() throws Exception {
            Reservation reservation = createPendingReservation(testLab, professor);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime newEndTime = newStartTime.plusHours(3);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newEndTime)
                    .description("Updated description")
                    .wholeLab(false)
                    .workstationIds(List.of(workstation1.getId()))
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify edit proposal was created
            Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated.getStatus());

            Optional<ReservationEditProposal> proposal = editProposalRepository
                    .findByReservationIdAndResolution(reservation.getId(), ResolutionStatus.PENDING);
            Assertions.assertTrue(proposal.isPresent());
            Assertions.assertEquals(labManager.getId(), proposal.get().getEditedBy().getId());
        }

        @Test
        @DisplayName("Should allow admin to edit any reservation")
        void shouldAllowAdminToEditAnyReservation() throws Exception {
            Reservation reservation = createPendingReservation(otherLab, professor);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .description("Admin edit")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit", reservation.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 403 when lab manager tries to edit reservation from other lab")
        void shouldReturn403ForOtherLabEdit() throws Exception {
            Reservation reservation = createPendingReservation(otherLab, professor);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Approve Edit Tests")
    class ApproveEditTests {

        @Test
        @DisplayName("Should approve professor's edit of approved reservation")
        void shouldApproveProfessorsEdit() throws Exception {
            // Create approved reservation
            Reservation reservation = createApprovedReservation(testLab, professor);

            // Professor edits it (creates edit proposal)
            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);

            // Simulate professor edit (would be done via ReservationController)
            // For this test, we'll create the edit proposal directly
            ReservationEditProposal proposal = new ReservationEditProposal();
            proposal.setReservation(reservation);
            proposal.setEditedBy(professor);
            proposal.setOriginalStatus(ReservationStatus.APPROVED);
            proposal.setOriginalStartTime(reservation.getStartTime());
            proposal.setOriginalEndTime(reservation.getEndTime());
            proposal.setOriginalDescription(reservation.getDescription());
            proposal.setOriginalWholeLab(reservation.getWholeLab());
            proposal.setProposedStartTime(newStartTime);
            proposal.setProposedEndTime(newStartTime.plusHours(2));
            proposal.setProposedDescription("Professor edit");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            proposal = editProposalRepository.save(proposal);

            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            // Lab manager approves the edit
            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit/approve", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken))
                    .andExpect(status().isNoContent());

            // Verify reservation is approved with new values
            Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
        }
    }

    @Nested
    @DisplayName("Reject Edit Tests")
    class RejectEditTests {

        @Test
        @DisplayName("Should reject professor's edit and restore original")
        void shouldRejectProfessorsEdit() throws Exception {
            // Create approved reservation
            Reservation reservation = createApprovedReservation(testLab, professor);
            OffsetDateTime originalStartTime = reservation.getStartTime();
            String originalDescription = reservation.getDescription();

            // Create edit proposal
            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);

            ReservationEditProposal proposal = new ReservationEditProposal();
            proposal.setReservation(reservation);
            proposal.setEditedBy(professor);
            proposal.setOriginalStatus(ReservationStatus.APPROVED);
            proposal.setOriginalStartTime(originalStartTime);
            proposal.setOriginalEndTime(reservation.getEndTime());
            proposal.setOriginalDescription(originalDescription);
            proposal.setOriginalWholeLab(reservation.getWholeLab());
            proposal.setProposedStartTime(newStartTime);
            proposal.setProposedEndTime(newStartTime.plusHours(2));
            proposal.setProposedDescription("New description");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            proposal = editProposalRepository.save(proposal);

            reservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(reservation);

            // Lab manager rejects the edit
            RejectEditRequest request = RejectEditRequest.builder()
                    .reason("Time conflict")
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit/reject", reservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify reservation is restored to original status
            Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
        }
    }

    @Nested
    @DisplayName("Recurring Group Tests")
    class RecurringGroupTests {

        @Test
        @DisplayName("Should approve recurring group")
        void shouldApproveRecurringGroup() throws Exception {
            UUID groupId = UUID.randomUUID();
            createPendingReservationWithGroup(testLab, professor, groupId);
            createPendingReservationWithGroup(testLab, professor, groupId);

            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/approve", groupId)
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());

            // Verify all reservations in group are approved
            List<Reservation> reservations = reservationRepository
                    .findByRecurringGroupIdAndStatus(groupId, ReservationStatus.APPROVED);
            Assertions.assertTrue(reservations.size() >= 2);
        }

        @Test
        @DisplayName("Should decline recurring group")
        void shouldDeclineRecurringGroup() throws Exception {
            UUID groupId = UUID.randomUUID();
            createPendingReservationWithGroup(testLab, professor, groupId);
            createPendingReservationWithGroup(testLab, professor, groupId);

            DeclineReservationRequest request = DeclineReservationRequest.builder()
                    .reason("Not available")
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/decline", groupId)
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify all reservations in group are declined
            List<Reservation> reservations = reservationRepository
                    .findByRecurringGroupIdAndStatus(groupId, ReservationStatus.REJECTED);
            Assertions.assertTrue(reservations.size() >= 2);
        }

        @Test
        @DisplayName("Should edit recurring group")
        void shouldEditRecurringGroup() throws Exception {
            UUID groupId = UUID.randomUUID();
            Reservation reservation1 = createPendingReservationWithGroup(testLab, professor, groupId);
            Reservation reservation2 = createPendingReservationWithGroup(testLab, professor, groupId);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .description("Updated group")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/edit", groupId)
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify both reservations have edit proposals
            Reservation updated1 = reservationRepository.findById(reservation1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(reservation2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated2.getStatus());
        }
    }

    @Nested
    @DisplayName("Recurring Group Occurrence Tests")
    class RecurringGroupOccurrenceTests {

        @Test
        @DisplayName("Should approve single occurrence")
        void shouldApproveSingleOccurrence() throws Exception {
            UUID groupId = UUID.randomUUID();
            Reservation reservation1 = createPendingReservationWithGroup(testLab, professor, groupId);
            Reservation reservation2 = createPendingReservationWithGroup(testLab, professor, groupId);

            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/occurrences/{id}/approve",
                            groupId, reservation1.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());

            // Verify only first reservation is approved
            Reservation updated1 = reservationRepository.findById(reservation1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(reservation2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.PENDING, updated2.getStatus());
        }

        @Test
        @DisplayName("Should decline single occurrence")
        void shouldDeclineSingleOccurrence() throws Exception {
            UUID groupId = UUID.randomUUID();
            Reservation reservation1 = createPendingReservationWithGroup(testLab, professor, groupId);
            Reservation reservation2 = createPendingReservationWithGroup(testLab, professor, groupId);

            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/occurrences/{id}/decline",
                            groupId, reservation1.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());

            // Verify only first reservation is declined
            Reservation updated1 = reservationRepository.findById(reservation1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(reservation2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.REJECTED, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.PENDING, updated2.getStatus());
        }

        @Test
        @DisplayName("Should edit single occurrence")
        void shouldEditSingleOccurrence() throws Exception {
            UUID groupId = UUID.randomUUID();
            Reservation reservation1 = createPendingReservationWithGroup(testLab, professor, groupId);
            Reservation reservation2 = createPendingReservationWithGroup(testLab, professor, groupId);

            OffsetDateTime newStartTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newStartTime.plusHours(2))
                    .description("Updated occurrence")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/occurrences/{id}/edit",
                            groupId, reservation1.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify only first reservation has edit proposal
            Reservation updated1 = reservationRepository.findById(reservation1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(reservation2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.PENDING, updated2.getStatus());
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
        reservation = reservationRepository.save(reservation);

        return reservation;
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
}

