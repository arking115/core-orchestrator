package com.lab.orchestrator.service;

import com.lab.orchestrator.model.LabConfig;
import com.lab.orchestrator.repository.LabConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerService {

    private final CommandExecutionService commandExecutionService;
    private final LabConfigRepository labConfigRepository;

    @Value("${storage.base-path}")
    private String basePath;

    @Value("${storage.container-path:/home/student}")
    private String containerPath;

    public void startContainer(String studentId, int coreNumber, double cpuLimit, int assignedPort) {
        validateStartContainerInputs(studentId, coreNumber, cpuLimit, assignedPort);

        LabConfig config = labConfigRepository.findById(1L).orElse(null);
        String imageName = (config != null && config.getImageName() != null && !config.getImageName().isBlank())
                ? config.getImageName()
                : "ubuntu-rt-base";

        String sanitizedImageName = sanitizeForPath(imageName);
        ensureStudentDirectory(sanitizedImageName, studentId);

        String volumeMapping = String.format("%s/%s/%s:%s", basePath, sanitizedImageName, studentId, containerPath);

        String portMapping = String.format("%d:22", assignedPort);

        String command = String.format(
                "docker run -d --name %s --cpuset-cpus=\"%d\" --cpus=\"%s\" -p %s -v %s --cap-add=SYS_NICE --privileged %s",
                studentId, coreNumber, cpuLimit, portMapping, volumeMapping, imageName
        );

        log.info("Starting Docker container for student {} with command: {}", studentId, command);
        try {
            commandExecutionService.executeCommand(command);
        } catch (Exception e) {
            log.error("Failed to start container for student {}, rolling back directory creation", studentId, e);
            rollbackStudentDirectory(sanitizedImageName, studentId);
            throw e;
        }
    }

    public void stopContainer(String studentId) {
        String stopCommand = String.format("docker stop %s || true", studentId);
        String removeCommand = String.format("docker rm %s || true", studentId);

        log.info("Stopping Docker container for student {}", studentId);
        commandExecutionService.executeCommand(stopCommand);

        log.info("Removing Docker container for student {}", studentId);
        commandExecutionService.executeCommand(removeCommand);
    }

    private void ensureStudentDirectory(String imageDir, String studentId) {
        String studentDirectory = String.format("%s/%s/%s", basePath, imageDir, studentId);
        String mkdirCommand = String.format("mkdir -p %s", studentDirectory);

        log.info("Ensuring directory exists for student {}: {}", studentId, studentDirectory);
        commandExecutionService.executeCommand(mkdirCommand);
    }

    private void rollbackStudentDirectory(String imageDir, String studentId) {
        String studentDirectory = String.format("%s/%s/%s", basePath, imageDir, studentId);
        String rmCommand = String.format("rm -rf %s", studentDirectory);

        log.info("Rolling back directory for student {}: {}", studentId, studentDirectory);
        try {
            commandExecutionService.executeCommand(rmCommand);
        } catch (Exception rollbackEx) {
            log.error("CRITICAL: Failed to rollback directory {} - manual cleanup required", studentDirectory, rollbackEx);
        }
    }

    private String sanitizeForPath(String imageName) {
        return imageName
                .replace("/", "_")
                .replace(":", "_")
                .replace(" ", "_");
    }

    private static void validateStartContainerInputs(String studentId, int coreNumber, double cpuLimit, int assignedPort) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("studentId must not be null or blank");
        }
        if (coreNumber <= 0) {
            throw new IllegalArgumentException("coreNumber must be > 0");
        }
        if (cpuLimit <= 0) {
            throw new IllegalArgumentException("cpuLimit must be > 0");
        }
        if (assignedPort <= 0) {
            throw new IllegalArgumentException("assignedPort must be > 0");
        }
    }
}

