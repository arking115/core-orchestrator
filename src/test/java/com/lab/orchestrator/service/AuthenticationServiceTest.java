package com.lab.orchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lab.orchestrator.dto.AuthenticationRequest;
import com.lab.orchestrator.dto.AuthenticationResponse;
import com.lab.orchestrator.model.Role;
import com.lab.orchestrator.model.User;
import com.lab.orchestrator.repository.UserRepository;
import com.lab.orchestrator.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Test
    void authenticate_ValidCredentials_ReturnsToken() {
        AuthenticationRequest request = new AuthenticationRequest("dawg", "lester");

        User user =
                User.builder()
                        .id(1L)
                        .username("dawg")
                        .password("hashed-password")
                        .role(Role.ROLE_STUDENT)
                        .build();

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(userRepository.findByUsername("dawg")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token-123");

        AuthenticationResponse response = authenticationService.authenticate(request);

        verify(authenticationManager, times(1)).authenticate(any(Authentication.class));
        assertEquals("jwt-token-123", response.token());
    }

    @Test
    void authenticate_InvalidCredentials_ThrowsException() {
        AuthenticationRequest request = new AuthenticationRequest("dawg", "wrong");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));

        verify(authenticationManager, times(1)).authenticate(any(Authentication.class));
        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    void authenticate_UserNotFound_ThrowsException() {
        AuthenticationRequest request = new AuthenticationRequest("missing", "pw");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authenticationService.authenticate(request));

        verify(authenticationManager, times(1)).authenticate(any(Authentication.class));
        verify(jwtService, never()).generateToken(any());
    }
}

