package com.keystone.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keystone.domain.Category;
import com.keystone.domain.Priority;
import com.keystone.domain.SlaStatus;
import com.keystone.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class WorkOrderDtos {
    private WorkOrderDtos() {}

    public record CreateWorkOrderRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 3000) String description,
            @NotNull Priority priority,
            Long customerId,                  // required for staff; ignored for customers (taken from their account)
            @NotNull Long siteId,
            Instant scheduledAt,
            Category category) {}

    public record UpdateWorkOrderRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 3000) String description,
            @NotNull Priority priority,
            Instant scheduledAt,
            Category category) {}

    public record AssignRequest(@NotNull Long technicianId, @Size(max = 500) String note) {}

    public record StatusChangeRequest(@NotNull WorkOrderStatus toStatus, @Size(max = 500) String note) {}

    public record Ref(Long id, String name) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HistoryDto(Long id, WorkOrderStatus fromStatus, WorkOrderStatus toStatus,
                             String changedBy, Instant changedAt, String note) {}

    /**
     * One shape for list, board and detail. Fields that are internal (assignee, SLA, costs, notes, actors)
     * are left null for CUSTOMER viewers and therefore omitted from the JSON entirely.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkOrderDto(
            Long id, String code, String title, String description,
            Priority priority, WorkOrderStatus status, Category category,
            Ref customer, Ref site, String siteAddress,
            Ref assignee, Ref createdBy,
            Instant createdAt, Instant updatedAt, Instant scheduledAt,
            Instant startedAt, Instant completedAt, Instant closedAt,
            Instant slaDueAt, SlaStatus slaStatus, Integer slaPercentElapsed, Boolean slaBreached,
            BigDecimal partsCost, Integer labourMinutes,
            List<HistoryDto> history,
            List<WorkOrderStatus> allowedTransitions, Boolean canAssign, Boolean canEdit) {}
}
