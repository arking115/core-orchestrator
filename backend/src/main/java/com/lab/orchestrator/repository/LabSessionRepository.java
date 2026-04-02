package com.lab.orchestrator.repository;

import com.lab.orchestrator.model.LabSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LabSessionRepository extends JpaRepository<LabSession, String> {

    @Query("SELECT ls.assignedPort FROM LabSession ls WHERE ls.assignedPort IS NOT NULL")
    List<Integer> findAllAssignedPorts();
}

