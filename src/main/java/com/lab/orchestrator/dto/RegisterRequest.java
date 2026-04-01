package com.lab.orchestrator.dto;

import com.lab.orchestrator.model.Role;

public record RegisterRequest(String username, String password, Role role) {}

