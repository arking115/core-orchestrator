package com.lab.orchestrator.service;

import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.repository.LabSessionRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabSessionService {

    private final LabSessionRepository labSessionRepository;
    private final PortManagerService portManagerService;
    private final CoreAllocationService coreAllocationService;
    private final DockerService dockerService;

    public LabSessionService(LabSessionRepository labSessionRepository,
                           PortManagerService portManagerService,
                           CoreAllocationService coreAllocationService,
                           DockerService dockerService) {
        this.labSessionRepository = labSessionRepository;
        this.portManagerService = portManagerService;
        this.coreAllocationService = coreAllocationService;
        this.dockerService = dockerService;
    }

    @Transactional
    public LabSession startSession(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("studentId must not be null or blank");
        }
        return labSessionRepository.findById(studentId)
                .orElseGet(() -> {
                    int assignedPort = portManagerService.getAvailablePort();

                    CoreAllocation allocation = coreAllocationService.getNextAvailableCore();
                    int coreNumber = allocation.getCoreNumber();
                    double cpuLimit = allocation.getCpuLimit();

                    dockerService.startContainer(studentId, coreNumber, cpuLimit, assignedPort);

                    LabSession session = new LabSession();
                    session.setStudentId(studentId);
                    session.setAssignedPort(assignedPort);
                    session.setAssignedCore(coreNumber);
                    session.setStartTime(LocalDateTime.now());

                    return labSessionRepository.save(session);
                });
    }
}
