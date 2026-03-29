package com.lab.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.lab.orchestrator.exception.RemoteServiceException;
import com.lab.orchestrator.service.CommandExecutionService;
import com.lab.orchestrator.service.ServerMetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerMetricsServiceTest {

    @Mock
    private CommandExecutionService commandExecutionService;

    @InjectMocks
    private ServerMetricsService serverMetricsService;

    @Test
    void getTotalServerCores_parsesTrimmedOutput() {
        when(commandExecutionService.executeCommand("nproc")).thenReturn("16\n");
        assertEquals(16, serverMetricsService.getTotalServerCores());
    }

    @Test
    void getTotalServerCores_invalidOutput_throws() {
        when(commandExecutionService.executeCommand("nproc")).thenReturn("not-a-number");
        assertThrows(RemoteServiceException.class, () -> serverMetricsService.getTotalServerCores());
    }

    @Test
    void getTotalServerCores_sshFailure_wrapsAsRemoteServiceException() {
        when(commandExecutionService.executeCommand("nproc")).thenThrow(new RuntimeException("connection refused"));
        RemoteServiceException ex = assertThrows(
                RemoteServiceException.class, () -> serverMetricsService.getTotalServerCores());
        assertEquals("connection refused", ex.getCause().getMessage());
    }
}
