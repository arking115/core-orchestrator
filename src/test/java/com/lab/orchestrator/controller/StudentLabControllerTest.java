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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.orchestrator.dto.LabStartRequest;
import com.lab.orchestrator.exception.GlobalExceptionHandler;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.service.LabSessionService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentLabController.class)
@Import(GlobalExceptionHandler.class)
class StudentLabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LabSessionService labSessionService;

    @Test
    @DisplayName("POST /api/student/start with valid request returns 200 OK and session JSON")
    void start_validRequest_returns200AndSessionDetails() throws Exception {
        LabSession session = new LabSession();
        session.setStudentId("student123");
        session.setAssignedPort(30005);
        session.setAssignedCore(1);
        session.setStartTime(LocalDateTime.of(2025, 3, 5, 10, 0, 0));

        when(labSessionService.startSession(eq("student123"))).thenReturn(session);

        LabStartRequest request = new LabStartRequest();
        request.setStudentId("student123");
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/student/start")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("student123"))
                .andExpect(jsonPath("$.assignedPort").value(30005))
                .andExpect(jsonPath("$.assignedCore").value(1))
                .andExpect(jsonPath("$.startTime").value("2025-03-05T10:00:00"));

        verify(labSessionService).startSession("student123");
    }

    @Test
    @DisplayName("POST /api/student/start with malformed JSON returns 400 and does not call service")
    void start_malformedJson_returnsBadRequestAndDoesNotCallService() throws Exception {
        String malformedJson = "{ \"studentId\": \"student123\"";

        mockMvc.perform(post("/api/student/start")
                        .contentType(APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(labSessionService);
    }

    @Test
    @DisplayName("POST /api/student/start when lab not initialized returns 500 (GlobalExceptionHandler)")
    void start_uninitializedLab_returns500() throws Exception {
        LabStartRequest request = new LabStartRequest();
        request.setStudentId("student123");
        String requestJson = objectMapper.writeValueAsString(request);

        doThrow(new IllegalStateException("No lab config. Call initializeCores first."))
                .when(labSessionService).startSession(eq("student123"));

        mockMvc.perform(post("/api/student/start")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No lab config. Call initializeCores first."));
    }

    @Test
    @DisplayName("POST /api/student/start when at capacity returns 500 (GlobalExceptionHandler)")
    void start_atCapacity_returns500() throws Exception {
        LabStartRequest request = new LabStartRequest();
        request.setStudentId("student123");
        String requestJson = objectMapper.writeValueAsString(request);

        doThrow(new IllegalStateException("At capacity: cannot allocate more students."))
                .when(labSessionService).startSession(eq("student123"));

        mockMvc.perform(post("/api/student/start")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("At capacity: cannot allocate more students."));
    }

    @Test
    @DisplayName("POST /api/student/start when infrastructure fails returns 500 without leaking details")
    void start_infrastructureFailure_returns500WithoutSensitiveDetails() throws Exception {
        LabStartRequest request = new LabStartRequest();
        request.setStudentId("student123");
        String requestJson = objectMapper.writeValueAsString(request);

        doThrow(new RuntimeException("Docker command failed"))
                .when(labSessionService).startSession(eq("student123"));

        mockMvc.perform(post("/api/student/start")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred."));

        verify(labSessionService).startSession("student123");
    }
}
