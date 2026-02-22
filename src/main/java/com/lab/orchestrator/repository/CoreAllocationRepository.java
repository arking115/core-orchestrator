package com.lab.orchestrator.repository;

import com.lab.orchestrator.model.CoreAllocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CoreAllocationRepository extends JpaRepository<CoreAllocation, Long> {

    Optional<CoreAllocation> findByCoreNumber(Integer coreNumber);

    List<CoreAllocation> findAllByOrderByCurrentStudentCountAsc();

    @Query("SELECT SUM(c.currentStudentCount) FROM CoreAllocation c")
    Integer getTotalStudentCount();
}