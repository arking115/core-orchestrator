package com.lab.orchestrator.service;

import com.lab.orchestrator.dto.StopSessionsResult;
import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.repository.LabSessionRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

    public StopSessionsResult stopAllActiveSessions() {
        List<LabSession> activeSessions = labSessionRepository.findAll();
        int totalSessions = activeSessions.size();
        log.info("Stopping {} active sessions in parallel", totalSessions);

        ConcurrentLinkedQueue<String> failedStudentIds = new ConcurrentLinkedQueue<>();

        activeSessions.parallelStream().forEach(session -> {
            try {
                dockerService.stopContainer(session.getStudentId());
                log.info("Stopped container for student {}", session.getStudentId());
            } catch (Exception e) {
                log.warn("Failed to stop container for student {} (may already be removed): {}",
                        session.getStudentId(), e.getMessage());
                failedStudentIds.add(session.getStudentId());
            }
        });

        labSessionRepository.deleteAll();
        log.info("Cleared all active sessions from database");

        List<String> failures = List.copyOf(failedStudentIds);
        int successCount = totalSessions - failures.size();

        log.info("Stop operation complete: {}/{} successful", successCount, totalSessions);
        if (!failures.isEmpty()) {
            log.warn("Failed to stop containers for students: {}", failures);
        }

        return StopSessionsResult.builder()
                .totalSessions(totalSessions)
                .successfullyStoppedCount(successCount)
                .failedStudentIds(failures)
                .build();
    }
}