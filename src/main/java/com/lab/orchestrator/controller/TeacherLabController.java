package com.lab.orchestrator.controller;

import com.lab.orchestrator.dto.LabInitializationRequest;
import com.lab.orchestrator.service.CoreAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherLabController {

    private final CoreAllocationService coreAllocationService;

    @PostMapping("/initialize")
    public void initialize(@RequestBody LabInitializationRequest request) {
        coreAllocationService.initializeCores(
                request.getTotalStudents(),
                request.getCoreNumbers(),
                request.getImageName());
    }
}
