package com.lab.orchestrator.service;

import com.lab.orchestrator.repository.LabSessionRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PortManagerService {

    public static final int START_PORT = 30000;
    public static final int MAX_PORT = 30200;

    private final LabSessionRepository labSessionRepository;

    public PortManagerService(LabSessionRepository labSessionRepository) {
        this.labSessionRepository = labSessionRepository;
    }

    public int getAvailablePort() {
        List<Integer> usedPortsList = labSessionRepository.findAllAssignedPorts();
        Set<Integer> usedPorts = new HashSet<>(usedPortsList);

        for (int port = START_PORT; port <= MAX_PORT; port++) {
            if (!usedPorts.contains(port)) {
                return port;
            }
        }

        throw new RuntimeException("No available ports in range " + START_PORT + "-" + MAX_PORT);
    }
}

