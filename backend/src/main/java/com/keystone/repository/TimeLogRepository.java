package com.keystone.repository;

import com.keystone.domain.TimeLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {
    @EntityGraph(attributePaths = {"technician"})
    List<TimeLog> findByWorkOrderIdOrderByLoggedAtAsc(Long workOrderId);
}
