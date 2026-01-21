package com._glab.booking_system.booking;

import com._glab.booking_system.auth.config.TestJwtConfig;
import com._glab.booking_system.auth.config.TestMailConfig;
import com._glab.booking_system.auth.service.JwtService;
import com._glab.booking_system.booking.model.*;
import com._glab.booking_system.booking.repository.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@Import({TestJwtConfig.class, TestMailConfig.class})
class ProfessorReservationEditIntegrationTest {

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
    private User otherProfessorUser;
    private User labManagerUser;
    private Building testBuilding;
    private Lab testLab;
    private Reservation pendingReservation;
    private Reservation approvedReservation;
    private String professorToken;
    private String otherProfessorToken;

    @BeforeEach
    void setUp() {
        // Clean up
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
        Role professorRole = new Role();
        professorRole.setName(RoleName.PROFESSOR);
        professorRole = roleRepository.save(professorRole);

        Role labManagerRole = new Role();
        labManagerRole.setName(RoleName.LAB_MANAGER);
        labManagerRole = roleRepository.save(labManagerRole);

        // Create users
        professorUser = new User();
        professorUser.setEmail("professor@test.com");
        professorUser.setUsername("proftest");
        professorUser.setPassword(passwordEncoder.encode("password123"));
        professorUser.setFirstName("Test");
        professorUser.setLastName("Professor");
        professorUser.setEnabled(true);
        professorUser.setRole(professorRole);
        professorUser = userRepository.save(professorUser);

        otherProfessorUser = new User();
        otherProfessorUser.setEmail("other.professor@test.com");
        otherProfessorUser.setUsername("otherproftest");
        otherProfessorUser.setPassword(passwordEncoder.encode("password123"));
        otherProfessorUser.setFirstName("Other");
        otherProfessorUser.setLastName("Professor");
        otherProfessorUser.setEnabled(true);
        otherProfessorUser.setRole(professorRole);
        otherProfessorUser = userRepository.save(otherProfessorUser);

        labManagerUser = new User();
        labManagerUser.setEmail("manager@test.com");
        labManagerUser.setUsername("managertest");
        labManagerUser.setPassword(passwordEncoder.encode("password123"));
        labManagerUser.setFirstName("Lab");
        labManagerUser.setLastName("Manager");
        labManagerUser.setEnabled(true);
        labManagerUser.setRole(labManagerRole);
        labManagerUser = userRepository.save(labManagerUser);

        // Generate tokens
        professorToken = jwtService.generateAccessToken(professorUser);
        otherProfessorToken = jwtService.generateAccessToken(otherProfessorUser);

        // Create building and lab
        testBuilding = new Building();
        testBuilding.setName("Test Building");
        testBuilding.setAddress("123 Test St");
        testBuilding = buildingRepository.save(testBuilding);

        testLab = new Lab();
        testLab.setName("Test Lab");
        testLab.setBuilding(testBuilding);
        testLab.setDefaultOpenTime(LocalTime.of(8, 0));
        testLab.setDefaultCloseTime(LocalTime.of(20, 0));
        testLab = labRepository.save(testLab);

        // Assign lab manager
        LabManager labManager = new LabManager();
        labManager.setLab(testLab);
        labManager.setUser(labManagerUser);
        labManagerRepository.save(labManager);

        // Create reservations
        OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);

        pendingReservation = new Reservation();
        pendingReservation.setLab(testLab);
        pendingReservation.setUser(professorUser);
        pendingReservation.setStartTime(tomorrow);
        pendingReservation.setEndTime(tomorrow.plusHours(2));
        pendingReservation.setDescription("Pending reservation");
        pendingReservation.setStatus(ReservationStatus.PENDING);
        pendingReservation.setWholeLab(true);
        pendingReservation = reservationRepository.save(pendingReservation);

