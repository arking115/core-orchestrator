package com.lab.orchestrator.controller;

import com.lab.orchestrator.dto.ActiveLabSessionResponse;
import com.lab.orchestrator.dto.LabInitializationRequest;
import com.lab.orchestrator.dto.ServerCapacityResponse;
import com.lab.orchestrator.dto.StopSessionsResult;
import com.lab.orchestrator.dto.TeacherStudentResponse;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.model.Role;
import com.lab.orchestrator.repository.UserRepository;
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

import java.util.List;
import java.util.Comparator;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherLabController {

    private final CoreAllocationService coreAllocationService;
    private final LabSessionService labSessionService;
    private final ServerMetricsService serverMetricsService;
    private final UserRepository userRepository;

    @GetMapping("/server-capacity")
    public ResponseEntity<ServerCapacityResponse> getServerCapacity() {
        return ResponseEntity.ok(serverMetricsService.getServerCapacity());
    }

    @GetMapping("/sessions")
    public List<ActiveLabSessionResponse> listActiveSessions() {
        return labSessionService.listActiveSessions();
    }

    @GetMapping("/students")
    public List<TeacherStudentResponse> listStudents() {
        return userRepository.findAllByRole(Role.ROLE_STUDENT).stream()
                .map(
                        user ->
                                new TeacherStudentResponse(
                                        user.getStudentId(),
                                        user.getStudentId()))
                .filter(dto -> dto.studentId() != null && !dto.studentId().isBlank())
                .sorted(Comparator.comparing(TeacherStudentResponse::studentId))
                .toList();
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
