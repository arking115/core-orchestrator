package com.lab.orchestrator.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lab.orchestrator.dto.StopSessionsResult;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.service.CoreAllocationService;
import com.lab.orchestrator.service.LabSessionService;
import com.lab.orchestrator.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TeacherLabController.class)
@Import(GlobalExceptionHandler.class)
class TeacherLabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoreAllocationService coreAllocationService;

    @MockBean
    private LabSessionService labSessionService;

    @Test
    @DisplayName("POST /api/teacher/initialize with valid request calls initializeCores and returns 200 OK")
    void initialize_validRequest_callsServiceAndReturns200() throws Exception {
        String json = """
                {
                    "totalStudents": 10,
                    "coreNumbers": [1, 2, 3],
                    "imageName": "my-lab-image"
                }
                """;

        mockMvc.perform(post("/api/teacher/initialize")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(coreAllocationService).initializeCores(
                eq(10),
                eq(List.of(1, 2, 3)),
                eq("my-lab-image"));
    }

    @Test
    @DisplayName("POST /api/teacher/initialize with malformed JSON returns 400 and does not call service")
    void initialize_malformedJson_returnsBadRequestAndDoesNotCallService() throws Exception {
        String malformedJson = "{ \"totalStudents\": 10, \"coreNumbers\": [1, 2, 3] ";

        mockMvc.perform(post("/api/teacher/initialize")
                        .contentType(APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(coreAllocationService);
    }

    @Test
    @DisplayName("POST /api/teacher/initialize when service throws returns 500 (handled by GlobalExceptionHandler)")
    void initialize_serviceThrows_returns500() throws Exception {
        String json = """
                {
                    "totalStudents": 10,
                    "coreNumbers": [1, 2, 3],
                    "imageName": "my-lab-image"
                }
                """;

        doThrow(new IllegalArgumentException("At least one core must be provided for allocation."))
                .when(coreAllocationService).initializeCores(eq(10), eq(List.of(1, 2, 3)), eq("my-lab-image"));

        mockMvc.perform(post("/api/teacher/initialize")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/teacher/initialize with zero students returns 500")
    void initialize_zeroStudents_returns500() throws Exception {
        String json = """
                {
                    "totalStudents": 0,
                    "coreNumbers": [1, 2, 3],
                    "imageName": "my-lab-image"
                }
                """;

        doThrow(new IllegalArgumentException("totalStudents must be positive and non-zero."))
                .when(coreAllocationService)
                .initializeCores(eq(0), eq(List.of(1, 2, 3)), eq("my-lab-image"));

        mockMvc.perform(post("/api/teacher/initialize")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/teacher/initialize calls stopAllActiveSessions before initializeCores")
    void initialize_callsStopAllActiveSessionsBeforeInitializeCores() throws Exception {
        String json = """
                {
                    "totalStudents": 10,
                    "coreNumbers": [1, 2, 3],
                    "imageName": "my-lab-image"
                }
                """;

        when(labSessionService.stopAllActiveSessions()).thenReturn(
                StopSessionsResult.builder()
                        .totalSessions(0)
                        .successfullyStoppedCount(0)
                        .failedStudentIds(Collections.emptyList())
                        .build());

        mockMvc.perform(post("/api/teacher/initialize")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        InOrder inOrder = inOrder(labSessionService, coreAllocationService);
        inOrder.verify(labSessionService).stopAllActiveSessions();
        inOrder.verify(coreAllocationService).initializeCores(eq(10), eq(List.of(1, 2, 3)), eq("my-lab-image"));
    }

    @Test
    @DisplayName("POST /api/teacher/stop-all calls stopAllActiveSessions and returns 200 OK with result")
    void stopAll_callsServiceAndReturns200() throws Exception {
        when(labSessionService.stopAllActiveSessions()).thenReturn(
                StopSessionsResult.builder()
                        .totalSessions(5)
                        .successfullyStoppedCount(5)
                        .failedStudentIds(Collections.emptyList())
                        .build());

        mockMvc.perform(post("/api/teacher/stop-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(5))
                .andExpect(jsonPath("$.successfullyStoppedCount").value(5))
                .andExpect(jsonPath("$.failedStudentIds").isEmpty())
                .andExpect(jsonPath("$.allSuccessful").value(true));

        verify(labSessionService).stopAllActiveSessions();
    }

    @Test
    @DisplayName("POST /api/teacher/stop-all when service throws RuntimeException returns 500")
    void stopAll_serviceThrowsRuntimeException_returns500() throws Exception {
        when(labSessionService.stopAllActiveSessions())
                .thenThrow(new RuntimeException("Docker daemon not responding"));

        mockMvc.perform(post("/api/teacher/stop-all"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/teacher/stop-all when service throws IllegalStateException returns 500")
    void stopAll_serviceThrowsIllegalStateException_returns500() throws Exception {
        when(labSessionService.stopAllActiveSessions())
                .thenThrow(new IllegalStateException("Database connection lost"));

        mockMvc.perform(post("/api/teacher/stop-all"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/teacher/stop-all completes even if slow (simulated delay)")
    void stopAll_slowExecution_eventuallyCompletes() throws Exception {
        AtomicBoolean methodCalled = new AtomicBoolean(false);

        when(labSessionService.stopAllActiveSessions()).thenAnswer(invocation -> {
            Thread.sleep(100);
            methodCalled.set(true);
            return StopSessionsResult.builder()
                    .totalSessions(10)
                    .successfullyStoppedCount(10)
                    .failedStudentIds(Collections.emptyList())
                    .build();
        });

        mockMvc.perform(post("/api/teacher/stop-all"))
                .andExpect(status().isOk());

        verify(labSessionService).stopAllActiveSessions();
        assert methodCalled.get() : "stopAllActiveSessions should have been called";
    }

    @Test
    @DisplayName("POST /api/teacher/stop-all can be interrupted during long operation")
    void stopAll_interruptedDuringExecution_throwsException() throws Exception {
        when(labSessionService.stopAllActiveSessions())
                .thenThrow(new RuntimeException("Operation interrupted: container stop timed out"));

        mockMvc.perform(post("/api/teacher/stop-all"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/teacher/initialize fails if stopAllActiveSessions throws, initializeCores not called")
    void initialize_stopAllSessionsFails_initializeCoresNotCalled() throws Exception {
        String json = """
                {
                    "totalStudents": 10,
                    "coreNumbers": [1, 2, 3],
                    "imageName": "my-lab-image"
                }
                """;

        when(labSessionService.stopAllActiveSessions())
                .thenThrow(new RuntimeException("Failed to stop containers"));

        mockMvc.perform(post("/api/teacher/initialize")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());

        verify(labSessionService).stopAllActiveSessions();
        verifyNoInteractions(coreAllocationService);
    }

    @Test
    @DisplayName("POST /api/teacher/stop-all returns partial failures when some containers fail to stop")
    void stopAll_partialFailure_returnsFailedStudentIds() throws Exception {
        when(labSessionService.stopAllActiveSessions()).thenReturn(
                StopSessionsResult.builder()
                        .totalSessions(5)
                        .successfullyStoppedCount(3)
                        .failedStudentIds(List.of("student2", "student4"))
                        .build());

        mockMvc.perform(post("/api/teacher/stop-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(5))
                .andExpect(jsonPath("$.successfullyStoppedCount").value(3))
                .andExpect(jsonPath("$.failedStudentIds").isArray())
                .andExpect(jsonPath("$.failedStudentIds[0]").value("student2"))
                .andExpect(jsonPath("$.failedStudentIds[1]").value("student4"))
                .andExpect(jsonPath("$.allSuccessful").value(false));
    }

    @Test
    @DisplayName("POST /api/teacher/start/{studentId} with valid studentId calls service and returns 200 OK with LabSession")
    void startStudent_validStudentId_callsServiceAndReturns200() throws Exception {
        LabSession dummySession = new LabSession();
        dummySession.setStudentId("student123");
        dummySession.setAssignedPort(8080);
        dummySession.setAssignedCore(1);
        dummySession.setStartTime(LocalDateTime.now());

        when(labSessionService.startSession("student123")).thenReturn(dummySession);

        mockMvc.perform(post("/api/teacher/start/student123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("student123"))
                .andExpect(jsonPath("$.assignedPort").value(8080))
                .andExpect(jsonPath("$.assignedCore").value(1));

        verify(labSessionService).startSession("student123");
    }

    @Test
    @DisplayName("POST /api/teacher/start/{studentId} when lab not initialized returns 500")
    void startStudent_labNotInitialized_returns500() throws Exception {
        doThrow(new IllegalStateException("No cores available"))
                .when(labSessionService).startSession("student123");

        mockMvc.perform(post("/api/teacher/start/student123"))
                .andExpect(status().isInternalServerError());

        verify(labSessionService).startSession("student123");
    }

    @Test
    @DisplayName("POST /api/teacher/start/{studentId} with invalid studentId returns 500")
    void startStudent_invalidStudentId_returns500() throws Exception {
        doThrow(new IllegalArgumentException("studentId must start with alphanumeric and contain only alphanumeric, underscore, or hyphen characters"))
                .when(labSessionService).startSession("invalid@id");

        mockMvc.perform(post("/api/teacher/start/invalid@id"))
                .andExpect(status().isInternalServerError());

        verify(labSessionService).startSession("invalid@id");
    }

    @Test
    @DisplayName("POST /api/teacher/stop/{studentId} with valid studentId calls service and returns 200 OK")
    void stopStudent_validStudentId_callsServiceAndReturns200() throws Exception {
        mockMvc.perform(post("/api/teacher/stop/student123"))
                .andExpect(status().isOk());

        verify(labSessionService).stopSession("student123");
    }

    @Test
    @DisplayName("POST /api/teacher/stop/{studentId} when session not found returns 500")
    void stopStudent_sessionNotFound_returns500() throws Exception {
        doThrow(new IllegalArgumentException("No active session found for student: student123"))
                .when(labSessionService).stopSession("student123");

        mockMvc.perform(post("/api/teacher/stop/student123"))
                .andExpect(status().isInternalServerError());

        verify(labSessionService).stopSession("student123");
    }

    @Test
    @DisplayName("POST /api/teacher/stop/{studentId} when Docker fails returns 500")
    void stopStudent_dockerFailure_returns500() throws Exception {
        doThrow(new RuntimeException("Docker daemon not responding"))
                .when(labSessionService).stopSession("student123");

        mockMvc.perform(post("/api/teacher/stop/student123"))
                .andExpect(status().isInternalServerError());

        verify(labSessionService).stopSession("student123");
    }

    @Test
    @DisplayName("POST /api/teacher/stop/{studentId} with special characters in studentId works correctly")
    void stopStudent_specialCharactersInStudentId_callsServiceAndReturns200() throws Exception {
        mockMvc.perform(post("/api/teacher/stop/student-123_test"))
                .andExpect(status().isOk());

        verify(labSessionService).stopSession("student-123_test");
    }

    @Test
    @DisplayName("POST /api/teacher/stop/{studentId} with numeric studentId works correctly")
    void stopStudent_numericStudentId_callsServiceAndReturns200() throws Exception {
        mockMvc.perform(post("/api/teacher/stop/12345"))
                .andExpect(status().isOk());

        verify(labSessionService).stopSession("12345");
    }

    @Test
    @DisplayName("POST /api/teacher/stop/{studentId} with invalid characters returns 500")
    void stopStudent_invalidCharacters_returns500() throws Exception {
        doThrow(new IllegalArgumentException("studentId must start with alphanumeric and contain only alphanumeric, underscore, or hyphen characters"))
                .when(labSessionService).stopSession("student@email");

        mockMvc.perform(post("/api/teacher/stop/student@email"))
                .andExpect(status().isInternalServerError());

        verify(labSessionService).stopSession("student@email");
    }

    @Test
    @DisplayName("POST /api/teacher/stop/{studentId} with path traversal attempt returns 500")
    void stopStudent_pathTraversalAttempt_returns500() throws Exception {
        doThrow(new IllegalArgumentException("studentId must start with alphanumeric and contain only alphanumeric, underscore, or hyphen characters"))
                .when(labSessionService).stopSession("..%2F..%2Fetc");

        mockMvc.perform(post("/api/teacher/stop/..%2F..%2Fetc"))
                .andExpect(status().isInternalServerError());

        verify(labSessionService).stopSession("..%2F..%2Fetc");
    }

    @Test
    @DisplayName("POST /api/teacher/stop/{studentId} with studentId exceeding max length returns 500")
    void stopStudent_exceedsMaxLength_returns500() throws Exception {
        String longStudentId = "a".repeat(65);

        doThrow(new IllegalArgumentException("studentId must not exceed 64 characters"))
                .when(labSessionService).stopSession(longStudentId);

        mockMvc.perform(post("/api/teacher/stop/" + longStudentId))
                .andExpect(status().isInternalServerError());

        verify(labSessionService).stopSession(longStudentId);
    }
}
