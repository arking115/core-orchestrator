package com.lab.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lab.orchestrator.dto.ServerCapacityResponse;
import com.lab.orchestrator.service.CommandExecutionService;
import com.lab.orchestrator.service.ServerMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerMetricsServiceTest {

    @Mock
    private CommandExecutionService commandExecutionService;

    private ServerMetricsService serverMetricsService;

    @BeforeEach
    void setUp() {
        serverMetricsService = new ServerMetricsService(commandExecutionService);
    }

    @Test
    void initializeServerCapacity_parsesTrimmedNprocAndCaches() {
        when(commandExecutionService.executeCommand("nproc")).thenReturn("16\n");

        serverMetricsService.initializeServerCapacity();

        ServerCapacityResponse r = serverMetricsService.getServerCapacity();
        assertEquals(16, r.cores());
        assertEquals(true, r.reliable());
        assertNull(r.message());
        verify(commandExecutionService, times(1)).executeCommand("nproc");
    }

    @Test
    void getServerCapacity_doesNotCallSshAgain() {
        when(commandExecutionService.executeCommand("nproc")).thenReturn("4");

        serverMetricsService.initializeServerCapacity();

        ServerCapacityResponse first = serverMetricsService.getServerCapacity();
        ServerCapacityResponse second = serverMetricsService.getServerCapacity();
        assertEquals(4, first.cores());
        assertEquals(4, second.cores());
        assertEquals(true, first.reliable());
        assertEquals(true, second.reliable());
        assertNull(first.message());

        verify(commandExecutionService, times(1)).executeCommand("nproc");
    }

    @Test
    void initializeServerCapacity_invalidOutput_usesDefaultWithUnreliableFlag() {
        when(commandExecutionService.executeCommand("nproc")).thenReturn("not-a-number");

        serverMetricsService.initializeServerCapacity();

        ServerCapacityResponse r = serverMetricsService.getServerCapacity();
        assertEquals(8, r.cores());
        assertEquals(false, r.reliable());
        assertEquals(
                "Could not parse remote nproc output; using default capacity.",
                r.message());
    }

    @Test
    void initializeServerCapacity_sshFailure_usesDefaultWithUnreliableFlag() {
        when(commandExecutionService.executeCommand("nproc")).thenThrow(new RuntimeException("connection refused"));

        serverMetricsService.initializeServerCapacity();

        ServerCapacityResponse r = serverMetricsService.getServerCapacity();
        assertEquals(8, r.cores());
        assertEquals(false, r.reliable());
        assertEquals(
                "Could not query remote server; using default capacity.",
                r.message());
    }
}
