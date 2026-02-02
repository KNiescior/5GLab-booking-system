package com._glab.booking_system.booking.service;

import com._glab.booking_system.booking.model.LabClosedDay;
import com._glab.booking_system.booking.repository.LabClosedDayRepository;
import com._glab.booking_system.booking.request.DaysOffRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DaysOffServiceTest {

    @Mock
    private LabClosedDayRepository labClosedDayRepository;

    private DaysOffService daysOffService;

    @BeforeEach
    void setUp() {
        daysOffService = new DaysOffService(labClosedDayRepository);
    }

    @Nested
    @DisplayName("Get University Days Off Tests")
    class GetUniversityDaysOffTests {

        @Test
        @DisplayName("Should return all university days off (lab is null)")
        void shouldReturnAllUniversityDaysOff() {
            LabClosedDay day1 = new LabClosedDay();
            day1.setId(1);
            day1.setLab(null); // university-wide
            day1.setSpecificDate(LocalDate.of(2026, 12, 25));
            day1.setReason("Christmas");

            LabClosedDay day2 = new LabClosedDay();
            day2.setId(2);
            day2.setLab(null); // university-wide
            day2.setRecurringDayOfWeek(7); // Sunday
            day2.setReason("Closed on Sundays");

            when(labClosedDayRepository.findByLabIsNull()).thenReturn(List.of(day1, day2));

            List<LabClosedDay> result = daysOffService.getUniversityDaysOff();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getReason()).isEqualTo("Christmas");
            assertThat(result.get(1).getRecurringDayOfWeek()).isEqualTo(7);
        }

        @Test
        @DisplayName("Should return empty list when no university days off")
        void shouldReturnEmptyListWhenNoUniversityDaysOff() {
            when(labClosedDayRepository.findByLabIsNull()).thenReturn(List.of());

            List<LabClosedDay> result = daysOffService.getUniversityDaysOff();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Add University Day Off Tests")
    class AddUniversityDayOffTests {

        @Test
        @DisplayName("Should add university day off with specific date")
        void shouldAddUniversityDayOffWithSpecificDate() {
            DaysOffRequest request = DaysOffRequest.builder()
                    .specificDate(LocalDate.of(2026, 12, 25))
                    .reason("Christmas")
                    .build();

            when(labClosedDayRepository.save(any(LabClosedDay.class))).thenAnswer(i -> {
                LabClosedDay day = i.getArgument(0);
                day.setId(1);
                return day;
            });

            LabClosedDay result = daysOffService.addUniversityDayOff(request);

            assertThat(result.getLab()).isNull(); // university-wide
            assertThat(result.getSpecificDate()).isEqualTo(LocalDate.of(2026, 12, 25));
            assertThat(result.getReason()).isEqualTo("Christmas");

            ArgumentCaptor<LabClosedDay> captor = ArgumentCaptor.forClass(LabClosedDay.class);
            verify(labClosedDayRepository).save(captor.capture());
            assertThat(captor.getValue().getLab()).isNull();
        }

        @Test
        @DisplayName("Should add university day off with recurring day of week")
        void shouldAddUniversityDayOffWithRecurringDay() {
            DaysOffRequest request = DaysOffRequest.builder()
                    .recurringDayOfWeek(7) // Sunday
                    .reason("Closed on Sundays")
                    .build();

            when(labClosedDayRepository.save(any(LabClosedDay.class))).thenAnswer(i -> {
                LabClosedDay day = i.getArgument(0);
                day.setId(1);
                return day;
            });

            LabClosedDay result = daysOffService.addUniversityDayOff(request);

            assertThat(result.getLab()).isNull();
            assertThat(result.getRecurringDayOfWeek()).isEqualTo(7);
            assertThat(result.getReason()).isEqualTo("Closed on Sundays");
        }

        @Test
        @DisplayName("Should throw exception when neither date nor day of week is set")
        void shouldThrowExceptionWhenNeitherDateNorDaySet() {
            DaysOffRequest request = DaysOffRequest.builder()
                    .reason("Invalid request")
                    .build();

            assertThatThrownBy(() -> daysOffService.addUniversityDayOff(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("specificDate or recurringDayOfWeek");
        }
    }

    @Nested
    @DisplayName("Update University Day Off Tests")
    class UpdateUniversityDayOffTests {

        @Test
        @DisplayName("Should update university day off successfully")
        void shouldUpdateUniversityDayOffSuccessfully() {
            LabClosedDay existingDay = new LabClosedDay();
            existingDay.setId(1);
            existingDay.setLab(null); // university-wide
            existingDay.setSpecificDate(LocalDate.of(2026, 12, 25));
            existingDay.setReason("Christmas");

            DaysOffRequest request = DaysOffRequest.builder()
                    .specificDate(LocalDate.of(2026, 12, 26))
                    .reason("Boxing Day")
                    .build();

            when(labClosedDayRepository.findById(1)).thenReturn(Optional.of(existingDay));
            when(labClosedDayRepository.save(any(LabClosedDay.class))).thenAnswer(i -> i.getArgument(0));

            LabClosedDay result = daysOffService.updateUniversityDayOff(1, request);

            assertThat(result.getSpecificDate()).isEqualTo(LocalDate.of(2026, 12, 26));
            assertThat(result.getReason()).isEqualTo("Boxing Day");
        }

        @Test
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            LabClosedDay existingDay = new LabClosedDay();
            existingDay.setId(1);
            existingDay.setLab(null);
            existingDay.setSpecificDate(LocalDate.of(2026, 12, 25));
            existingDay.setReason("Christmas");

            DaysOffRequest request = DaysOffRequest.builder()
                    .reason("Christmas Holiday")
                    .build();

            when(labClosedDayRepository.findById(1)).thenReturn(Optional.of(existingDay));
            when(labClosedDayRepository.save(any(LabClosedDay.class))).thenAnswer(i -> i.getArgument(0));

            LabClosedDay result = daysOffService.updateUniversityDayOff(1, request);

            assertThat(result.getSpecificDate()).isEqualTo(LocalDate.of(2026, 12, 25)); // unchanged
            assertThat(result.getReason()).isEqualTo("Christmas Holiday");
        }

        @Test
        @DisplayName("Should throw exception when day off not found")
        void shouldThrowExceptionWhenDayOffNotFound() {
            DaysOffRequest request = DaysOffRequest.builder()
                    .reason("Updated")
                    .build();

            when(labClosedDayRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> daysOffService.updateUniversityDayOff(999, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw exception when trying to update lab-specific day off")
        void shouldThrowExceptionWhenUpdatingLabSpecificDayOff() {
            LabClosedDay labSpecificDay = new LabClosedDay();
            labSpecificDay.setId(1);
            labSpecificDay.setLab(new com._glab.booking_system.booking.model.Lab()); // NOT university-wide
            labSpecificDay.setReason("Lab specific");

            DaysOffRequest request = DaysOffRequest.builder()
                    .reason("Updated")
                    .build();

            when(labClosedDayRepository.findById(1)).thenReturn(Optional.of(labSpecificDay));

            assertThatThrownBy(() -> daysOffService.updateUniversityDayOff(1, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Not a university day off");
        }
    }

    @Nested
    @DisplayName("Delete University Day Off Tests")
    class DeleteUniversityDayOffTests {

        @Test
        @DisplayName("Should delete university day off successfully")
        void shouldDeleteUniversityDayOffSuccessfully() {
            LabClosedDay existingDay = new LabClosedDay();
            existingDay.setId(1);
            existingDay.setLab(null); // university-wide
            existingDay.setReason("To delete");

            when(labClosedDayRepository.findById(1)).thenReturn(Optional.of(existingDay));

            daysOffService.deleteUniversityDayOff(1);

            verify(labClosedDayRepository).delete(existingDay);
        }

        @Test
        @DisplayName("Should throw exception when day off not found")
        void shouldThrowExceptionWhenDayOffNotFound() {
            when(labClosedDayRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> daysOffService.deleteUniversityDayOff(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw exception when trying to delete lab-specific day off")
        void shouldThrowExceptionWhenDeletingLabSpecificDayOff() {
            LabClosedDay labSpecificDay = new LabClosedDay();
            labSpecificDay.setId(1);
            labSpecificDay.setLab(new com._glab.booking_system.booking.model.Lab()); // NOT university-wide

            when(labClosedDayRepository.findById(1)).thenReturn(Optional.of(labSpecificDay));

            assertThatThrownBy(() -> daysOffService.deleteUniversityDayOff(1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Not a university day off");
        }
    }
}
