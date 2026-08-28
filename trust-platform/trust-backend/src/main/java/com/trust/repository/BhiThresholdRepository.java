package com.trust.repository;

import com.trust.domain.BhiThreshold;
import com.trust.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BhiThresholdRepository extends JpaRepository<BhiThreshold, Long> {
    List<BhiThreshold> findByCategory(Category category);
}
