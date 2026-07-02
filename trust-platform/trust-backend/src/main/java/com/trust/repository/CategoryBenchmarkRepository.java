package com.trust.repository;

import com.trust.domain.Category;
import com.trust.domain.CategoryBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryBenchmarkRepository extends JpaRepository<CategoryBenchmark, Category> {
}
