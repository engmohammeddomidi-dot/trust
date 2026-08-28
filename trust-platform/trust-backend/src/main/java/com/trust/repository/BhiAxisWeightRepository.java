package com.trust.repository;

import com.trust.domain.BhiAxisWeight;
import com.trust.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BhiAxisWeightRepository extends JpaRepository<BhiAxisWeight, Long> {
    List<BhiAxisWeight> findByCategory(Category category);
}
