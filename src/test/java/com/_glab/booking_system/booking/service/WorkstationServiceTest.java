package com._glab.booking_system.booking.service;

import com._glab.booking_system.booking.exception.LabNotFoundException;
import com._glab.booking_system.booking.exception.WorkstationNotFoundException;
import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.Workstation;
import com._glab.booking_system.booking.repository.LabRepository;
import com._glab.booking_system.booking.repository.WorkstationRepository;
import com._glab.booking_system.booking.request.CreateWorkstationRequest;
import com._glab.booking_system.booking.request.UpdateWorkstationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkstationServiceTest {

    @Mock
    private WorkstationRepository workstationRepository;

    @Mock
    private LabRepository labRepository;

    private WorkstationService workstationService;

    private Lab testLab;
    private Building testBuilding;
    private Workstation testWorkstation;

    @BeforeEach
    void setUp() {
        workstationService = new WorkstationService(workstationRepository, labRepository);

        testBuilding = new Building();
        testBuilding.setId(1);
        testBuilding.setName("Test Building");

        testLab = new Lab();
        testLab.setId(1);
        testLab.setName("Test Lab");
        testLab.setBuilding(testBuilding);
        testLab.setActive(true);

        testWorkstation = new Workstation();
        testWorkstation.setId(1);
        testWorkstation.setLab(testLab);
        testWorkstation.setIdentifier("WS-001");
        testWorkstation.setDescription("Test workstation");
        testWorkstation.setActive(true);
    }

    @Nested
    @DisplayName("Get Workstation Tests")
    class GetWorkstationTests {

        @Test
        @DisplayName("Should return workstation by ID")
        void shouldReturnWorkstationById() {
            when(workstationRepository.findById(1)).thenReturn(Optional.of(testWorkstation));

            Workstation result = workstationService.getWorkstationById(1);

            assertThat(result.getIdentifier()).isEqualTo("WS-001");
        }

        @Test
        @DisplayName("Should throw WorkstationNotFoundException when not found")
        void shouldThrowWorkstationNotFoundException() {
            when(workstationRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workstationService.getWorkstationById(999))
                    .isInstanceOf(WorkstationNotFoundException.class);
        }

        @Test
        @DisplayName("Should return workstations by lab ID")
        void shouldReturnWorkstationsByLabId() {
            when(workstationRepository.findByLabId(1)).thenReturn(List.of(testWorkstation));

            List<Workstation> result = workstationService.getWorkstationsByLabId(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIdentifier()).isEqualTo("WS-001");
        }

        @Test
        @DisplayName("Should return active workstations by lab ID")
        void shouldReturnActiveWorkstationsByLabId() {
            when(workstationRepository.findByLabIdAndActiveTrue(1)).thenReturn(List.of(testWorkstation));

            List<Workstation> result = workstationService.getActiveWorkstationsByLabId(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getActive()).isTrue();
        }

        @Test
        @DisplayName("Should return all workstations")
        void shouldReturnAllWorkstations() {
            Workstation ws2 = new Workstation();
            ws2.setId(2);
            ws2.setIdentifier("WS-002");

            when(workstationRepository.findAll()).thenReturn(List.of(testWorkstation, ws2));

            List<Workstation> result = workstationService.getAllWorkstations();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Create Workstation Tests")
    class CreateWorkstationTests {

        @Test
        @DisplayName("Should create workstation successfully")
        void shouldCreateWorkstationSuccessfully() {
            CreateWorkstationRequest request = CreateWorkstationRequest.builder()
                    .labId(1)
                    .identifier("WS-NEW")
                    .description("New workstation")
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(workstationRepository.findByLabAndIdentifier(testLab, "WS-NEW"))
                    .thenReturn(Optional.empty());
            when(workstationRepository.save(any(Workstation.class))).thenAnswer(i -> {
                Workstation ws = i.getArgument(0);
                ws.setId(2);
                return ws;
            });

            Workstation result = workstationService.createWorkstation(request);

            assertThat(result.getIdentifier()).isEqualTo("WS-NEW");
            assertThat(result.getDescription()).isEqualTo("New workstation");
            assertThat(result.getActive()).isTrue();

            ArgumentCaptor<Workstation> captor = ArgumentCaptor.forClass(Workstation.class);
            verify(workstationRepository).save(captor.capture());
            assertThat(captor.getValue().getLab()).isEqualTo(testLab);
        }

        @Test
        @DisplayName("Should throw LabNotFoundException when lab not found")
        void shouldThrowLabNotFoundException() {
            CreateWorkstationRequest request = CreateWorkstationRequest.builder()
                    .labId(999)
                    .identifier("WS-NEW")
                    .build();

            when(labRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workstationService.createWorkstation(request))
                    .isInstanceOf(LabNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when identifier already exists in lab")
        void shouldThrowExceptionWhenIdentifierExists() {
            CreateWorkstationRequest request = CreateWorkstationRequest.builder()
                    .labId(1)
                    .identifier("WS-001") // Same as testWorkstation
                    .build();

            when(labRepository.findById(1)).thenReturn(Optional.of(testLab));
            when(workstationRepository.findByLabAndIdentifier(testLab, "WS-001"))
                    .thenReturn(Optional.of(testWorkstation));

            assertThatThrownBy(() -> workstationService.createWorkstation(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("Update Workstation Tests")
    class UpdateWorkstationTests {

        @Test
        @DisplayName("Should update workstation successfully")
        void shouldUpdateWorkstationSuccessfully() {
            UpdateWorkstationRequest request = UpdateWorkstationRequest.builder()
                    .identifier("WS-UPDATED")
                    .description("Updated description")
                    .active(true)
                    .build();

            when(workstationRepository.findById(1)).thenReturn(Optional.of(testWorkstation));
            when(workstationRepository.findByLabAndIdentifier(testLab, "WS-UPDATED"))
                    .thenReturn(Optional.empty());
            when(workstationRepository.save(any(Workstation.class))).thenAnswer(i -> i.getArgument(0));

            Workstation result = workstationService.updateWorkstation(1, request);

            assertThat(result.getIdentifier()).isEqualTo("WS-UPDATED");
            assertThat(result.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            UpdateWorkstationRequest request = UpdateWorkstationRequest.builder()
                    .description("Only description updated")
                    .build();

            when(workstationRepository.findById(1)).thenReturn(Optional.of(testWorkstation));
            when(workstationRepository.save(any(Workstation.class))).thenAnswer(i -> i.getArgument(0));

            Workstation result = workstationService.updateWorkstation(1, request);

            assertThat(result.getIdentifier()).isEqualTo("WS-001"); // unchanged
            assertThat(result.getDescription()).isEqualTo("Only description updated");
        }

        @Test
        @DisplayName("Should throw WorkstationNotFoundException when not found")
        void shouldThrowWorkstationNotFoundException() {
            UpdateWorkstationRequest request = UpdateWorkstationRequest.builder()
                    .identifier("WS-UPDATED")
                    .build();

            when(workstationRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workstationService.updateWorkstation(999, request))
                    .isInstanceOf(WorkstationNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when identifier already exists for another workstation")
        void shouldThrowExceptionWhenIdentifierExistsForAnother() {
            Workstation otherWorkstation = new Workstation();
            otherWorkstation.setId(2);
            otherWorkstation.setLab(testLab);
            otherWorkstation.setIdentifier("WS-002");

            UpdateWorkstationRequest request = UpdateWorkstationRequest.builder()
                    .identifier("WS-002")
                    .build();

            when(workstationRepository.findById(1)).thenReturn(Optional.of(testWorkstation));
            when(workstationRepository.findByLabAndIdentifier(testLab, "WS-002"))
                    .thenReturn(Optional.of(otherWorkstation));

            assertThatThrownBy(() -> workstationService.updateWorkstation(1, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should allow updating to same identifier")
        void shouldAllowUpdatingToSameIdentifier() {
            UpdateWorkstationRequest request = UpdateWorkstationRequest.builder()
                    .identifier("WS-001") // Same identifier
                    .description("New description")
                    .build();

            when(workstationRepository.findById(1)).thenReturn(Optional.of(testWorkstation));
            when(workstationRepository.findByLabAndIdentifier(testLab, "WS-001"))
                    .thenReturn(Optional.of(testWorkstation)); // Same workstation
            when(workstationRepository.save(any(Workstation.class))).thenAnswer(i -> i.getArgument(0));

            Workstation result = workstationService.updateWorkstation(1, request);

            assertThat(result.getIdentifier()).isEqualTo("WS-001");
            assertThat(result.getDescription()).isEqualTo("New description");
        }
    }

    @Nested
    @DisplayName("Archive Workstation Tests")
    class ArchiveWorkstationTests {

        @Test
        @DisplayName("Should archive workstation successfully")
        void shouldArchiveWorkstationSuccessfully() {
            when(workstationRepository.findById(1)).thenReturn(Optional.of(testWorkstation));
            when(workstationRepository.save(any(Workstation.class))).thenAnswer(i -> i.getArgument(0));

            Workstation result = workstationService.archiveWorkstation(1);

            assertThat(result.getActive()).isFalse();
        }

        @Test
        @DisplayName("Should throw WorkstationNotFoundException when not found")
        void shouldThrowWorkstationNotFoundException() {
            when(workstationRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workstationService.archiveWorkstation(999))
                    .isInstanceOf(WorkstationNotFoundException.class);
        }
    }
}
