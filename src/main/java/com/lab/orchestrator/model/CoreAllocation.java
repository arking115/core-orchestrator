package com.lab.orchestrator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "core_allocation")
public class CoreAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "core_number", nullable = false)
    private Integer coreNumber;

    @Column(name = "current_student_count", nullable = false)
    private Integer currentStudentCount;

    @Column(name = "cpu_limit", nullable = false)
    private Double cpuLimit;

    public CoreAllocation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCoreNumber() {
        return coreNumber;
    }

    public void setCoreNumber(Integer coreNumber) {
        this.coreNumber = coreNumber;
    }

    public Integer getCurrentStudentCount() {
        return currentStudentCount;
    }

    public void setCurrentStudentCount(Integer currentStudentCount) {
        this.currentStudentCount = currentStudentCount;
    }

    public Double getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(Double cpuLimit) {
        this.cpuLimit = cpuLimit;
    }
}
