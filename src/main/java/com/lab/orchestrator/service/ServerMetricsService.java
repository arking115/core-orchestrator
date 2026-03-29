package com.lab.orchestrator.service;

import com.lab.orchestrator.exception.RemoteServiceException;
import org.springframework.stereotype.Service;

@Service
public class ServerMetricsService {

    private final CommandExecutionService commandExecutionService;

    public ServerMetricsService(CommandExecutionService commandExecutionService) {
        this.commandExecutionService = commandExecutionService;
    }

    public int getTotalServerCores() {
        String trimmed = "";
        try {
            String output = commandExecutionService.executeCommand("nproc");
            trimmed = output.trim();
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new RemoteServiceException("Could not parse nproc output as integer: " + trimmed, e);
        } catch (RuntimeException e) {
            throw new RemoteServiceException("Failed to query remote server capacity", e);
        }
    }
}
