package com.keystone.repository;

import com.keystone.domain.PartUsage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartUsageRepository extends JpaRepository<PartUsage, Long> {
    @EntityGraph(attributePaths = {"part", "usedBy"})
    List<PartUsage> findByWorkOrderIdOrderByUsedAtAsc(Long workOrderId);
}
