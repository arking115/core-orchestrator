package com.lab.orchestrator.controller;

import com.lab.orchestrator.dto.LabInitializationRequest;
import com.lab.orchestrator.dto.ServerCapacityResponse;
import com.lab.orchestrator.dto.StopSessionsResult;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.service.CoreAllocationService;
import com.lab.orchestrator.service.LabSessionService;
import com.lab.orchestrator.service.ServerMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherLabController {

    private final CoreAllocationService coreAllocationService;
    private final LabSessionService labSessionService;
    private final ServerMetricsService serverMetricsService;

    @GetMapping("/server-capacity")
    public ResponseEntity<ServerCapacityResponse> getServerCapacity() {
        return ResponseEntity.ok(serverMetricsService.getServerCapacity());
    }

    @PostMapping("/initialize")
    public void initialize(@RequestBody LabInitializationRequest request) {
        labSessionService.stopAllActiveSessions();
        coreAllocationService.initializeCores(
                request.getTotalStudents(),
                request.getCoreNumbers(),
                request.getImageName());
    }

    @PostMapping("/stop-all")
    public StopSessionsResult stopAll() {
        return labSessionService.stopAllActiveSessions();
    }

    @PostMapping("/start/{studentId}")
    public LabSession startStudent(@PathVariable String studentId) {
        return labSessionService.startSession(studentId);
    }

    @PostMapping("/stop/{studentId}")
    public void stopStudent(@PathVariable String studentId) {
        labSessionService.stopSession(studentId);
    }
}
