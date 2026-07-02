package com.trust.repository;

import com.trust.domain.GroupOrderParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupOrderParticipantRepository extends JpaRepository<GroupOrderParticipant, Long> {
    List<GroupOrderParticipant> findByGroupOrderId(Long groupOrderId);
    List<GroupOrderParticipant> findByOrganizationIdOrderByIdDesc(Long organizationId);
}
