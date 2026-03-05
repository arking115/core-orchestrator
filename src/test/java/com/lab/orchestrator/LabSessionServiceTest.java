package com.lab.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.repository.LabSessionRepository;
import com.lab.orchestrator.service.CoreAllocationService;
import com.lab.orchestrator.service.DockerService;
import com.lab.orchestrator.service.LabSessionService;
import com.lab.orchestrator.service.PortManagerService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LabSessionServiceTest {

    @Mock
    private LabSessionRepository labSessionRepository;

    @Mock
    private PortManagerService portManagerService;

    @Mock
    private CoreAllocationService coreAllocationService;

    @Mock
    private DockerService dockerService;

    @InjectMocks
    private LabSessionService labSessionService;

    @Test
    @DisplayName("startSession returns existing LabSession when one already exists for the student")
    void startSession_existingSession_returnsExisting() {
        String studentId = "student1";
        LabSession existing = new LabSession();
        existing.setStudentId(studentId);
        existing.setAssignedPort(30001);
        existing.setAssignedCore(1);
        existing.setStartTime(LocalDateTime.now().minusMinutes(5));

        when(labSessionRepository.findById(studentId)).thenReturn(Optional.of(existing));

        LabSession result = labSessionService.startSession(studentId);

        assertSame(existing, result);
        verify(labSessionRepository).findById(studentId);
        verifyNoInteractions(portManagerService, coreAllocationService, dockerService);
        verify(labSessionRepository, never()).save(any(LabSession.class));
    }

    @Test
    @DisplayName("startSession creates and persists a new LabSession when none exists")
    void startSession_noExistingSession_createsNew() {
        String studentId = "student2";

        when(labSessionRepository.findById(studentId)).thenReturn(Optional.empty());
        when(portManagerService.getAvailablePort()).thenReturn(30002);

        CoreAllocation allocation = new CoreAllocation();
        allocation.setCoreNumber(2);
        allocation.setCpuLimit(0.5);
        when(coreAllocationService.getNextAvailableCore()).thenReturn(allocation);

        when(labSessionRepository.save(any(LabSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LabSession result = labSessionService.startSession(studentId);

        assertEquals(studentId, result.getStudentId());
        assertEquals(30002, result.getAssignedPort());
        assertEquals(2, result.getAssignedCore());
        assertNotNull(result.getStartTime());

        verify(portManagerService).getAvailablePort();
        verify(coreAllocationService).getNextAvailableCore();
        verify(dockerService).startContainer(studentId, 2, 0.5, 30002);
        verify(labSessionRepository).save(any(LabSession.class));
    }

    @Test
    @DisplayName("startSession rejects null or blank studentId with IllegalArgumentException")
    void startSession_invalidStudentId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> labSessionService.startSession(null));
        assertThrows(IllegalArgumentException.class, () -> labSessionService.startSession("   "));

        verifyNoInteractions(labSessionRepository, portManagerService, coreAllocationService, dockerService);
    }

    @Test
    @DisplayName("startSession throws IllegalStateException when lab is not initialized (no cores available)")
    void startSession_labNotInitialized_throwsIllegalStateException() {
        String studentId = "student3";

        when(labSessionRepository.findById(studentId)).thenReturn(Optional.empty());
        when(portManagerService.getAvailablePort()).thenReturn(30003);
        when(coreAllocationService.getNextAvailableCore())
                .thenThrow(new IllegalStateException("No cores available. Call initializeCores first."));

        assertThrows(IllegalStateException.class, () -> labSessionService.startSession(studentId));

        verify(labSessionRepository).findById(studentId);
        verify(portManagerService).getAvailablePort();
        verify(coreAllocationService).getNextAvailableCore();
        verifyNoInteractions(dockerService);
        verify(labSessionRepository, never()).save(any(LabSession.class));
    }
}

