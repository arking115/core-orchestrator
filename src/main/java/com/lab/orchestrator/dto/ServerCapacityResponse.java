package com.lab.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerCapacityResponse(
        int cores,
        boolean reliable,
        String message
) {}
