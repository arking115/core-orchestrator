package com.lab.orchestrator.repository;

import com.lab.orchestrator.model.Role;
import com.lab.orchestrator.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByStudentId(String studentId);

    List<User> findAllByRole(Role role);
}

