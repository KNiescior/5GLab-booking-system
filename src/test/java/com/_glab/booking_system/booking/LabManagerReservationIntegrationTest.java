package com._glab.booking_system.booking;

import com._glab.booking_system.auth.config.TestJwtConfig;
import com._glab.booking_system.auth.config.TestMailConfig;
import com._glab.booking_system.auth.service.JwtService;
import com._glab.booking_system.booking.model.*;
import com._glab.booking_system.booking.repository.*;
import com._glab.booking_system.booking.request.DeclineReservationRequest;
import com._glab.booking_system.booking.request.EditReservationRequest;
import com._glab.booking_system.booking.request.RejectEditRequest;
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
class LabManagerReservationIntegrationTest {

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
    private ReservationEditProposalRepository editProposalRepository;

    @Autowired
    private LabManagerRepository labManagerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User professorUser;
    private User labManagerUser;
    private User adminUser;
    private Role professorRole;
    private Role labManagerRole;
    private Role adminRole;
    private Building testBuilding;
    private Lab testLab;
    private Workstation workstation1;
    private Reservation pendingReservation;
    private String professorToken;
    private String labManagerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Clean up in correct order
        editProposalRepository.deleteAll();
        reservationWorkstationRepository.deleteAll();
        reservationRepository.deleteAll();
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
        professorUser = new User();
        professorUser.setEmail("professor@test.com");
        professorUser.setUsername("proftest");
        professorUser.setPassword(passwordEncoder.encode("password123"));
        professorUser.setFirstName("Test");
        professorUser.setLastName("Professor");
        professorUser.setEnabled(true);
        professorUser.setRole(professorRole);
        professorUser = userRepository.save(professorUser);

        // Create lab manager user
        labManagerUser = new User();
        labManagerUser.setEmail("manager@test.com");
        labManagerUser.setUsername("managertest");
        labManagerUser.setPassword(passwordEncoder.encode("password123"));
        labManagerUser.setFirstName("Lab");
        labManagerUser.setLastName("Manager");
        labManagerUser.setEnabled(true);
        labManagerUser.setRole(labManagerRole);
        labManagerUser = userRepository.save(labManagerUser);

        // Create admin user
        adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setUsername("admintest");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEnabled(true);
        adminUser.setRole(adminRole);
        adminUser = userRepository.save(adminUser);

        // Generate tokens
        professorToken = jwtService.generateAccessToken(professorUser);
        labManagerToken = jwtService.generateAccessToken(labManagerUser);
        adminToken = jwtService.generateAccessToken(adminUser);

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

        // Assign lab manager to lab
        LabManager labManager = new LabManager();
        labManager.setLab(testLab);
        labManager.setUser(labManagerUser);
        labManagerRepository.save(labManager);

        // Create workstation
        workstation1 = new Workstation();
        workstation1.setLab(testLab);
        workstation1.setIdentifier("WS-001");
        workstation1.setDescription("Workstation 1");
        workstation1.setActive(true);
        workstation1 = workstationRepository.save(workstation1);

