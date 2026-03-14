package com.lab.orchestrator.controller;

import com.lab.orchestrator.dto.LabInitializationRequest;
import com.lab.orchestrator.dto.StopSessionsResult;
import com.lab.orchestrator.service.CoreAllocationService;
import com.lab.orchestrator.service.LabSessionService;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/stop/{studentId}")
    public void stopStudent(@PathVariable String studentId) {
        labSessionService.stopSession(studentId);
    }
}
