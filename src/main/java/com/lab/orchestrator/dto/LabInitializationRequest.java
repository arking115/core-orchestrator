package com.lab.orchestrator.dto;

import java.util.List;
import lombok.Data;

@Data
public class LabInitializationRequest {

    private int totalStudents;
    private List<Integer> coreNumbers;
    private String imageName;
}
