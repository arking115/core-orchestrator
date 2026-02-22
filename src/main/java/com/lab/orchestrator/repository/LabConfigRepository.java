package com.lab.orchestrator.repository;

import com.lab.orchestrator.model.LabConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabConfigRepository extends JpaRepository<LabConfig, Long> {
}
