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
import com.lab.orchestrator.dto.RegisterRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void authenticate_ValidCredentials_ReturnsToken() {
        AuthenticationRequest request = new AuthenticationRequest("dawg", "lester");

        User user =
                User.builder()
                        .id(1L)
                        .username("dawg")
                        .password("hashed-password")
                        .studentId("dawg-1")
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

    @Test
    void register_NewUser_ReturnsToken() {
        RegisterRequest request = new RegisterRequest("newuser", "pw", Role.ROLE_STUDENT, "newuser-1");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByStudentId("newuser-1")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");

        User saved =
                User.builder()
                        .id(99L)
                        .username("newuser")
                        .password("hashed")
                        .studentId("newuser-1")
                        .role(Role.ROLE_STUDENT)
                        .build();

        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken(saved)).thenReturn("jwt-token-123");

        AuthenticationResponse response = authenticationService.register(request);

        assertEquals("jwt-token-123", response.token());
        verify(jwtService, times(1)).generateToken(saved);
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest("dupe", "pw", Role.ROLE_STUDENT, "dupe-1");
        when(userRepository.existsByUsername("dupe")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authenticationService.register(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void register_InvalidStudentId_ThrowsException() {
        RegisterRequest request = new RegisterRequest("newuser", "pw", Role.ROLE_STUDENT, "Rus Alexandru");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authenticationService.register(request));

        verify(userRepository, never()).existsByStudentId(any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void register_DuplicateStudentId_ThrowsException() {
        RegisterRequest request = new RegisterRequest("newuser", "pw", Role.ROLE_STUDENT, "taken-id");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByStudentId("taken-id")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authenticationService.register(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void register_TeacherWithoutStudentId_ReturnsToken() {
        RegisterRequest request = new RegisterRequest("teacher@school.ro", "pw", Role.ROLE_TEACHER, null);

        when(userRepository.existsByUsername("teacher@school.ro")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");

        User saved =
                User.builder()
                        .id(1L)
                        .username("teacher@school.ro")
                        .password("hashed")
                        .studentId(null)
                        .role(Role.ROLE_TEACHER)
                        .build();

        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken(saved)).thenReturn("jwt-teacher");

        AuthenticationResponse response = authenticationService.register(request);

        assertEquals("jwt-teacher", response.token());
        verify(userRepository, never()).existsByStudentId(any());
    }

    @Test
    void register_TeacherWithStudentId_ThrowsException() {
        RegisterRequest request =
                new RegisterRequest("teacher@school.ro", "pw", Role.ROLE_TEACHER, "should-not-send");

        when(userRepository.existsByUsername("teacher@school.ro")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authenticationService.register(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void register_StudentMissingStudentId_ThrowsException() {
        RegisterRequest request = new RegisterRequest("stu@school.ro", "pw", Role.ROLE_STUDENT, null);

        when(userRepository.existsByUsername("stu@school.ro")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authenticationService.register(request));

        verify(userRepository, never()).existsByStudentId(any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }
}

