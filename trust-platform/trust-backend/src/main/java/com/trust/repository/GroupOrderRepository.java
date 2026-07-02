package com.trust.repository;

import com.trust.domain.GroupOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupOrderRepository extends JpaRepository<GroupOrder, Long> {
    List<GroupOrder> findByStatusOrderByCreatedAtDesc(GroupOrder.Status status);
    List<GroupOrder> findAllByOrderByCreatedAtDesc();
}
