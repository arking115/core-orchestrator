package com.lab.orchestrator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.orchestrator.dto.AuthenticationRequest;
import com.lab.orchestrator.dto.AuthenticationResponse;
import com.lab.orchestrator.dto.RegisterRequest;
import com.lab.orchestrator.exception.GlobalExceptionHandler;
import com.lab.orchestrator.repository.UserRepository;
import com.lab.orchestrator.security.JwtAuthenticationFilter;
import com.lab.orchestrator.security.JwtService;
import com.lab.orchestrator.security.SecurityConfig;
import com.lab.orchestrator.service.AuthenticationService;
import com.lab.orchestrator.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void login_ValidRequest_Returns200AndToken() throws Exception {
        when(authenticationService.authenticate(any()))
                .thenReturn(new AuthenticationResponse("dummy-jwt-token"));

        AuthenticationRequest request = new AuthenticationRequest("dawg", "lester");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy-jwt-token"));
    }

    @Test
    void login_InvalidCredentials_ReturnsUnauthorizedOrForbidden() throws Exception {
        when(authenticationService.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AuthenticationRequest request = new AuthenticationRequest("dawg", "wrong");

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_ValidRequest_Returns200AndToken() throws Exception {
        when(authenticationService.register(any()))
                .thenReturn(new AuthenticationResponse("dummy-jwt-token"));

        RegisterRequest request = new RegisterRequest("newuser", "pw", Role.ROLE_STUDENT);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy-jwt-token"));
    }
}

