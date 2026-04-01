package com.lab.orchestrator.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lab.orchestrator.exception.GlobalExceptionHandler;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.model.Role;
import com.lab.orchestrator.model.User;
import com.lab.orchestrator.repository.UserRepository;
import com.lab.orchestrator.security.JwtAuthenticationFilter;
import com.lab.orchestrator.security.JwtService;
import com.lab.orchestrator.security.SecurityConfig;
import com.lab.orchestrator.service.LabSessionService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentLabController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
@WithMockUser(username = "student@school.ro", authorities = "ROLE_STUDENT")
class StudentLabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabSessionService labSessionService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    private User studentUser() {
        return User.builder()
                .id(1L)
                .username("student@school.ro")
                .password("pw")
                .role(Role.ROLE_STUDENT)
                .studentId("student123")
                .build();
    }

    @Test
    @DisplayName("POST /api/student/start returns 200 OK and session JSON (handle from authenticated user)")
    void start_validRequest_returns200AndSessionDetails() throws Exception {
        when(userRepository.findByUsername("student@school.ro")).thenReturn(Optional.of(studentUser()));

        LabSession session = new LabSession();
        session.setStudentId("student123");
        session.setAssignedPort(30005);
        session.setAssignedCore(1);
        session.setStartTime(LocalDateTime.of(2025, 3, 5, 10, 0, 0));

        when(labSessionService.startSession(eq("student123"))).thenReturn(session);

        mockMvc.perform(post("/api/student/start").contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("student123"))
                .andExpect(jsonPath("$.assignedPort").value(30005))
                .andExpect(jsonPath("$.assignedCore").value(1))
                .andExpect(jsonPath("$.startTime").value("2025-03-05T10:00:00"));

        verify(labSessionService).startSession("student123");
    }

    @Test
    @DisplayName("POST /api/student/start when lab not initialized returns 500 (GlobalExceptionHandler)")
    void start_uninitializedLab_returns500() throws Exception {
        when(userRepository.findByUsername("student@school.ro")).thenReturn(Optional.of(studentUser()));

        doThrow(new IllegalStateException("No lab config. Call initializeCores first."))
                .when(labSessionService).startSession(eq("student123"));

        mockMvc.perform(post("/api/student/start").contentType(APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No lab config. Call initializeCores first."));
    }

    @Test
    @DisplayName("POST /api/student/start when at capacity returns 500 (GlobalExceptionHandler)")
    void start_atCapacity_returns500() throws Exception {
        when(userRepository.findByUsername("student@school.ro")).thenReturn(Optional.of(studentUser()));

        doThrow(new IllegalStateException("At capacity: cannot allocate more students."))
                .when(labSessionService).startSession(eq("student123"));

        mockMvc.perform(post("/api/student/start").contentType(APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("At capacity: cannot allocate more students."));
    }

    @Test
    @DisplayName("POST /api/student/start when infrastructure fails returns 500 without leaking details")
    void start_infrastructureFailure_returns500WithoutSensitiveDetails() throws Exception {
        when(userRepository.findByUsername("student@school.ro")).thenReturn(Optional.of(studentUser()));

        doThrow(new RuntimeException("Docker command failed"))
                .when(labSessionService).startSession(eq("student123"));

        mockMvc.perform(post("/api/student/start").contentType(APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred."));

        verify(labSessionService).startSession("student123");
    }

    @Test
    @DisplayName("POST /api/student/start when user has no studentId returns 400")
    void start_missingStudentIdOnUser_returns400() throws Exception {
        User noHandle =
                User.builder()
                        .id(1L)
                        .username("student@school.ro")
                        .password("pw")
                        .role(Role.ROLE_STUDENT)
                        .studentId(null)
                        .build();
        when(userRepository.findByUsername("student@school.ro")).thenReturn(Optional.of(noHandle));

        mockMvc.perform(post("/api/student/start").contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid studentId"));

        verifyNoInteractions(labSessionService);
    }

    @Test
    @DisplayName("POST /api/student/stop calls service and returns 200 OK")
    void stop_validRequest_callsServiceAndReturns200() throws Exception {
        when(userRepository.findByUsername("student@school.ro")).thenReturn(Optional.of(studentUser()));

        mockMvc.perform(post("/api/student/stop").contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(labSessionService).stopSession("student123");
    }

    @Test
    @DisplayName("POST /api/student/stop when session not found returns 500")
    void stop_sessionNotFound_returns500() throws Exception {
        when(userRepository.findByUsername("student@school.ro")).thenReturn(Optional.of(studentUser()));

        doThrow(new IllegalArgumentException("No active session found for student: student123"))
                .when(labSessionService).stopSession(eq("student123"));

        mockMvc.perform(post("/api/student/stop").contentType(APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(labSessionService).stopSession("student123");
    }

    @Test
    @DisplayName("POST /api/student/stop when Docker fails returns 500 without leaking details")
    void stop_dockerFailure_returns500WithoutSensitiveDetails() throws Exception {
        when(userRepository.findByUsername("student@school.ro")).thenReturn(Optional.of(studentUser()));

        doThrow(new RuntimeException("Docker command failed"))
                .when(labSessionService).stopSession(eq("student123"));

        mockMvc.perform(post("/api/student/stop").contentType(APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred."));

        verify(labSessionService).stopSession("student123");
    }
}
