package com._glab.booking_system.booking;

import com._glab.booking_system.auth.config.TestJwtConfig;
import com._glab.booking_system.auth.config.TestMailConfig;
import com._glab.booking_system.auth.service.JwtService;
import com._glab.booking_system.booking.model.*;
import com._glab.booking_system.booking.repository.*;
import com._glab.booking_system.booking.request.CreateReservationRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@Import({TestJwtConfig.class, TestMailConfig.class})
class BookingIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private Role professorRole;
    private Building testBuilding;
    private Lab testLab;
    private Workstation workstation1;
    private Workstation workstation2;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // Clean up in correct order (handle foreign key constraints)
        reservationWorkstationRepository.deleteAll();
        reservationRepository.deleteAll();
        workstationRepository.deleteAll();
        labRepository.deleteAll();
        buildingRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create role
        professorRole = new Role();
        professorRole.setName(RoleName.PROFESSOR);
        professorRole = roleRepository.save(professorRole);

        // Create test user
        testUser = new User();
        testUser.setEmail("professor@test.com");
        testUser.setUsername("proftest");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Test");
        testUser.setLastName("Professor");
        testUser.setEnabled(true);
        testUser.setRole(professorRole);
        testUser = userRepository.save(testUser);

        // Generate access token
        accessToken = jwtService.generateAccessToken(testUser);

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
    }

    @Nested
    @DisplayName("Building Discovery Endpoint Tests")
    class BuildingDiscoveryTests {

        @Test
        @DisplayName("Should list all buildings (public endpoint)")
        void shouldListAllBuildings() throws Exception {
            mockMvc.perform(get("/api/v1/buildings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Test Building"))
                    .andExpect(jsonPath("$[0].address").value("123 Test St"));
        }

        @Test
        @DisplayName("Should list labs for a building (public endpoint)")
        void shouldListLabsForBuilding() throws Exception {
            mockMvc.perform(get("/api/v1/buildings/{buildingId}/labs", testBuilding.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Test Lab"));
        }
    }

    @Nested
    @DisplayName("Lab Discovery Endpoint Tests")
    class LabDiscoveryTests {

        @Test
        @DisplayName("Should get lab by ID (public endpoint)")
        void shouldGetLabById() throws Exception {
            mockMvc.perform(get("/api/v1/labs/{labId}", testLab.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Test Lab"))
                    .andExpect(jsonPath("$.defaultOpenTime").value("08:00:00"))
                    .andExpect(jsonPath("$.defaultCloseTime").value("20:00:00"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent lab")
        void shouldReturn404ForNonExistentLab() throws Exception {
            mockMvc.perform(get("/api/v1/labs/{labId}", 9999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("BOOKING_LAB_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should get lab workstations (public endpoint)")
        void shouldGetLabWorkstations() throws Exception {
            mockMvc.perform(get("/api/v1/labs/{labId}/workstations", testLab.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.labId").value(testLab.getId()))
                    .andExpect(jsonPath("$.labName").value("Test Lab"))
                    .andExpect(jsonPath("$.workstations").isArray())
                    .andExpect(jsonPath("$.workstations", hasSize(2)))
                    .andExpect(jsonPath("$.workstations[0].identifier").value("WS-001"))
                    .andExpect(jsonPath("$.workstations[0].active").value(true));
        }

        @Test
        @DisplayName("Should get weekly availability (public endpoint)")
        void shouldGetWeeklyAvailability() throws Exception {
            mockMvc.perform(get("/api/v1/labs/{labId}/availability", testLab.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.labId").value(testLab.getId()))
                    .andExpect(jsonPath("$.labName").value("Test Lab"))
                    .andExpect(jsonPath("$.weekStart").isNotEmpty())
                    .andExpect(jsonPath("$.weekEnd").isNotEmpty())
                    .andExpect(jsonPath("$.operatingHours").isArray())
                    .andExpect(jsonPath("$.closedDays").isArray())
                    .andExpect(jsonPath("$.reservations").isArray());
        }

        @Test
        @DisplayName("Should get current availability (public endpoint)")
        void shouldGetCurrentAvailability() throws Exception {
            mockMvc.perform(get("/api/v1/labs/{labId}/availability/current", testLab.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.labId").value(testLab.getId()))
                    .andExpect(jsonPath("$.labName").value("Test Lab"))
                    .andExpect(jsonPath("$.isOpen").isBoolean())
                    .andExpect(jsonPath("$.currentReservations").isArray());
        }
    }

    @Nested
    @DisplayName("Reservation Endpoint Tests")
    class ReservationTests {

        @Test
        @DisplayName("Should create reservation with workstations")
        void shouldCreateReservationWithWorkstations() throws Exception {
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(testLab.getId())
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .description("Test reservation")
                    .wholeLab(false)
                    .workstationIds(List.of(workstation1.getId()))
                    .build();

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.labId").value(testLab.getId()))
                    .andExpect(jsonPath("$.labName").value("Test Lab"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.wholeLab").value(false))
                    .andExpect(jsonPath("$.workstationIds").isArray())
                    .andExpect(jsonPath("$.workstationIds", hasSize(1)));
        }

        @Test
        @DisplayName("Should create whole lab reservation")
        void shouldCreateWholeLabReservation() throws Exception {
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(testLab.getId())
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(3))
                    .description("Whole lab reservation")
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.wholeLab").value(true))
                    .andExpect(jsonPath("$.workstationIds").isEmpty());
        }

        @Test
        @DisplayName("Should return 401 or 403 when creating reservation without auth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(testLab.getId())
                    .startTime(OffsetDateTime.now().plusDays(1))
                    .endTime(OffsetDateTime.now().plusDays(1).plusHours(2))
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError()); // Accept 401 or 403
        }

        @Test
        @DisplayName("Should return 400 for invalid time range")
        void shouldReturn400ForInvalidTimeRange() throws Exception {
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(14).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(testLab.getId())
                    .startTime(tomorrow)
                    .endTime(tomorrow.minusHours(2)) // End before start
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("BOOKING_INVALID_TIME_RANGE"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent lab in reservation")
        void shouldReturn404ForNonExistentLabInReservation() throws Exception {
            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(9999)
                    .startTime(OffsetDateTime.now().plusDays(1))
                    .endTime(OffsetDateTime.now().plusDays(1).plusHours(2))
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("BOOKING_LAB_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 for time outside operating hours")
        void shouldReturn400ForOutsideOperatingHours() throws Exception {
            // Lab opens at 8:00, try to book at 6:00
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(6).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(testLab.getId())
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(1))
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("BOOKING_OUTSIDE_OPERATING_HOURS"));
        }

        @Test
        @DisplayName("Should get user's reservations")
        void shouldGetUserReservations() throws Exception {
            // First create a reservation
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(testLab.getId())
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .wholeLab(true)
                    .build();

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Then get user's reservations
            mockMvc.perform(get("/api/v1/reservations/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].labName").value("Test Lab"));
        }

        @Test
        @DisplayName("Should create recurring reservation")
        void shouldCreateRecurringReservation() throws Exception {
            OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);

            CreateReservationRequest.RecurringConfig recurring = CreateReservationRequest.RecurringConfig.builder()
                    .patternType("WEEKLY")
                    .occurrences(3)
                    .build();

            CreateReservationRequest request = CreateReservationRequest.builder()
                    .labId(testLab.getId())
                    .startTime(tomorrow)
                    .endTime(tomorrow.plusHours(2))
                    .description("Weekly meeting")
                    .wholeLab(true)
                    .recurring(recurring)
                    .build();

            mockMvc.perform(post("/api/v1/reservations")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recurringGroupId").isNotEmpty())
                    .andExpect(jsonPath("$.patternType").value("WEEKLY"))
                    .andExpect(jsonPath("$.totalOccurrences").value(3))
                    .andExpect(jsonPath("$.reservations").isArray())
                    .andExpect(jsonPath("$.reservations", hasSize(3)));
        }
    }
}
