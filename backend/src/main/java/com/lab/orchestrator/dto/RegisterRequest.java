package com.lab.orchestrator.dto;

import com.lab.orchestrator.model.Role;

/**
 * @param studentId Required for {@link Role#ROLE_STUDENT} (lab handle). Omit or null for
 *     {@link Role#ROLE_TEACHER}.
 */
public record RegisterRequest(String username, String password, Role role, String studentId) {}

