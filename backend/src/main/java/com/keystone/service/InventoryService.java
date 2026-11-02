package com.keystone.service;

import com.keystone.domain.*;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.dto.PartDtos.*;
import com.keystone.exception.ApiException;
import com.keystone.repository.*;
import com.keystone.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Parts catalogue, parts used on jobs, and technician time logs. */
@Service
@RequiredArgsConstructor
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final PartRepository parts;
    private final PartUsageRepository usages;
    private final TimeLogRepository timeLogs;
    private final UserRepository users;
    private final WorkOrderAccess access;

    // ------------------------------------------------------------ catalogue

    @Transactional(readOnly = true)
    public PageResponse<PartDto> listParts(String q, Pageable pageable) {
        Pageable p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
        var page = (q == null || q.isBlank())
                ? parts.findAll(p)
                : parts.findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(q.trim(), q.trim(), p);
        return PageResponse.of(page, this::toDto);
    }

    @Transactional
    public PartDto createPart(PartRequest req) {
        if (parts.existsBySkuIgnoreCase(req.sku().trim())) {
            throw new ApiException.Conflict("A part with SKU " + req.sku().trim() + " already exists");
        }
        Part p = new Part();
        apply(p, req);
        return toDto(parts.save(p));
    }

    @Transactional
    public PartDto updatePart(Long id, PartRequest req) {
        Part p = parts.findByIdForUpdate(id).orElseThrow(() -> new ApiException.NotFound("Part not found"));
        parts.findBySkuIgnoreCase(req.sku().trim()).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new ApiException.Conflict("A part with SKU " + req.sku().trim() + " already exists");
        });
        apply(p, req);
        return toDto(p);
    }

    @Transactional
    public PartDto restock(Long id, RestockRequest req) {
        Part p = parts.findByIdForUpdate(id).orElseThrow(() -> new ApiException.NotFound("Part not found"));
        p.setStockQty(p.getStockQty() + req.quantity());
        log.info("Part {} restocked by {} -> {}", p.getSku(), req.quantity(), p.getStockQty());
        return toDto(p);
    }

    // ----------------------------------------------------------- job parts

    /**
     * Logs parts against a job and decrements stock in ONE transaction. The part row is locked (SELECT ... FOR UPDATE)
     * so two technicians can never both take the last unit; any failure rolls back the usage row and the stock change.
     */
    @Transactional
    public PartUsageDto usePart(Long workOrderId, PartUsageRequest req, AppUserPrincipal me) {
        WorkOrder w = access.loadVisible(workOrderId, me);
        assertCanLog(w, me);
        Part part = parts.findByIdForUpdate(req.partId()).orElseThrow(() -> new ApiException.NotFound("Part not found"));
        if (part.getStockQty() < req.quantity()) {
            throw new ApiException.Conflict("Insufficient stock for " + part.getSku() + ": only " + part.getStockQty() + " available");
        }
        part.setStockQty(part.getStockQty() - req.quantity());

        PartUsage u = new PartUsage();
        u.setWorkOrder(w);
        u.setPart(part);
        u.setQtyUsed(req.quantity());
        u.setUnitCost(part.getUnitCost());
        u.setUsedBy(users.getReferenceById(me.id()));
        u.setNote(blankToNull(req.note()));
        usages.save(u);
        log.info("{} x{} used on {} by {}", part.getSku(), req.quantity(), w.getCode(), me.email());
        return toDto(u);
    }

    @Transactional(readOnly = true)
    public List<PartUsageDto> listUsage(Long workOrderId, AppUserPrincipal me) {
        WorkOrder w = loadForStaff(workOrderId, me);
        return usages.findByWorkOrderIdOrderByUsedAtAsc(w.getId()).stream().map(this::toDto).toList();
    }

    // ------------------------------------------------------------ time logs

    @Transactional
    public TimeLogDto logTime(Long workOrderId, TimeLogRequest req, AppUserPrincipal me) {
        WorkOrder w = access.loadVisible(workOrderId, me);
        assertCanLog(w, me);
        TimeLog t = new TimeLog();
        t.setWorkOrder(w);
        t.setTechnician(users.getReferenceById(me.id()));
        t.setMinutes(req.minutes());
        t.setNote(blankToNull(req.note()));
        timeLogs.save(t);
        log.info("{} min logged on {} by {}", req.minutes(), w.getCode(), me.email());
        return new TimeLogDto(t.getId(), me.id(), me.name(), t.getMinutes(), t.getNote(), t.getLoggedAt());
    }

    @Transactional(readOnly = true)
    public List<TimeLogDto> listTime(Long workOrderId, AppUserPrincipal me) {
        WorkOrder w = loadForStaff(workOrderId, me);
        return timeLogs.findByWorkOrderIdOrderByLoggedAtAsc(w.getId()).stream()
                .map(t -> new TimeLogDto(t.getId(), t.getTechnician().getId(), t.getTechnician().getName(),
                        t.getMinutes(), t.getNote(), t.getLoggedAt())).toList();
    }

    // -------------------------------------------------------------- helpers

    private WorkOrder loadForStaff(Long id, AppUserPrincipal me) {
        if (!access.isStaff(me)) throw new ApiException.Forbidden("Internal records are not available to customers");
        return access.loadVisible(id, me);
    }

    private void assertCanLog(WorkOrder w, AppUserPrincipal me) {
        boolean allowed = me.role() == Role.MANAGER || (me.role() == Role.TECHNICIAN && access.isAssignee(w, me));
        if (!allowed) throw new ApiException.Forbidden("Only the assigned technician or a manager can log work on this order");
        if (!WorkOrderStatus.LOGGABLE.contains(w.getStatus())) {
            throw new ApiException.Conflict("Parts and time can only be logged while a job is in progress, on hold or completed (this one is "
                    + w.getStatus() + ")");
        }
    }

    private void apply(Part p, PartRequest req) {
        p.setSku(req.sku().trim());
        p.setName(req.name().trim());
        p.setDescription(blankToNull(req.description()));
        p.setUnitCost(req.unitCost());
        p.setStockQty(req.stockQty());
    }

    private PartDto toDto(Part p) {
        return new PartDto(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getUnitCost(), p.getStockQty());
    }

    private PartUsageDto toDto(PartUsage u) {
        return new PartUsageDto(u.getId(), u.getPart().getId(), u.getPart().getSku(), u.getPart().getName(),
                u.getQtyUsed(), u.getUnitCost(), u.getUnitCost().multiply(BigDecimal.valueOf(u.getQtyUsed())),
                u.getUsedBy().getName(), u.getUsedAt(), u.getNote());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