        // Create pending reservation
        OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);

        pendingReservation = new Reservation();
        pendingReservation.setLab(testLab);
        pendingReservation.setUser(professorUser);
        pendingReservation.setStartTime(tomorrow);
        pendingReservation.setEndTime(tomorrow.plusHours(2));
        pendingReservation.setDescription("Test reservation");
        pendingReservation.setStatus(ReservationStatus.PENDING);
        pendingReservation.setWholeLab(true);
        pendingReservation = reservationRepository.save(pendingReservation);
    }

    @Nested
    @DisplayName("GET /api/v1/manager/reservations/pending Tests")
    class GetPendingReservationsTests {

        @Test
        @DisplayName("Should return pending reservations for lab manager")
        void shouldReturnPendingReservationsForLabManager() throws Exception {
            mockMvc.perform(get("/api/v1/manager/reservations/pending")
                            .header("Authorization", "Bearer " + labManagerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(pendingReservation.getId().toString()))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("Should return all pending reservations for admin")
        void shouldReturnAllPendingReservationsForAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/manager/reservations/pending")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/manager/reservations/pending"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/manager/reservations/{id} Tests")
    class GetReservationTests {

        @Test
        @DisplayName("Should return reservation details")
        void shouldReturnReservationDetails() throws Exception {
            mockMvc.perform(get("/api/v1/manager/reservations/{id}", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(pendingReservation.getId().toString()))
                    .andExpect(jsonPath("$.labName").value("Test Lab"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent reservation")
        void shouldReturn404ForNonExistentReservation() throws Exception {
            mockMvc.perform(get("/api/v1/manager/reservations/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + labManagerToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/manager/reservations/{id}/approve Tests")
    class ApproveReservationTests {

        @Test
        @DisplayName("Should approve reservation as lab manager")
        void shouldApproveReservationAsLabManager() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify reservation is approved
            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
        }

        @Test
        @DisplayName("Should approve reservation as admin")
        void shouldApproveReservationAsAdmin() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", pendingReservation.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
        }

        @Test
        @DisplayName("Should return 403 when professor tries to approve")
        void shouldReturn403WhenProfessorTriesToApprove() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", pendingReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 for non-existent reservation")
        void shouldReturn404ForNonExistentReservation() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", UUID.randomUUID())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when approving already approved reservation")
        void shouldReturn400WhenApprovingApprovedReservation() throws Exception {
            // First approve it
            pendingReservation.setStatus(ReservationStatus.APPROVED);
            reservationRepository.save(pendingReservation);

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/manager/reservations/{id}/decline Tests")
    class DeclineReservationTests {

        @Test
        @DisplayName("Should decline reservation with reason")
        void shouldDeclineReservationWithReason() throws Exception {
            DeclineReservationRequest request = DeclineReservationRequest.builder()
                    .reason("Lab not available for the requested time")
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/decline", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.REJECTED, updated.getStatus());
        }

        @Test
        @DisplayName("Should decline reservation without reason")
        void shouldDeclineReservationWithoutReason() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/{id}/decline", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.REJECTED, updated.getStatus());
        }

        @Test
        @DisplayName("Should return 403 when professor tries to decline")
        void shouldReturn403WhenProfessorTriesToDecline() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/{id}/decline", pendingReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/manager/reservations/{id}/edit Tests")
    class EditReservationByManagerTests {

        @Test
        @DisplayName("Should create edit proposal for reservation")
        void shouldCreateEditProposal() throws Exception {
            OffsetDateTime newStartTime = pendingReservation.getStartTime().plusHours(1);
            OffsetDateTime newEndTime = pendingReservation.getEndTime().plusHours(1);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newEndTime)
                    .description("Updated by manager")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify reservation status changed
            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated.getStatus());

            // Verify edit proposal was created
            var proposals = editProposalRepository.findByReservationId(pendingReservation.getId());
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals(ResolutionStatus.PENDING, proposals.get(0).getResolution());
        }

        @Test
        @DisplayName("Should return 400 when edit proposal already exists")
        void shouldReturn400WhenEditProposalExists() throws Exception {
            // Create an existing edit proposal
            ReservationEditProposal proposal = new ReservationEditProposal();
            proposal.setReservation(pendingReservation);
            proposal.setEditedBy(labManagerUser);
            proposal.setOriginalStatus(ReservationStatus.PENDING);
            proposal.setOriginalStartTime(pendingReservation.getStartTime());
            proposal.setOriginalEndTime(pendingReservation.getEndTime());
            proposal.setOriginalDescription(pendingReservation.getDescription());
            proposal.setOriginalWholeLab(true);
            proposal.setProposedStartTime(pendingReservation.getStartTime().plusHours(1));
            proposal.setProposedEndTime(pendingReservation.getEndTime().plusHours(1));
            proposal.setProposedDescription("Proposed");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            editProposalRepository.save(proposal);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(pendingReservation.getStartTime().plusHours(2))
                    .endTime(pendingReservation.getEndTime().plusHours(2))
                    .description("Another edit")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when professor tries to use manager edit endpoint")
        void shouldReturn403WhenProfessorUsesManagerEndpoint() throws Exception {
            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(pendingReservation.getStartTime().plusHours(1))
                    .endTime(pendingReservation.getEndTime().plusHours(1))
                    .description("Updated")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit", pendingReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/manager/reservations/{id}/edit/approve Tests")
    class ApproveEditByManagerTests {

        @Test
        @DisplayName("Should approve professor's edit")
        void shouldApproveProfessorsEdit() throws Exception {
            // Create edit proposal by professor
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(pendingReservation);

            ReservationEditProposal proposal = new ReservationEditProposal();
            proposal.setReservation(pendingReservation);
            proposal.setEditedBy(professorUser); // Professor made the edit
            proposal.setOriginalStatus(ReservationStatus.APPROVED);
            proposal.setOriginalStartTime(pendingReservation.getStartTime());
            proposal.setOriginalEndTime(pendingReservation.getEndTime());
            proposal.setOriginalDescription(pendingReservation.getDescription());
            proposal.setOriginalWholeLab(true);
            proposal.setProposedStartTime(pendingReservation.getStartTime().plusHours(1));
            proposal.setProposedEndTime(pendingReservation.getEndTime().plusHours(1));
            proposal.setProposedDescription("Professor's edit");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            editProposalRepository.save(proposal);

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit/approve", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify reservation was updated with proposed values
            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
            Assertions.assertEquals("Professor's edit", updated.getDescription());
        }

        @Test
        @DisplayName("Should return 400 when trying to approve own edit")
        void shouldReturn400WhenApprovingOwnEdit() throws Exception {
            // Create edit proposal by lab manager (not professor)
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(pendingReservation);

            ReservationEditProposal proposal = new ReservationEditProposal();
            proposal.setReservation(pendingReservation);
            proposal.setEditedBy(labManagerUser); // Lab manager made the edit
            proposal.setOriginalStatus(ReservationStatus.PENDING);
            proposal.setOriginalStartTime(pendingReservation.getStartTime());
            proposal.setOriginalEndTime(pendingReservation.getEndTime());
            proposal.setOriginalDescription(pendingReservation.getDescription());
            proposal.setOriginalWholeLab(true);
            proposal.setProposedStartTime(pendingReservation.getStartTime().plusHours(1));
            proposal.setProposedEndTime(pendingReservation.getEndTime().plusHours(1));
            proposal.setProposedDescription("Manager's edit");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            editProposalRepository.save(proposal);

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit/approve", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/manager/reservations/{id}/edit/reject Tests")
    class RejectEditByManagerTests {

        @Test
        @DisplayName("Should reject professor's edit with reason")
        void shouldRejectProfessorsEditWithReason() throws Exception {
            // Create edit proposal by professor
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(pendingReservation);

            ReservationEditProposal proposal = new ReservationEditProposal();
            proposal.setReservation(pendingReservation);
            proposal.setEditedBy(professorUser);
            proposal.setOriginalStatus(ReservationStatus.APPROVED);
            proposal.setOriginalStartTime(pendingReservation.getStartTime());
            proposal.setOriginalEndTime(pendingReservation.getEndTime());
            proposal.setOriginalDescription(pendingReservation.getDescription());
            proposal.setOriginalWholeLab(true);
            proposal.setOriginalWorkstationIds(null);
            proposal.setProposedStartTime(pendingReservation.getStartTime().plusHours(1));
            proposal.setProposedEndTime(pendingReservation.getEndTime().plusHours(1));
            proposal.setProposedDescription("Professor's edit");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            editProposalRepository.save(proposal);

            RejectEditRequest request = RejectEditRequest.builder()
                    .reason("Time conflict with other reservation")
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/{id}/edit/reject", pendingReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify reservation was restored to original values
            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus()); // Restored original status
        }
    }

    @Nested
    @DisplayName("Recurring Group Tests")
    class RecurringGroupTests {

        private UUID recurringGroupId;
        private Reservation recurring1;
        private Reservation recurring2;

        @BeforeEach
        void setUpRecurringGroup() {
            recurringGroupId = UUID.randomUUID();
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            recurring1 = new Reservation();
            recurring1.setLab(testLab);
            recurring1.setUser(professorUser);
            recurring1.setStartTime(tomorrow);
            recurring1.setEndTime(tomorrow.plusHours(2));
            recurring1.setDescription("Recurring 1");
            recurring1.setStatus(ReservationStatus.PENDING);
            recurring1.setWholeLab(true);
            recurring1.setRecurringGroupId(recurringGroupId);
            recurring1 = reservationRepository.save(recurring1);

            recurring2 = new Reservation();
            recurring2.setLab(testLab);
            recurring2.setUser(professorUser);
            recurring2.setStartTime(tomorrow.plusDays(7));
            recurring2.setEndTime(tomorrow.plusDays(7).plusHours(2));
            recurring2.setDescription("Recurring 2");
            recurring2.setStatus(ReservationStatus.PENDING);
            recurring2.setWholeLab(true);
            recurring2.setRecurringGroupId(recurringGroupId);
            recurring2 = reservationRepository.save(recurring2);
        }

        @Test
        @DisplayName("Should approve all reservations in recurring group")
        void shouldApproveRecurringGroup() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/approve", recurringGroupId)
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify both reservations are approved
            Reservation updated1 = reservationRepository.findById(recurring1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(recurring2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.APPROVED, updated2.getStatus());
        }

        @Test
        @DisplayName("Should decline all reservations in recurring group")
        void shouldDeclineRecurringGroup() throws Exception {
            DeclineReservationRequest request = DeclineReservationRequest.builder()
                    .reason("Lab will be under maintenance")
                    .build();

            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/decline", recurringGroupId)
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify both reservations are rejected
            Reservation updated1 = reservationRepository.findById(recurring1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(recurring2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.REJECTED, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.REJECTED, updated2.getStatus());
        }

        @Test
        @DisplayName("Should approve single occurrence in recurring group")
        void shouldApproveSingleOccurrence() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/occurrences/{id}/approve",
                            recurringGroupId, recurring1.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify only first reservation is approved
            Reservation updated1 = reservationRepository.findById(recurring1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(recurring2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.PENDING, updated2.getStatus()); // Still pending
        }

        @Test
        @DisplayName("Should decline single occurrence in recurring group")
        void shouldDeclineSingleOccurrence() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/occurrences/{id}/decline",
                            recurringGroupId, recurring1.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify only first reservation is rejected
            Reservation updated1 = reservationRepository.findById(recurring1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(recurring2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.REJECTED, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.PENDING, updated2.getStatus()); // Still pending
        }

        @Test
        @DisplayName("Should return 404 for non-existent recurring group")
        void shouldReturn404ForNonExistentRecurringGroup() throws Exception {
            mockMvc.perform(post("/api/v1/manager/reservations/recurring/{groupId}/approve", UUID.randomUUID())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Lab manager should not manage reservation from other lab")
        void labManagerShouldNotManageOtherLabReservation() throws Exception {
            // Create another lab without the lab manager assigned
            Lab otherLab = new Lab();
            otherLab.setName("Other Lab");
            otherLab.setBuilding(testBuilding);
            otherLab.setDefaultOpenTime(LocalTime.of(8, 0));
            otherLab.setDefaultCloseTime(LocalTime.of(20, 0));
            otherLab = labRepository.save(otherLab);

            // Create reservation in other lab
            Reservation otherLabReservation = new Reservation();
            otherLabReservation.setLab(otherLab);
            otherLabReservation.setUser(professorUser);
            otherLabReservation.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withHour(10));
            otherLabReservation.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withHour(12));
            otherLabReservation.setDescription("Other lab reservation");
            otherLabReservation.setStatus(ReservationStatus.PENDING);
            otherLabReservation.setWholeLab(true);
            otherLabReservation = reservationRepository.save(otherLabReservation);

            // Lab manager should not be able to approve this
            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", otherLabReservation.getId())
                            .header("Authorization", "Bearer " + labManagerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin should manage reservation from any lab")
        void adminShouldManageAnyLabReservation() throws Exception {
            // Create another lab
            Lab otherLab = new Lab();
            otherLab.setName("Other Lab");
            otherLab.setBuilding(testBuilding);
            otherLab.setDefaultOpenTime(LocalTime.of(8, 0));
            otherLab.setDefaultCloseTime(LocalTime.of(20, 0));
            otherLab = labRepository.save(otherLab);

            // Create reservation in other lab
            Reservation otherLabReservation = new Reservation();
            otherLabReservation.setLab(otherLab);
            otherLabReservation.setUser(professorUser);
            otherLabReservation.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withHour(10));
            otherLabReservation.setEndTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withHour(12));
            otherLabReservation.setDescription("Other lab reservation");
            otherLabReservation.setStatus(ReservationStatus.PENDING);
            otherLabReservation.setWholeLab(true);
            otherLabReservation = reservationRepository.save(otherLabReservation);

            // Admin should be able to approve this
            mockMvc.perform(post("/api/v1/manager/reservations/{id}/approve", otherLabReservation.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            Reservation updated = reservationRepository.findById(otherLabReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
        }
    }
}
