package com.lab.orchestrator.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lab.orchestrator.service.CoreAllocationService;
import com.lab.orchestrator.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
}
