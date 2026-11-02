package com.keystone.service;

import com.keystone.domain.WorkOrder;
import com.keystone.domain.WorkOrderStatus;
import com.keystone.security.AppUserPrincipal;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Query building. visibleTo() is the data-scoping rule: customers only see their own, technicians only their jobs. */
public final class WorkOrderSpecs {
    private WorkOrderSpecs() {}

    public static Specification<WorkOrder> visibleTo(AppUserPrincipal me) {
        return (root, query, cb) -> switch (me.role()) {
            case CUSTOMER -> cb.equal(root.get("customer").get("id"), me.customerId());
            case TECHNICIAN -> cb.equal(root.get("assignedTo").get("id"), me.id());
            case MANAGER, DISPATCHER -> cb.conjunction();
        };
    }

    public static Specification<WorkOrder> matching(WorkOrderFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.statuses() != null && !f.statuses().isEmpty()) p.add(root.get("status").in(f.statuses()));
            if (f.priority() != null) p.add(cb.equal(root.get("priority"), f.priority()));
            if (f.customerId() != null) p.add(cb.equal(root.get("customer").get("id"), f.customerId()));
            if (f.siteId() != null) p.add(cb.equal(root.get("site").get("id"), f.siteId()));
            if (f.technicianId() != null) p.add(cb.equal(root.get("assignedTo").get("id"), f.technicianId()));
            if (f.unassigned()) p.add(cb.isNull(root.get("assignedTo")));
            if (f.overdue()) {
                p.add(root.get("status").in(WorkOrderStatus.ACTIVE));
                p.add(cb.lessThan(root.<Instant>get("slaDueAt"), Instant.now()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase() + "%";
                Join<Object, Object> customer = root.join("customer", JoinType.LEFT);
                p.add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(customer.get("name")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
