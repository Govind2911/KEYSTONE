package com.keystone.service;

import com.keystone.domain.*;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.dto.WorkOrderDtos.*;
import com.keystone.exception.ApiException;
import com.keystone.repository.*;
import com.keystone.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);
    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "slaDueAt", "status", "code", "title");

    private final WorkOrderRepository workOrders;
    private final WorkOrderStatusHistoryRepository historyRepo;
    private final CustomerRepository customers;
    private final SiteRepository sites;
    private final UserRepository users;
    private final PartUsageRepository usages;
    private final TimeLogRepository timeLogs;
    private final WorkOrderMapper mapper;
    private final WorkOrderAccess access;
    private final SlaService sla;
    private final NotificationService notifications;

    // ---------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderDto> search(WorkOrderFilter filter, Pageable pageable, AppUserPrincipal me) {
        Specification<WorkOrder> spec = WorkOrderSpecs.visibleTo(me).and(WorkOrderSpecs.matching(filter));
        Page<WorkOrder> page = workOrders.findAll(spec, safePageable(pageable));
        return PageResponse.of(page, w -> mapper.toListDto(w, me));
    }

    /** Kanban board: all open work (plus completed-awaiting-sign-off), grouped by status, role-scoped. */
    @Transactional(readOnly = true)
    public Map<WorkOrderStatus, List<WorkOrderDto>> board(AppUserPrincipal me) {
        Specification<WorkOrder> spec = WorkOrderSpecs.visibleTo(me)
                .and(WorkOrderSpecs.matching(WorkOrderFilter.ofStatuses(WorkOrderStatus.BOARD)));
        Page<WorkOrder> page = workOrders.findAll(spec, PageRequest.of(0, 300, Sort.by("slaDueAt").ascending()));
        Map<WorkOrderStatus, List<WorkOrderDto>> columns = new LinkedHashMap<>();
        for (WorkOrderStatus s : WorkOrderStatus.BOARD) columns.put(s, new ArrayList<>());
        for (WorkOrder w : page.getContent()) columns.get(w.getStatus()).add(mapper.toListDto(w, me));
        return columns;
    }

    @Transactional(readOnly = true)
    public WorkOrderDto get(Long id, AppUserPrincipal me) {
        return detail(access.loadVisible(id, me), me);
    }

    @Transactional(readOnly = true)
    public List<HistoryDto> history(Long id, AppUserPrincipal me) {
        WorkOrder w = access.loadVisible(id, me);
        boolean internal = access.isStaff(me);
        return historyRepo.findByWorkOrderIdOrderByChangedAtAscIdAsc(w.getId()).stream()
                .map(h -> mapper.toHistoryDto(h, internal)).toList();
    }

    // --------------------------------------------------------------- writes

    @Transactional
    public WorkOrderDto create(CreateWorkOrderRequest req, AppUserPrincipal me) {
        Customer customer;
        Site site;
        if (me.role() == Role.CUSTOMER) {
            customer = customers.findById(me.customerId()).orElseThrow(() -> new ApiException.NotFound("Customer not found"));
            site = sites.findById(req.siteId()).orElseThrow(() -> new ApiException.NotFound("Site not found"));
            if (!site.getCustomer().getId().equals(customer.getId())) {
                throw new ApiException.NotFound("Site not found");   // never confirm another customer's site exists
            }
            if (req.priority() == Priority.URGENT) {
                throw new ApiException.BadRequest("Urgent requests must be phoned in to dispatch. Choose High or lower.");
            }
        } else if (me.role().isDispatchOrManager()) {
            if (req.customerId() == null) throw new ApiException.BadRequest("customerId is required");
            customer = customers.findById(req.customerId()).orElseThrow(() -> new ApiException.BadRequest("Customer not found"));
            site = sites.findById(req.siteId()).orElseThrow(() -> new ApiException.BadRequest("Site not found"));
            if (!site.getCustomer().getId().equals(customer.getId())) {
                throw new ApiException.BadRequest("Selected site does not belong to this customer");
            }
        } else {
            throw new ApiException.Forbidden("You are not allowed to create work orders");
        }
        if (!customer.isActive()) throw new ApiException.BadRequest("This customer is inactive");

        Instant now = Instant.now();
        UserAccount creator = users.getReferenceById(me.id());
        WorkOrder w = new WorkOrder();
        w.setCode("TMP-" + UUID.randomUUID());                  // replaced with WO-nnnnnn from the generated id below
        w.setTitle(req.title().trim());
        w.setDescription(req.description().trim());
        w.setPriority(req.priority());
        w.setCategory(req.category() == null ? Category.GENERAL : req.category());
        w.setStatus(WorkOrderStatus.NEW);
        w.setCustomer(customer);
        w.setSite(site);
        w.setCreatedBy(creator);
        w.setScheduledAt(req.scheduledAt());
        w.setCreatedAt(now);
        w.setUpdatedAt(now);
        w.setSlaDueAt(sla.dueFor(req.priority(), now));
        w = workOrders.save(w);
        w.setCode(String.format("WO-%06d", w.getId()));

        record(w, null, WorkOrderStatus.NEW, creator,
                me.role() == Role.CUSTOMER ? "Request raised by customer" : "Work order created");
        log.info("Work order {} created by {} ({})", w.getCode(), me.email(), me.role());

        if (me.role() == Role.CUSTOMER) {
            notifications.notifyRoles(EnumSet.of(Role.DISPATCHER, Role.MANAGER), NotificationType.NEW_REQUEST,
                    "New customer request " + w.getCode() + ": " + w.getTitle(), w.getId());
        }
        return detail(w, me);
    }

    @Transactional
    public WorkOrderDto update(Long id, UpdateWorkOrderRequest req, AppUserPrincipal me) {
        requireDispatchOrManager(me);
        WorkOrder w = access.loadVisible(id, me);
        if (w.getStatus().isTerminal()) {
            throw new ApiException.Conflict("A " + w.getStatus() + " work order is immutable");
        }
        w.setTitle(req.title().trim());
        w.setDescription(req.description().trim());
        w.setScheduledAt(req.scheduledAt());
        if (req.category() != null) w.setCategory(req.category());
        if (w.getPriority() != req.priority()) {
            w.setPriority(req.priority());
            sla.recalculate(w, Instant.now());
        }
        log.info("Work order {} edited by {}", w.getCode(), me.email());
        return detail(w, me);
    }

    @Transactional
    public WorkOrderDto assign(Long id, AssignRequest req, AppUserPrincipal me) {
        requireDispatchOrManager(me);
        WorkOrder w = access.loadVisible(id, me);
        if (!w.getStatus().isActive()) {
            throw new ApiException.Conflict("Cannot assign a work order that is " + w.getStatus());
        }
        UserAccount tech = users.findById(req.technicianId())
                .orElseThrow(() -> new ApiException.BadRequest("Technician not found"));
        if (tech.getRole() != Role.TECHNICIAN || !tech.isActive()) {
            throw new ApiException.BadRequest("Selected user is not an active technician");
        }
        UserAccount previous = w.getAssignedTo();
        if (previous != null && previous.getId().equals(tech.getId())) {
            throw new ApiException.BadRequest("Already assigned to " + tech.getName());
        }

        WorkOrderStatus from = w.getStatus();
        if (from == WorkOrderStatus.NEW) {
            WorkOrderStateMachine.check(WorkOrderStatus.NEW, WorkOrderStatus.ASSIGNED, me.role(), false);
            w.setStatus(WorkOrderStatus.ASSIGNED);
        }
        w.setAssignedTo(tech);

        String note = previous == null ? "Assigned to " + tech.getName()
                : "Reassigned from " + previous.getName() + " to " + tech.getName();
        if (req.note() != null && !req.note().isBlank()) note += ". " + req.note().trim();
        record(w, from, w.getStatus(), users.getReferenceById(me.id()), note);
        log.info("Work order {} assigned to {} by {}", w.getCode(), tech.getEmail(), me.email());

        notifications.notifyUser(tech.getId(), NotificationType.ASSIGNED,
                "You have been assigned " + w.getCode() + ": " + w.getTitle(), w.getId());
        if (previous != null) {
            notifications.notifyUser(previous.getId(), NotificationType.STATUS_CHANGED,
                    w.getCode() + " was reassigned to " + tech.getName(), w.getId());
        } else {
            notifications.notifyCustomerUsers(w.getCustomer().getId(), NotificationType.STATUS_CHANGED,
                    w.getCode() + " has been assigned to a technician", w.getId());
        }
        return detail(w, me);
    }

    @Transactional
    public WorkOrderDto changeStatus(Long id, StatusChangeRequest req, AppUserPrincipal me) {
        WorkOrder w = access.loadVisible(id, me);
        WorkOrderStatus from = w.getStatus();
        WorkOrderStatus to = req.toStatus();
        if (to == WorkOrderStatus.ASSIGNED) {
            throw new ApiException.BadRequest("Use the assign endpoint to assign a technician");
        }
        // The lifecycle and who-may-do-what live in one place, enforced here regardless of what the UI shows.
        WorkOrderStateMachine.check(from, to, me.role(), access.isAssignee(w, me));

        Instant now = Instant.now();
        w.setStatus(to);
        switch (to) {
            case IN_PROGRESS -> {
                if (w.getStartedAt() == null) w.setStartedAt(now);
                if (from == WorkOrderStatus.COMPLETED) w.setCompletedAt(null);   // reopened
            }
            case COMPLETED -> {
                w.setCompletedAt(now);
                sla.flagIfLate(w, now);
            }
            case CLOSED -> w.setClosedAt(now);
            default -> { }
        }
        record(w, from, to, users.getReferenceById(me.id()), req.note());
        log.info("Work order {} {} -> {} by {} ({})", w.getCode(), from, to, me.email(), me.role());

        String msg = w.getCode() + " is now " + to;
        if (to == WorkOrderStatus.COMPLETED) {
            notifications.notifyRoles(EnumSet.of(Role.MANAGER), NotificationType.STATUS_CHANGED,
                    w.getCode() + " is complete and awaiting sign-off", w.getId());
        } else if (w.getAssignedTo() != null && !w.getAssignedTo().getId().equals(me.id())) {
            notifications.notifyUser(w.getAssignedTo().getId(), NotificationType.STATUS_CHANGED, msg, w.getId());
        }
        if (to != WorkOrderStatus.ON_HOLD) {
            notifications.notifyCustomerUsers(w.getCustomer().getId(), NotificationType.STATUS_CHANGED, msg, w.getId());
        }
        return detail(w, me);
    }

    // -------------------------------------------------------------- helpers

    private WorkOrderDto detail(WorkOrder w, AppUserPrincipal me) {
        List<WorkOrderStatusHistory> history = historyRepo.findByWorkOrderIdOrderByChangedAtAscIdAsc(w.getId());
        BigDecimal partsCost = null;
        Integer minutes = null;
        if (access.isStaff(me)) {
            partsCost = usages.findByWorkOrderIdOrderByUsedAtAsc(w.getId()).stream()
                    .map(u -> u.getUnitCost().multiply(BigDecimal.valueOf(u.getQtyUsed())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            minutes = timeLogs.findByWorkOrderIdOrderByLoggedAtAsc(w.getId()).stream().mapToInt(TimeLog::getMinutes).sum();
        }
        return mapper.toDetailDto(w, me, history, partsCost, minutes);
    }

    private void record(WorkOrder w, WorkOrderStatus from, WorkOrderStatus to, UserAccount actor, String note) {
        WorkOrderStatusHistory h = new WorkOrderStatusHistory();
        h.setWorkOrder(w);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedBy(actor);
        h.setNote(note == null || note.isBlank() ? null : note.trim());
        historyRepo.save(h);
    }

    private void requireDispatchOrManager(AppUserPrincipal me) {
        if (!me.role().isDispatchOrManager()) {
            throw new ApiException.Forbidden("Only a dispatcher or manager can do this");
        }
    }

    /** Whitelists sortable columns so a crafted ?sort= can never reach an unexpected property. */
    private Pageable safePageable(Pageable p) {
        List<Sort.Order> orders = p.getSort().stream().filter(o -> SORTABLE.contains(o.getProperty())).toList();
        Sort sort = orders.isEmpty() ? Sort.by(Sort.Order.desc("createdAt")) : Sort.by(orders);
        return PageRequest.of(p.getPageNumber(), p.getPageSize(), sort);
    }
}
