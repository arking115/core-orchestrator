package com.lab.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record ActiveLabSessionResponse(
        String studentId,
        int assignedCore,
        int assignedPort,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startTime) {}
