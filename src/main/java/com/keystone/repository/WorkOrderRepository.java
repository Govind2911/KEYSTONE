package com.keystone.repository;

import com.keystone.domain.WorkOrder;
import com.keystone.domain.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {

    /** Overrides the inherited method only to attach an entity graph: avoids N+1 on list screens. */
    @Override
    @EntityGraph(attributePaths = {"customer", "site", "assignedTo", "createdBy"})
    Page<WorkOrder> findAll(Specification<WorkOrder> spec, Pageable pageable);

    List<WorkOrder> findByStatusIn(Collection<WorkOrderStatus> statuses);

    /** Open job count per technician, used by dispatch to spot who is overloaded. */
    @Query("select w.assignedTo.id, count(w) from WorkOrder w "
            + "where w.assignedTo is not null and w.status in :statuses group by w.assignedTo.id")
    List<Object[]> countOpenByTechnician(@Param("statuses") Collection<WorkOrderStatus> statuses);
}
