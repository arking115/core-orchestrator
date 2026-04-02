package com.lab.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.lab.orchestrator.repository.LabSessionRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortManagerServiceTest {

    @Mock
    private LabSessionRepository labSessionRepository;

    @InjectMocks
    private PortManagerService portManagerService;

    @Test
    @DisplayName("When no ports are taken, the start port is returned")
    void getAvailablePort_noPortsTaken_returnsStartPort() {
        when(labSessionRepository.findAllAssignedPorts()).thenReturn(List.of());

        int port = portManagerService.getAvailablePort();

        assertEquals(PortManagerService.START_PORT, port);
    }

    @Test
    @DisplayName("When all ports in the range are taken, an exception is thrown")
    void getAvailablePort_allPortsTaken_throwsRuntimeException() {
        List<Integer> usedPorts = new ArrayList<>();
        for (int port = PortManagerService.START_PORT; port <= PortManagerService.MAX_PORT; port++) {
            usedPorts.add(port);
        }
        when(labSessionRepository.findAllAssignedPorts()).thenReturn(usedPorts);

        assertThrows(RuntimeException.class, () -> portManagerService.getAvailablePort());
    }

    @Test
    @DisplayName("When there is a gap, the lowest free port is returned")
    void getAvailablePort_gapInPorts_returnsLowestFreePort() {
        int first = PortManagerService.START_PORT;
        List<Integer> usedPorts = List.of(first, first + 2, first + 3);
        when(labSessionRepository.findAllAssignedPorts()).thenReturn(usedPorts);

        int port = portManagerService.getAvailablePort();

        assertEquals(first + 1, port);
    }
}

