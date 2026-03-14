package com.lab.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lab.orchestrator.dto.StopSessionsResult;
import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.repository.LabSessionRepository;
import com.lab.orchestrator.service.CoreAllocationService;
import com.lab.orchestrator.service.DockerService;
import com.lab.orchestrator.service.LabSessionService;
import com.lab.orchestrator.service.PortManagerService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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

    @Test
    @DisplayName("stopAllActiveSessions stops all containers in parallel and clears database when sessions exist")
    void stopsAllContainersAndClearsDatabase_whenSessionsExist() {
        LabSession session1 = new LabSession();
        session1.setStudentId("student1");
        session1.setAssignedPort(30001);
        session1.setAssignedCore(1);
        session1.setStartTime(LocalDateTime.now());

        LabSession session2 = new LabSession();
        session2.setStudentId("student2");
        session2.setAssignedPort(30002);
        session2.setAssignedCore(2);
        session2.setStartTime(LocalDateTime.now());

        when(labSessionRepository.findAll()).thenReturn(List.of(session1, session2));
        doNothing().when(dockerService).stopContainer(any());

        StopSessionsResult result = labSessionService.stopAllActiveSessions();

        verify(dockerService).stopContainer("student1");
        verify(dockerService).stopContainer("student2");
        verify(labSessionRepository).deleteAll();

        assertEquals(2, result.getTotalSessions());
        assertEquals(2, result.getSuccessfullyStoppedCount());
        assertTrue(result.getFailedStudentIds().isEmpty());
        assertTrue(result.isAllSuccessful());
    }

    @Test
    @DisplayName("stopAllActiveSessions does not call docker but clears database when no sessions exist")
    void doesNotCallDockerButClearsDatabase_whenNoSessionsExist() {
        when(labSessionRepository.findAll()).thenReturn(Collections.emptyList());

        StopSessionsResult result = labSessionService.stopAllActiveSessions();

        verifyNoInteractions(dockerService);
        verify(labSessionRepository).deleteAll();

        assertEquals(0, result.getTotalSessions());
        assertEquals(0, result.getSuccessfullyStoppedCount());
        assertTrue(result.getFailedStudentIds().isEmpty());
        assertTrue(result.isAllSuccessful());
    }

    @Test
    @DisplayName("stopAllActiveSessions continues to stop next containers and clears database when one container throws exception")
    void continuesToStopNextContainersAndClearsDatabase_whenOneContainerThrowsException() {
        LabSession session1 = new LabSession();
        session1.setStudentId("student1");
        session1.setAssignedPort(30001);
        session1.setAssignedCore(1);
        session1.setStartTime(LocalDateTime.now());

        LabSession session2 = new LabSession();
        session2.setStudentId("student2");
        session2.setAssignedPort(30002);
        session2.setAssignedCore(2);
        session2.setStartTime(LocalDateTime.now());

        when(labSessionRepository.findAll()).thenReturn(List.of(session1, session2));
        doThrow(new RuntimeException("Container not found")).when(dockerService).stopContainer("student1");
        doNothing().when(dockerService).stopContainer("student2");

        StopSessionsResult result = labSessionService.stopAllActiveSessions();

        verify(dockerService).stopContainer("student1");
        verify(dockerService).stopContainer("student2");
        verify(labSessionRepository).deleteAll();

        assertEquals(2, result.getTotalSessions());
        assertEquals(1, result.getSuccessfullyStoppedCount());
        assertEquals(1, result.getFailedStudentIds().size());
        assertTrue(result.getFailedStudentIds().contains("student1"));
        assertFalse(result.isAllSuccessful());
    }
}

