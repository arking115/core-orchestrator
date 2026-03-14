package com.lab.orchestrator.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StopSessionsResult {
    private final int totalSessions;
    private final int successfullyStoppedCount;
    private final List<String> failedStudentIds;

    public boolean isAllSuccessful() {
        return failedStudentIds.isEmpty();
    }
}
