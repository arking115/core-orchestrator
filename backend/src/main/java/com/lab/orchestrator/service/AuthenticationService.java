package com.lab.orchestrator.service;

import com.lab.orchestrator.dto.AuthenticationRequest;
import com.lab.orchestrator.dto.AuthenticationResponse;
import com.lab.orchestrator.dto.RegisterRequest;
import com.lab.orchestrator.exception.InvalidStudentIdException;
import com.lab.orchestrator.model.Role;
import com.lab.orchestrator.model.User;
import com.lab.orchestrator.repository.UserRepository;
import com.lab.orchestrator.security.JwtService;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Pattern STUDENT_ID_PATTERN =
            Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final int STUDENT_ID_MIN_LEN = 3;
    private static final int STUDENT_ID_MAX_LEN = 64;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user =
                userRepository
                        .findByUsername(request.username())
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "User not found: " + request.username()));

        String token = jwtService.generateToken(user);
        return new AuthenticationResponse(token);
    }

    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        String studentIdForDb = null;
        if (request.role() == Role.ROLE_STUDENT) {
            validateStudentHandle(request.studentId());
            if (userRepository.existsByStudentId(request.studentId())) {
                throw new IllegalArgumentException("StudentId already taken");
            }
            studentIdForDb = request.studentId();
        } else if (request.role() == Role.ROLE_TEACHER) {
            if (request.studentId() != null && !request.studentId().isBlank()) {
                throw new IllegalArgumentException("StudentId must not be set for teacher accounts");
            }
        }

        User user =
                User.builder()
                        .username(request.username())
                        .password(passwordEncoder.encode(request.password()))
                        .role(request.role())
                        .studentId(studentIdForDb)
                        .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);
        return new AuthenticationResponse(token);
    }

    /**
     * Validates the lab handle for {@link Role#ROLE_STUDENT} only. Teachers omit this field.
     */
    private void validateStudentHandle(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("StudentId is required for student accounts");
        }
        if (studentId.length() < STUDENT_ID_MIN_LEN || studentId.length() > STUDENT_ID_MAX_LEN) {
            throw new InvalidStudentIdException(
                    "studentId must be between " + STUDENT_ID_MIN_LEN + " and " + STUDENT_ID_MAX_LEN + " characters");
        }
        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            throw new InvalidStudentIdException(
                    "studentId may contain lowercase letters, digits, and hyphens (no leading/trailing hyphen)");
        }
    }
}

