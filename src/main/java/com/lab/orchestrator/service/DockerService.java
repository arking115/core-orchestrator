package com.lab.orchestrator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerService {

    private final CommandExecutionService commandExecutionService;

    @Value("${storage.base-path}")
    private String basePath;

    public void startContainer(String studentId, int coreNumber, double cpuLimit) {
        ensureStudentDirectory(studentId);

        String containerName = studentId;
        String studentVolumePath = String.format("%s/%s", basePath, studentId);

        String command = String.format(
                "docker run -d --name %s --cpuset-cpus=\"%d\" --cpus=\"%s\" -v %s:/home/student/work your-image-name",
                containerName,
                coreNumber,
                cpuLimit,
                studentVolumePath
        );

        log.info("Starting Docker container for student {} on core {} with CPU limit {}", studentId, coreNumber, cpuLimit);
        commandExecutionService.executeCommand(command);
    }

    public void stopContainer(String studentId) {
        String stopCommand = String.format("docker stop %s || true", studentId);
        String removeCommand = String.format("docker rm %s || true", studentId);

        log.info("Stopping Docker container for student {}", studentId);
        commandExecutionService.executeCommand(stopCommand);

        log.info("Removing Docker container for student {}", studentId);
        commandExecutionService.executeCommand(removeCommand);
    }

    private void ensureStudentDirectory(String studentId) {
        String studentDirectory = String.format("%s/%s", basePath, studentId);
        String mkdirCommand = String.format("mkdir -p %s", studentDirectory);

        log.info("Ensuring directory exists for student {}: {}", studentId, studentDirectory);
        commandExecutionService.executeCommand(mkdirCommand);
    }
}

