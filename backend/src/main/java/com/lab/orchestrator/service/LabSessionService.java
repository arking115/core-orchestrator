package com.lab.orchestrator.service;

import com.lab.orchestrator.dto.StopSessionsResult;
import com.lab.orchestrator.model.CoreAllocation;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.repository.LabSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class LabSessionService {

    private static final Pattern VALID_STUDENT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]*$");
    private static final int MAX_STUDENT_ID_LENGTH = 64;

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
        validateStudentId(studentId);
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

    public void stopSession(String studentId) {
        validateStudentId(studentId);

        LabSession session = labSessionRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active session found for student: " + studentId));

        log.info("Stopping session for student {}", studentId);

        dockerService.stopContainer(studentId);
        log.info("Stopped container for student {}", studentId);

        coreAllocationService.releaseCore(session.getAssignedCore());
        log.info("Released core {} for student {}", session.getAssignedCore(), studentId);

        labSessionRepository.delete(session);
        log.info("Deleted session for student {}", studentId);
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

    private void validateStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("studentId must not be null or blank");
        }
        if (studentId.length() > MAX_STUDENT_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "studentId must not exceed " + MAX_STUDENT_ID_LENGTH + " characters");
        }
        if (!VALID_STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            throw new IllegalArgumentException(
                    "studentId must start with alphanumeric and contain only alphanumeric, underscore, or hyphen characters");
        }
    }
}