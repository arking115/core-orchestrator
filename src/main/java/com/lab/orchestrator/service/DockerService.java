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

    public void startContainer(String studentId, int coreNumber, double cpuLimit, int assignedPort) {
        ensureStudentDirectory(studentId);

        LabConfig config = labConfigRepository.findById(1L).orElse(null);
        String imageName = (config != null && config.getImageName() != null && !config.getImageName().isBlank())
                ? config.getImageName()
                : "ubuntu-rt-base";

        String volumeMapping = String.format("%s/%s:/home/student", basePath, studentId)

        String portMapping = String.format("%d:22", assignedPort);

        String command = String.format(
            "docker run -d --name %s --cpus=\"%s\" -p %s -v %s %s",
            studentId, cpuLimit, portMapping, volumeMapping, imageName
        );

        log.info("Starting Docker container for student {} with command: {}", studentId, command);
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

