package com.trust.repository;

import com.trust.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByOrganizationId(Long organizationId);
    Optional<Goal> findByOrganizationIdAndType(Long organizationId, Goal.Type type);
}
