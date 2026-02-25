package com.lab.orchestrator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommandExecutionService {

    @Value("${ssh.host}")
    private String host;

    @Value("${ssh.username}")
    private String username;

    @Value("${ssh.key-path}")
    private String keyPath;

    public String executeCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command must not be null or blank");
        }

        List<String> sshCommand = List.of(
                "ssh",
                "-i", keyPath,
                "-o", "StrictHostKeyChecking=no",
                username + "@" + host,
                command
        );

        log.info("Executing SSH command: {}", String.join(" ", sshCommand));

        ProcessBuilder processBuilder = new ProcessBuilder(sshCommand);

        try {
            Process process = processBuilder.start();

            String stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            ).lines().collect(Collectors.joining(System.lineSeparator()));

            String stderr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)
            ).lines().collect(Collectors.joining(System.lineSeparator()));

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorMessage = stderr.isEmpty()
                        ? "SSH command failed with exit code " + exitCode
                        : stderr;
                throw new RuntimeException(errorMessage);
            }

            return stdout;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SSH command execution was interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute SSH command", e);
        }
    }
}