package com.keystone.service;

import com.keystone.domain.Priority;
import com.keystone.domain.WorkOrderStatus;

import java.util.List;

public record WorkOrderFilter(List<WorkOrderStatus> statuses, Priority priority, Long customerId, Long siteId,
                              Long technicianId, String q, boolean overdue, boolean unassigned) {
    public static WorkOrderFilter ofStatuses(java.util.Collection<WorkOrderStatus> statuses) {
        return new WorkOrderFilter(List.copyOf(statuses), null, null, null, null, null, false, false);
    }
}
