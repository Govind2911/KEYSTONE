package com.keystone.dto;

import com.keystone.domain.WorkOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ReportDtos {
    private ReportDtos() {}

    public record SlaCompliance(long met, long total, Double percent) {}

    public record TechnicianLoad(Long technicianId, String name, long open, long completed, long overdue) {}

    public record SiteLoad(Long siteId, String name, long total, long open, long overdue) {}

    public record Filters(Long customerId, Long siteId, Long technicianId, Integer days) {}

    public record Summary(
            Filters filters,
            long total, long open, long overdue, long atRisk, long breached,
            Map<WorkOrderStatus, Long> statusCounts,
            SlaCompliance slaCompliance,
            List<TechnicianLoad> byTechnician,
            List<SiteLoad> bySite,
            List<WorkOrderDtos.WorkOrderDto> overdueOrders,
            Instant generatedAt) {}
}
