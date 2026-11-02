package com.keystone.controller;

import com.keystone.domain.Priority;
import com.keystone.domain.WorkOrderStatus;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.dto.WorkOrderDtos.*;
import com.keystone.security.AppUserPrincipal;
import com.keystone.service.WorkOrderFilter;
import com.keystone.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@Tag(name = "Work orders")
public class WorkOrderController {
    private final WorkOrderService service;

    @GetMapping
    @Operation(summary = "List work orders",
            description = "Paginated, filterable, sortable (sort=createdAt|updatedAt|slaDueAt|status|code|title). "
                    + "Results are scoped by role: customers see only their organisation's orders, technicians only their assigned jobs.")
    public PageResponse<WorkOrderDto> list(
            @RequestParam(required = false) List<WorkOrderStatus> status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(defaultValue = "false") boolean unassigned,
            Pageable pageable, @AuthenticationPrincipal AppUserPrincipal me) {
        return service.search(new WorkOrderFilter(status, priority, customerId, siteId, technicianId, q, overdue, unassigned), pageable, me);
    }

    @GetMapping("/board")
    @Operation(summary = "Kanban board", description = "Open work grouped by status (NEW, ASSIGNED, IN_PROGRESS, ON_HOLD, COMPLETED), soonest SLA first.")
    public Map<WorkOrderStatus, List<WorkOrderDto>> board(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.board(me);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    @Operation(summary = "Create a work order / raise a request",
            description = "Staff supply customerId + siteId. A CUSTOMER supplies only siteId (their own); the customer comes from their account. Customers cannot request URGENT.")
    public WorkOrderDto create(@Valid @RequestBody CreateWorkOrderRequest req, @AuthenticationPrincipal AppUserPrincipal me) {
        return service.create(req, me);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a work order with its history", description = "404 if the caller is not allowed to see it.")
    public WorkOrderDto get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        return service.get(id, me);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Operation(summary = "Edit a work order", description = "Allowed until the order is CLOSED or CANCELLED (409 afterwards).")
    public WorkOrderDto update(@PathVariable Long id, @Valid @RequestBody UpdateWorkOrderRequest req,
                               @AuthenticationPrincipal AppUserPrincipal me) {
        return service.update(id, req, me);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Operation(summary = "Assign or reassign a technician",
            description = "NEW -> ASSIGNED on first assignment; reassignment is allowed while the order is open. Notifies the technician.")
    public WorkOrderDto assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req,
                               @AuthenticationPrincipal AppUserPrincipal me) {
        return service.assign(id, req, me);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DISPATCHER','TECHNICIAN','MANAGER')")
    @Operation(summary = "Transition the lifecycle",
            description = "Validated against the lifecycle: an illegal jump returns 409; a role/assignee not allowed to make that move returns 403. Every change writes a status-history row.")
    public WorkOrderDto changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest req,
                                     @AuthenticationPrincipal AppUserPrincipal me) {
        return service.changeStatus(id, req, me);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Status history (audit trail)", description = "Append-only. Customers see transitions only, without actors or internal notes.")
    public List<HistoryDto> history(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        return service.history(id, me);
    }
}
