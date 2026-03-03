package com.lab.orchestrator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_session")
public class LabSession {

    @Id
    @Column(name = "student_id", nullable = false, unique = true)
    private String studentId;

    @Column(name = "assigned_port", nullable = false)
    private Integer assignedPort;

    @Column(name = "assigned_core", nullable = false)
    private Integer assignedCore;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    public LabSession() {
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Integer getAssignedPort() {
        return assignedPort;
    }

    public void setAssignedPort(Integer assignedPort) {
        this.assignedPort = assignedPort;
    }

    public Integer getAssignedCore() {
        return assignedCore;
    }

    public void setAssignedCore(Integer assignedCore) {
        this.assignedCore = assignedCore;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}