        approvedReservation = new Reservation();
        approvedReservation.setLab(testLab);
        approvedReservation.setUser(professorUser);
        approvedReservation.setStartTime(tomorrow.plusDays(1));
        approvedReservation.setEndTime(tomorrow.plusDays(1).plusHours(2));
        approvedReservation.setDescription("Approved reservation");
        approvedReservation.setStatus(ReservationStatus.APPROVED);
        approvedReservation.setWholeLab(true);
        approvedReservation = reservationRepository.save(approvedReservation);
    }

    @Nested
    @DisplayName("POST /api/v1/reservations/{id}/edit Tests")
    class ProfessorEditReservationTests {

        @Test
        @DisplayName("Should apply edit directly for PENDING reservation")
        void shouldApplyEditDirectlyForPendingReservation() throws Exception {
            OffsetDateTime newStartTime = pendingReservation.getStartTime().plusHours(1);
            OffsetDateTime newEndTime = pendingReservation.getEndTime().plusHours(1);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newEndTime)
                    .description("Updated by professor")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit", pendingReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify changes were applied directly
            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals("Updated by professor", updated.getDescription());
            // Status should remain PENDING (no approval needed)
            Assertions.assertEquals(ReservationStatus.PENDING, updated.getStatus());

            // No edit proposal should be created
            var proposals = editProposalRepository.findByReservationId(pendingReservation.getId());
            Assertions.assertTrue(proposals.isEmpty());
        }

        @Test
        @DisplayName("Should create edit proposal for APPROVED reservation")
        void shouldCreateEditProposalForApprovedReservation() throws Exception {
            OffsetDateTime newStartTime = approvedReservation.getStartTime().plusHours(1);
            OffsetDateTime newEndTime = approvedReservation.getEndTime().plusHours(1);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(newStartTime)
                    .endTime(newEndTime)
                    .description("Professor wants to change time")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit", approvedReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify status changed to PENDING_EDIT_APPROVAL
            Reservation updated = reservationRepository.findById(approvedReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated.getStatus());

            // Verify edit proposal was created
            var proposals = editProposalRepository.findByReservationId(approvedReservation.getId());
            Assertions.assertEquals(1, proposals.size());
            Assertions.assertEquals(professorUser.getId(), proposals.get(0).getEditedBy().getId());
            Assertions.assertEquals(ResolutionStatus.PENDING, proposals.get(0).getResolution());
        }

        @Test
        @DisplayName("Should return 403 when editing other professor's reservation")
        void shouldReturn403WhenEditingOtherProfessorsReservation() throws Exception {
            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(pendingReservation.getStartTime().plusHours(1))
                    .endTime(pendingReservation.getEndTime().plusHours(1))
                    .description("Trying to edit")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit", pendingReservation.getId())
                            .header("Authorization", "Bearer " + otherProfessorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 for invalid edit request (end before start)")
        void shouldReturn400ForInvalidEditRequest() throws Exception {
            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(pendingReservation.getEndTime()) // Start after original end
                    .endTime(pendingReservation.getStartTime()) // End before start
                    .description("Invalid times")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit", pendingReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for REJECTED reservation")
        void shouldReturn400ForRejectedReservation() throws Exception {
            pendingReservation.setStatus(ReservationStatus.REJECTED);
            reservationRepository.save(pendingReservation);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(pendingReservation.getStartTime().plusHours(1))
                    .endTime(pendingReservation.getEndTime().plusHours(1))
                    .description("Trying to edit rejected")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit", pendingReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/reservations/{id}/edit/approve Tests")
    class ProfessorApproveEditTests {

        @Test
        @DisplayName("Should approve lab manager's edit")
        void shouldApproveLabManagersEdit() throws Exception {
            // Set up: Lab manager created an edit proposal
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
            proposal.setProposedDescription("Lab manager's suggestion");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            editProposalRepository.save(proposal);

            mockMvc.perform(post("/api/v1/reservations/{id}/edit/approve", pendingReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify reservation was updated with proposed values
            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals("Lab manager's suggestion", updated.getDescription());
            Assertions.assertEquals(ReservationStatus.APPROVED, updated.getStatus());
        }

        @Test
        @DisplayName("Should return 400 when trying to approve own edit")
        void shouldReturn400WhenApprovingOwnEdit() throws Exception {
            // Professor created the edit proposal
            approvedReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(approvedReservation);

            ReservationEditProposal proposal = new ReservationEditProposal();
            proposal.setReservation(approvedReservation);
            proposal.setEditedBy(professorUser); // Professor made the edit
            proposal.setOriginalStatus(ReservationStatus.APPROVED);
            proposal.setOriginalStartTime(approvedReservation.getStartTime());
            proposal.setOriginalEndTime(approvedReservation.getEndTime());
            proposal.setOriginalDescription(approvedReservation.getDescription());
            proposal.setOriginalWholeLab(true);
            proposal.setProposedStartTime(approvedReservation.getStartTime().plusHours(1));
            proposal.setProposedEndTime(approvedReservation.getEndTime().plusHours(1));
            proposal.setProposedDescription("My own edit");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            editProposalRepository.save(proposal);

            mockMvc.perform(post("/api/v1/reservations/{id}/edit/approve", approvedReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when other professor tries to approve")
        void shouldReturn403WhenOtherProfessorApprovesEdit() throws Exception {
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(pendingReservation);

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
            proposal.setProposedDescription("Manager's edit");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            editProposalRepository.save(proposal);

            mockMvc.perform(post("/api/v1/reservations/{id}/edit/approve", pendingReservation.getId())
                            .header("Authorization", "Bearer " + otherProfessorToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/reservations/{id}/edit/reject Tests")
    class ProfessorRejectEditTests {

        @Test
        @DisplayName("Should reject lab manager's edit with reason")
        void shouldRejectLabManagersEditWithReason() throws Exception {
            // Set up: Lab manager created an edit proposal
            pendingReservation.setStatus(ReservationStatus.PENDING_EDIT_APPROVAL);
            reservationRepository.save(pendingReservation);

            ReservationEditProposal proposal = new ReservationEditProposal();
            proposal.setReservation(pendingReservation);
            proposal.setEditedBy(labManagerUser);
            proposal.setOriginalStatus(ReservationStatus.PENDING);
            proposal.setOriginalStartTime(pendingReservation.getStartTime());
            proposal.setOriginalEndTime(pendingReservation.getEndTime());
            proposal.setOriginalDescription(pendingReservation.getDescription());
            proposal.setOriginalWholeLab(true);
            proposal.setOriginalWorkstationIds(null);
            proposal.setProposedStartTime(pendingReservation.getStartTime().plusHours(1));
            proposal.setProposedEndTime(pendingReservation.getEndTime().plusHours(1));
            proposal.setProposedDescription("Lab manager's suggestion");
            proposal.setProposedWholeLab(true);
            proposal.setResolution(ResolutionStatus.PENDING);
            editProposalRepository.save(proposal);

            RejectEditRequest request = RejectEditRequest.builder()
                    .reason("The suggested time doesn't work for me")
                    .build();

            mockMvc.perform(post("/api/v1/reservations/{id}/edit/reject", pendingReservation.getId())
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify reservation was restored
            Reservation updated = reservationRepository.findById(pendingReservation.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING, updated.getStatus()); // Restored to PENDING
            Assertions.assertEquals("Pending reservation", updated.getDescription()); // Original description
        }
    }

    @Nested
    @DisplayName("Recurring Group Professor Edit Tests")
    class RecurringGroupProfessorEditTests {

        private UUID recurringGroupId;
        private Reservation pendingRecurring1;
        private Reservation pendingRecurring2;

        @BeforeEach
        void setUpRecurringGroup() {
            recurringGroupId = UUID.randomUUID();
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            pendingRecurring1 = new Reservation();
            pendingRecurring1.setLab(testLab);
            pendingRecurring1.setUser(professorUser);
            pendingRecurring1.setStartTime(tomorrow);
            pendingRecurring1.setEndTime(tomorrow.plusHours(2));
            pendingRecurring1.setDescription("Recurring 1");
            pendingRecurring1.setStatus(ReservationStatus.PENDING);
            pendingRecurring1.setWholeLab(true);
            pendingRecurring1.setRecurringGroupId(recurringGroupId);
            pendingRecurring1 = reservationRepository.save(pendingRecurring1);

            pendingRecurring2 = new Reservation();
            pendingRecurring2.setLab(testLab);
            pendingRecurring2.setUser(professorUser);
            pendingRecurring2.setStartTime(tomorrow.plusDays(7));
            pendingRecurring2.setEndTime(tomorrow.plusDays(7).plusHours(2));
            pendingRecurring2.setDescription("Recurring 2");
            pendingRecurring2.setStatus(ReservationStatus.PENDING);
            pendingRecurring2.setWholeLab(true);
            pendingRecurring2.setRecurringGroupId(recurringGroupId);
            pendingRecurring2 = reservationRepository.save(pendingRecurring2);
        }

        @Test
        @DisplayName("Should apply edit directly for all PENDING reservations in group")
        void shouldApplyEditDirectlyForAllPendingInGroup() throws Exception {
            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(pendingRecurring1.getStartTime().plusHours(1))
                    .endTime(pendingRecurring1.getEndTime().plusHours(1))
                    .description("Updated all recurring")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/recurring/{groupId}/edit", recurringGroupId)
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // Verify both were updated directly
            Reservation updated1 = reservationRepository.findById(pendingRecurring1.getId()).orElseThrow();
            Reservation updated2 = reservationRepository.findById(pendingRecurring2.getId()).orElseThrow();
            
            Assertions.assertEquals("Updated all recurring", updated1.getDescription());
            Assertions.assertEquals("Updated all recurring", updated2.getDescription());
            Assertions.assertEquals(ReservationStatus.PENDING, updated1.getStatus());
            Assertions.assertEquals(ReservationStatus.PENDING, updated2.getStatus());
        }

        @Test
        @DisplayName("Should create edit proposals for APPROVED reservations in mixed group")
        void shouldCreateEditProposalsForApprovedInMixedGroup() throws Exception {
            // Make one reservation APPROVED
            pendingRecurring2.setStatus(ReservationStatus.APPROVED);
            reservationRepository.save(pendingRecurring2);

            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(pendingRecurring1.getStartTime().plusHours(1))
                    .endTime(pendingRecurring1.getEndTime().plusHours(1))
                    .description("Updated")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/recurring/{groupId}/edit", recurringGroupId)
                            .header("Authorization", "Bearer " + professorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // PENDING one should be updated directly
            Reservation updated1 = reservationRepository.findById(pendingRecurring1.getId()).orElseThrow();
            Assertions.assertEquals("Updated", updated1.getDescription());
            Assertions.assertEquals(ReservationStatus.PENDING, updated1.getStatus());

            // APPROVED one should have edit proposal
            Reservation updated2 = reservationRepository.findById(pendingRecurring2.getId()).orElseThrow();
            Assertions.assertEquals(ReservationStatus.PENDING_EDIT_APPROVAL, updated2.getStatus());
            
            var proposals = editProposalRepository.findByReservationId(pendingRecurring2.getId());
            Assertions.assertEquals(1, proposals.size());
        }

        @Test
        @DisplayName("Should return 403 when other professor edits recurring group")
        void shouldReturn403WhenOtherProfessorEditsGroup() throws Exception {
            EditReservationRequest request = EditReservationRequest.builder()
                    .startTime(pendingRecurring1.getStartTime().plusHours(1))
                    .endTime(pendingRecurring1.getEndTime().plusHours(1))
                    .description("Trying to edit")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations/recurring/{groupId}/edit", recurringGroupId)
                            .header("Authorization", "Bearer " + otherProfessorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
