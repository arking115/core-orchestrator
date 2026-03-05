package com.lab.orchestrator.controller;

import com.lab.orchestrator.dto.LabStartRequest;
import com.lab.orchestrator.model.LabSession;
import com.lab.orchestrator.service.LabSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student")
public class StudentLabController {

    private final LabSessionService labSessionService;

    @PostMapping("/start")
    public LabSession start(@RequestBody LabStartRequest request) {
        return labSessionService.startSession(request.getStudentId());
    }
}
