package com._glab.booking_system.booking.service;

import com._glab.booking_system.booking.exception.BuildingNotFoundException;
import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.model.BuildingClosedDay;
import com._glab.booking_system.booking.model.BuildingOperatingHours;
import com._glab.booking_system.booking.repository.BuildingClosedDayRepository;
import com._glab.booking_system.booking.repository.BuildingOperatingHoursRepository;
import com._glab.booking_system.booking.repository.BuildingRepository;
import com._glab.booking_system.booking.request.CreateBuildingRequest;
import com._glab.booking_system.booking.request.DaysOffRequest;
import com._glab.booking_system.booking.request.OperatingHoursRequest;
import com._glab.booking_system.booking.request.UpdateBuildingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private BuildingOperatingHoursRepository buildingOperatingHoursRepository;

    @Mock
    private BuildingClosedDayRepository buildingClosedDayRepository;

    private BuildingService buildingService;

    private Building testBuilding;

    @BeforeEach
    void setUp() {
        buildingService = new BuildingService(
                buildingRepository,
                buildingOperatingHoursRepository,
                buildingClosedDayRepository
        );

        testBuilding = new Building();
        testBuilding.setId(1);
        testBuilding.setName("Test Building");
        testBuilding.setDescription("A test building");
        testBuilding.setAddress("123 Test St");
        testBuilding.setCity("Test City");
        testBuilding.setActive(true);
    }

    @Nested
    @DisplayName("Get Buildings Tests")
    class GetBuildingsTests {

        @Test
        @DisplayName("Should return all active buildings")
        void shouldReturnAllActiveBuildings() {
            when(buildingRepository.findAllByActiveTrue()).thenReturn(List.of(testBuilding));

            List<Building> result = buildingService.getBuildings();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Test Building");
        }

        @Test
        @DisplayName("Should return building by ID")
        void shouldReturnBuildingById() {
            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));

            Building result = buildingService.getBuildingById(1);

            assertThat(result.getName()).isEqualTo("Test Building");
        }

        @Test
        @DisplayName("Should throw BuildingNotFoundException when building not found")
        void shouldThrowBuildingNotFoundException() {
            when(buildingRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> buildingService.getBuildingById(999))
                    .isInstanceOf(BuildingNotFoundException.class);
        }

        @Test
        @DisplayName("Should return all buildings for admin including inactive")
        void shouldReturnAllBuildingsForAdmin() {
            Building inactiveBuilding = new Building();
            inactiveBuilding.setId(2);
            inactiveBuilding.setActive(false);

            when(buildingRepository.findAll()).thenReturn(List.of(testBuilding, inactiveBuilding));

            List<Building> result = buildingService.getAllBuildingsForAdmin();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Create Building Tests")
    class CreateBuildingTests {

        @Test
        @DisplayName("Should create building successfully")
        void shouldCreateBuildingSuccessfully() {
            CreateBuildingRequest request = CreateBuildingRequest.builder()
                    .name("New Building")
                    .description("A new building")
                    .address("456 New St")
                    .city("New City")
                    .build();

            when(buildingRepository.save(any(Building.class))).thenAnswer(i -> {
                Building b = i.getArgument(0);
                b.setId(2);
                return b;
            });

            Building result = buildingService.createBuilding(request);

            assertThat(result.getName()).isEqualTo("New Building");
            assertThat(result.getActive()).isTrue();

            ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);
            verify(buildingRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Update Building Tests")
    class UpdateBuildingTests {

        @Test
        @DisplayName("Should update building successfully")
        void shouldUpdateBuildingSuccessfully() {
            UpdateBuildingRequest request = UpdateBuildingRequest.builder()
                    .name("Updated Building")
                    .description("Updated description")
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingRepository.save(any(Building.class))).thenAnswer(i -> i.getArgument(0));

            Building result = buildingService.updateBuilding(1, request);

            assertThat(result.getName()).isEqualTo("Updated Building");
            assertThat(result.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should throw exception when updating archived building")
        void shouldThrowExceptionWhenUpdatingArchivedBuilding() {
            testBuilding.setActive(false);
            UpdateBuildingRequest request = UpdateBuildingRequest.builder()
                    .name("Updated")
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));

            assertThatThrownBy(() -> buildingService.updateBuilding(1, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("archived");
        }
    }

    @Nested
    @DisplayName("Archive Building Tests")
    class ArchiveBuildingTests {

        @Test
        @DisplayName("Should archive building successfully")
        void shouldArchiveBuildingSuccessfully() {
            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingRepository.save(any(Building.class))).thenAnswer(i -> i.getArgument(0));

            Building result = buildingService.archiveBuilding(1);

            assertThat(result.getActive()).isFalse();
            assertThat(result.getArchivedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Hard Delete Building Tests")
    class HardDeleteBuildingTests {

        @Test
        @DisplayName("Should hard delete building successfully")
        void shouldHardDeleteBuildingSuccessfully() {
            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));

            buildingService.hardDeleteBuilding(1);

            verify(buildingRepository).delete(testBuilding);
        }

        @Test
        @DisplayName("Should throw BuildingNotFoundException when building not found")
        void shouldThrowBuildingNotFoundException() {
            when(buildingRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> buildingService.hardDeleteBuilding(999))
                    .isInstanceOf(BuildingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Building Operating Hours Tests")
    class BuildingOperatingHoursTests {

        @Test
        @DisplayName("Should get building operating hours")
        void shouldGetBuildingOperatingHours() {
            BuildingOperatingHours hours = new BuildingOperatingHours();
            hours.setId(1);
            hours.setBuilding(testBuilding);
            hours.setDayOfWeek(1);
            hours.setOpenTime(LocalTime.of(8, 0));
            hours.setCloseTime(LocalTime.of(18, 0));

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingOperatingHoursRepository.findByBuildingId(1)).thenReturn(List.of(hours));

            List<BuildingOperatingHours> result = buildingService.getBuildingOperatingHours(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDayOfWeek()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should set building operating hours - create new")
        void shouldSetBuildingOperatingHoursNew() {
            OperatingHoursRequest request = OperatingHoursRequest.builder()
                    .dayOfWeek(1)
                    .openTime(LocalTime.of(9, 0))
                    .closeTime(LocalTime.of(17, 0))
                    .isClosed(false)
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingOperatingHoursRepository.findByBuildingIdAndDayOfWeek(1, 1))
                    .thenReturn(Optional.empty());
            when(buildingOperatingHoursRepository.save(any(BuildingOperatingHours.class)))
                    .thenAnswer(i -> i.getArgument(0));

            BuildingOperatingHours result = buildingService.setBuildingOperatingHours(1, request);

            assertThat(result.getDayOfWeek()).isEqualTo(1);
            assertThat(result.getOpenTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(result.getCloseTime()).isEqualTo(LocalTime.of(17, 0));
        }

        @Test
        @DisplayName("Should set building operating hours - update existing")
        void shouldSetBuildingOperatingHoursUpdate() {
            BuildingOperatingHours existingHours = new BuildingOperatingHours();
            existingHours.setId(1);
            existingHours.setBuilding(testBuilding);
            existingHours.setDayOfWeek(1);
            existingHours.setOpenTime(LocalTime.of(8, 0));
            existingHours.setCloseTime(LocalTime.of(18, 0));

            OperatingHoursRequest request = OperatingHoursRequest.builder()
                    .dayOfWeek(1)
                    .openTime(LocalTime.of(10, 0))
                    .closeTime(LocalTime.of(20, 0))
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingOperatingHoursRepository.findByBuildingIdAndDayOfWeek(1, 1))
                    .thenReturn(Optional.of(existingHours));
            when(buildingOperatingHoursRepository.save(any(BuildingOperatingHours.class)))
                    .thenAnswer(i -> i.getArgument(0));

            BuildingOperatingHours result = buildingService.setBuildingOperatingHours(1, request);

            assertThat(result.getOpenTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(result.getCloseTime()).isEqualTo(LocalTime.of(20, 0));
        }

        @Test
        @DisplayName("Should delete building operating hours")
        void shouldDeleteBuildingOperatingHours() {
            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));

            buildingService.deleteBuildingOperatingHours(1, 1);

            verify(buildingOperatingHoursRepository).deleteByBuildingIdAndDayOfWeek(1, 1);
        }
    }

    @Nested
    @DisplayName("Building Days Off Tests")
    class BuildingDaysOffTests {

        @Test
        @DisplayName("Should get building days off")
        void shouldGetBuildingDaysOff() {
            BuildingClosedDay day = new BuildingClosedDay();
            day.setId(1);
            day.setBuilding(testBuilding);
            day.setSpecificDate(LocalDate.of(2026, 12, 25));
            day.setReason("Christmas");

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingClosedDayRepository.findByBuildingId(1)).thenReturn(List.of(day));

            List<BuildingClosedDay> result = buildingService.getBuildingDaysOff(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReason()).isEqualTo("Christmas");
        }

        @Test
        @DisplayName("Should add building day off with specific date")
        void shouldAddBuildingDayOffWithSpecificDate() {
            DaysOffRequest request = DaysOffRequest.builder()
                    .specificDate(LocalDate.of(2026, 12, 25))
                    .reason("Christmas")
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingClosedDayRepository.save(any(BuildingClosedDay.class)))
                    .thenAnswer(i -> i.getArgument(0));

            BuildingClosedDay result = buildingService.addBuildingDayOff(1, request);

            assertThat(result.getSpecificDate()).isEqualTo(LocalDate.of(2026, 12, 25));
            assertThat(result.getReason()).isEqualTo("Christmas");
        }

        @Test
        @DisplayName("Should add building day off with recurring day of week")
        void shouldAddBuildingDayOffWithRecurringDay() {
            DaysOffRequest request = DaysOffRequest.builder()
                    .recurringDayOfWeek(7) // Sunday
                    .reason("Closed on Sundays")
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingClosedDayRepository.save(any(BuildingClosedDay.class)))
                    .thenAnswer(i -> i.getArgument(0));

            BuildingClosedDay result = buildingService.addBuildingDayOff(1, request);

            assertThat(result.getRecurringDayOfWeek()).isEqualTo(7);
        }

        @Test
        @DisplayName("Should throw exception when neither date nor day of week is set")
        void shouldThrowExceptionWhenNeitherDateNorDaySet() {
            DaysOffRequest request = DaysOffRequest.builder()
                    .reason("Invalid request")
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));

            assertThatThrownBy(() -> buildingService.addBuildingDayOff(1, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("specificDate or recurringDayOfWeek");
        }

        @Test
        @DisplayName("Should update building day off")
        void shouldUpdateBuildingDayOff() {
            BuildingClosedDay existingDay = new BuildingClosedDay();
            existingDay.setId(1);
            existingDay.setBuilding(testBuilding);
            existingDay.setSpecificDate(LocalDate.of(2026, 12, 25));
            existingDay.setReason("Christmas");

            DaysOffRequest request = DaysOffRequest.builder()
                    .reason("Christmas Holiday")
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingClosedDayRepository.findById(1)).thenReturn(Optional.of(existingDay));
            when(buildingClosedDayRepository.save(any(BuildingClosedDay.class)))
                    .thenAnswer(i -> i.getArgument(0));

            BuildingClosedDay result = buildingService.updateBuildingDayOff(1, 1, request);

            assertThat(result.getReason()).isEqualTo("Christmas Holiday");
        }

        @Test
        @DisplayName("Should throw exception when day off not found")
        void shouldThrowExceptionWhenDayOffNotFound() {
            DaysOffRequest request = DaysOffRequest.builder()
                    .reason("Updated")
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingClosedDayRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> buildingService.updateBuildingDayOff(1, 999, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw exception when day off belongs to different building")
        void shouldThrowExceptionWhenDayOffBelongsToDifferentBuilding() {
            Building otherBuilding = new Building();
            otherBuilding.setId(2);

            BuildingClosedDay existingDay = new BuildingClosedDay();
            existingDay.setId(1);
            existingDay.setBuilding(otherBuilding);

            DaysOffRequest request = DaysOffRequest.builder()
                    .reason("Updated")
                    .build();

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingClosedDayRepository.findById(1)).thenReturn(Optional.of(existingDay));

            assertThatThrownBy(() -> buildingService.updateBuildingDayOff(1, 1, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("Should delete building day off")
        void shouldDeleteBuildingDayOff() {
            BuildingClosedDay existingDay = new BuildingClosedDay();
            existingDay.setId(1);
            existingDay.setBuilding(testBuilding);

            when(buildingRepository.findById(1)).thenReturn(Optional.of(testBuilding));
            when(buildingClosedDayRepository.findById(1)).thenReturn(Optional.of(existingDay));

            buildingService.deleteBuildingDayOff(1, 1);

            verify(buildingClosedDayRepository).delete(existingDay);
        }
    }
}
