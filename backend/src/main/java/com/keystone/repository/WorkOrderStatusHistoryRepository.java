package com.keystone.repository;

import com.keystone.domain.WorkOrderStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderStatusHistoryRepository extends JpaRepository<WorkOrderStatusHistory, Long> {
    @EntityGraph(attributePaths = {"changedBy"})
    List<WorkOrderStatusHistory> findByWorkOrderIdOrderByChangedAtAscIdAsc(Long workOrderId);

    @EntityGraph(attributePaths = {"workOrder", "changedBy"})
    Page<WorkOrderStatusHistory> findAllByOrderByChangedAtDescIdDesc(Pageable pageable);
}
