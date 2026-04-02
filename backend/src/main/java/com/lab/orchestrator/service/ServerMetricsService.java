package com.lab.orchestrator.service;

import com.lab.orchestrator.dto.ServerCapacityResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ServerMetricsService {

    private static final int DEFAULT_CORE_COUNT = 8;

    private static final String MSG_PARSE_FAILED =
            "Could not parse remote nproc output; using default capacity.";
    private static final String MSG_REMOTE_FAILED =
            "Could not query remote server; using default capacity.";

    private final CommandExecutionService commandExecutionService;

    private Integer cachedTotalCores;
    private boolean coresReliable;
    private String capacityMessage;

    public ServerMetricsService(CommandExecutionService commandExecutionService) {
        this.commandExecutionService = commandExecutionService;
    }

    @PostConstruct
    public void initializeServerCapacity() {
        try {
            String output = commandExecutionService.executeCommand("nproc");
            String trimmed = output.trim();
            cachedTotalCores = Integer.parseInt(trimmed);
            coresReliable = true;
            capacityMessage = null;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse nproc output at startup, using default core count {}", DEFAULT_CORE_COUNT, e);
            applyFallback(MSG_PARSE_FAILED);
        } catch (RuntimeException e) {
            log.warn("Failed to query remote server capacity at startup, using default core count {}", DEFAULT_CORE_COUNT, e);
            applyFallback(MSG_REMOTE_FAILED);
        }
    }

    private void applyFallback(String message) {
        cachedTotalCores = DEFAULT_CORE_COUNT;
        coresReliable = false;
        capacityMessage = message;
    }

    public ServerCapacityResponse getServerCapacity() {
        return new ServerCapacityResponse(cachedTotalCores, coresReliable, capacityMessage);
    }
}
