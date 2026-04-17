package com.lab.orchestrator.controller;

import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.model.User;
import com.lab.orchestrator.exception.InvalidStudentIdException;
import com.lab.orchestrator.repository.UserRepository;
import com.lab.orchestrator.service.LabSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student")
public class StudentLabController {

    private final LabSessionService labSessionService;
    private final UserRepository userRepository;

    @PostMapping("/start")
    public LabSession start() {
        return labSessionService.startSession(getAuthenticatedStudentId());
    }

    @PostMapping("/stop")
    public void stop() {
        labSessionService.stopSession(getAuthenticatedStudentId());
    }

    private String getAuthenticatedStudentId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? null : authentication.getName();
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("No authenticated user");
        }

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getStudentId() == null || user.getStudentId().isBlank()) {
            throw new InvalidStudentIdException("studentId is missing for authenticated student");
        }

        return user.getStudentId();
    }
}
