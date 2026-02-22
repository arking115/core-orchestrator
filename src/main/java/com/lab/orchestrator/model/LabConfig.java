package com.lab.orchestrator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Single lab configuration. Only one row is used at a time, with id = 1L.
 */
@Entity
@Table(name = "lab_config")
public class LabConfig {

    @Id
    private Long id = 1L;

    @Column(name = "max_students", nullable = false)
    private Integer maxStudents;

    public LabConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }
}
