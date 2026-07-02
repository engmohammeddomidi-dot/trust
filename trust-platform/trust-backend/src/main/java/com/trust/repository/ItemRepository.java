package com.trust.repository;

import com.trust.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByBranchId(Long branchId);
    List<Item> findByBranchIdIn(List<Long> branchIds);
    List<Item> findByBranchIdAndMovementStatus(Long branchId, Item.MovementStatus status);
}
