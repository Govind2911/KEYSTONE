package com.keystone.service;

import com.keystone.domain.Role;
import com.keystone.domain.WorkOrder;
import com.keystone.exception.ApiException;
import com.keystone.repository.WorkOrderRepository;
import com.keystone.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The single answer to "may this person see this work order?". Used by every service that touches an order,
 * so ID-guessing (IDOR) is blocked everywhere. Invisible orders look exactly like missing ones (404).
 */
@Component
@RequiredArgsConstructor
public class WorkOrderAccess {
    private final WorkOrderRepository workOrders;

    public boolean canView(WorkOrder w, AppUserPrincipal me) {
        return switch (me.role()) {
            case MANAGER, DISPATCHER -> true;
            case TECHNICIAN -> isAssignee(w, me);
            case CUSTOMER -> me.customerId() != null && me.customerId().equals(w.getCustomer().getId());
        };
    }

    public WorkOrder loadVisible(Long id, AppUserPrincipal me) {
        WorkOrder w = workOrders.findById(id).orElseThrow(() -> new ApiException.NotFound("Work order not found"));
        if (!canView(w, me)) {
            throw new ApiException.NotFound("Work order not found");
        }
        return w;
    }

    public boolean isAssignee(WorkOrder w, AppUserPrincipal me) {
        return w.getAssignedTo() != null && w.getAssignedTo().getId().equals(me.id());
    }

    public boolean isStaff(AppUserPrincipal me) {
        return me.role() != Role.CUSTOMER;
    }
}
