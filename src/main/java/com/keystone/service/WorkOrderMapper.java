package com.keystone.service;

import com.keystone.domain.Role;
import com.keystone.domain.WorkOrder;
import com.keystone.domain.WorkOrderStatusHistory;
import com.keystone.dto.WorkOrderDtos.HistoryDto;
import com.keystone.dto.WorkOrderDtos.Ref;
import com.keystone.dto.WorkOrderDtos.WorkOrderDto;
import com.keystone.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Entities never leave the service layer: everything is mapped here. For CUSTOMER viewers internal fields
 * (assignee, SLA, costs, actors, notes) are simply not populated, so they are absent from the JSON.
 */
@Component
@RequiredArgsConstructor
public class WorkOrderMapper {
    private final SlaService sla;
    private final WorkOrderAccess access;

    /** List / board shape: no history, no costs. */
    public WorkOrderDto toListDto(WorkOrder w, AppUserPrincipal viewer) {
        return build(w, viewer, null, null, null);
    }

    public WorkOrderDto toDetailDto(WorkOrder w, AppUserPrincipal viewer, List<WorkOrderStatusHistory> history,
                                    BigDecimal partsCost, Integer labourMinutes) {
        boolean internal = viewer.role() != Role.CUSTOMER;
        List<HistoryDto> h = history.stream().map(x -> toHistoryDto(x, internal)).toList();
        return build(w, viewer, h, internal ? partsCost : null, internal ? labourMinutes : null);
    }

    public HistoryDto toHistoryDto(WorkOrderStatusHistory h, boolean internal) {
        return new HistoryDto(h.getId(), h.getFromStatus(), h.getToStatus(),
                internal ? h.getChangedBy().getName() : null, h.getChangedAt(), internal ? h.getNote() : null);
    }

    private WorkOrderDto build(WorkOrder w, AppUserPrincipal viewer, List<HistoryDto> history,
                               BigDecimal partsCost, Integer labourMinutes) {
        boolean internal = viewer.role() != Role.CUSTOMER;
        Instant now = Instant.now();
        boolean dispatchOrManager = viewer.role().isDispatchOrManager();

        Ref assignee = internal && w.getAssignedTo() != null ? new Ref(w.getAssignedTo().getId(), w.getAssignedTo().getName()) : null;
        Ref createdBy = internal ? new Ref(w.getCreatedBy().getId(), w.getCreatedBy().getName()) : null;
        String siteAddress = w.getSite().getAddressLine() + ", " + w.getSite().getCity();

        return new WorkOrderDto(
                w.getId(), w.getCode(), w.getTitle(), w.getDescription(), w.getPriority(), w.getStatus(), w.getCategory(),
                new Ref(w.getCustomer().getId(), w.getCustomer().getName()),
                new Ref(w.getSite().getId(), w.getSite().getName()), siteAddress,
                assignee, createdBy,
                w.getCreatedAt(), w.getUpdatedAt(), w.getScheduledAt(),
                w.getStartedAt(), w.getCompletedAt(), w.getClosedAt(),
                internal ? w.getSlaDueAt() : null,
                internal ? sla.statusOf(w, now) : null,
                internal ? sla.percentElapsed(w, now) : null,
                internal ? w.isSlaBreached() : null,
                partsCost, labourMinutes, history,
                internal ? WorkOrderStateMachine.allowedTargets(w.getStatus(), viewer.role(), access.isAssignee(w, viewer)) : null,
                internal ? dispatchOrManager && w.getStatus().isActive() : null,
                internal ? dispatchOrManager && !w.getStatus().isTerminal() : null);
    }
}
