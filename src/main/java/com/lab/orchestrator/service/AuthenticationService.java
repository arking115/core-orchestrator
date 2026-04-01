package com.lab.orchestrator.service;

import com.lab.orchestrator.dto.AuthenticationRequest;
import com.lab.orchestrator.dto.AuthenticationResponse;
import com.lab.orchestrator.model.User;
import com.lab.orchestrator.repository.UserRepository;
import com.lab.orchestrator.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

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
}

