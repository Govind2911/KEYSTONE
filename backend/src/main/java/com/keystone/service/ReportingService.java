package com.keystone.service;

import com.keystone.domain.Role;
import com.keystone.domain.SlaStatus;
import com.keystone.domain.UserAccount;
import com.keystone.domain.WorkOrder;
import com.keystone.domain.WorkOrderStatus;
import com.keystone.dto.ReportDtos.*;
import com.keystone.dto.WorkOrderDtos.WorkOrderDto;
import com.keystone.repository.UserRepository;
import com.keystone.security.AppUserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Dashboard numbers. Every figure honours the same optional filters (customer, site, technician, last N days). */
@Service
@RequiredArgsConstructor
public class ReportingService {
    private static final int SLA_WINDOW_DAYS = 30;

    @PersistenceContext
    private EntityManager em;

    private final UserRepository users;
    private final SlaService sla;
    private final WorkOrderMapper mapper;

    /** Builds " where ..." plus named parameters from whichever filters were supplied. */
    private static final class Where {
        final StringBuilder sql = new StringBuilder(" where 1=1");
        final Map<String, Object> params = new HashMap<>();

        Where add(String clause, String name, Object value) {
            if (value != null) {
                sql.append(" and ").append(clause);
                params.put(name, value);
            }
            return this;
        }

        <T> TypedQuery<T> bind(TypedQuery<T> q) {
            params.forEach(q::setParameter);
            return q;
        }
    }

    @Transactional(readOnly = true)
    public Summary summary(Long customerId, Long siteId, Long technicianId, Integer days, AppUserPrincipal me) {
        Instant now = Instant.now();
        Instant since = days == null ? null : now.minus(days, ChronoUnit.DAYS);
        Where w = new Where()
                .add("w.customer.id = :customerId", "customerId", customerId)
                .add("w.site.id = :siteId", "siteId", siteId)
                .add("w.assignedTo.id = :technicianId", "technicianId", technicianId)
                .add("w.createdAt >= :since", "since", since);

        // 1. counts by status
        Map<WorkOrderStatus, Long> statusCounts = new EnumMap<>(WorkOrderStatus.class);
        for (WorkOrderStatus s : WorkOrderStatus.values()) statusCounts.put(s, 0L);
        for (Object[] row : w.bind(em.createQuery("select w.status, count(w) from WorkOrder w" + w.sql + " group by w.status", Object[].class)).getResultList()) {
            statusCounts.put((WorkOrderStatus) row[0], (Long) row[1]);
        }
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        // 2. the active workload: used for overdue / at-risk / per-technician / per-site figures
        List<WorkOrder> active = w.bind(em.createQuery(
                "select w from WorkOrder w join fetch w.site join fetch w.customer left join fetch w.assignedTo join fetch w.createdBy"
                        + w.sql + " and w.status in :active order by w.slaDueAt asc", WorkOrder.class))
                .setParameter("active", WorkOrderStatus.ACTIVE).getResultList();
        long open = active.size();
        long overdue = 0, atRisk = 0;
        Map<Long, Long> overdueByTech = new HashMap<>();
        Map<Long, Long> overdueBySite = new HashMap<>();
        List<WorkOrderDto> overdueOrders = new ArrayList<>();
        for (WorkOrder o : active) {
            SlaStatus s = sla.statusOf(o, now);
            if (s == SlaStatus.BREACHED) {
                overdue++;
                if (o.getAssignedTo() != null) overdueByTech.merge(o.getAssignedTo().getId(), 1L, Long::sum);
                overdueBySite.merge(o.getSite().getId(), 1L, Long::sum);
                if (overdueOrders.size() < 10) overdueOrders.add(mapper.toListDto(o, me));
            } else if (s == SlaStatus.AT_RISK) {
                atRisk++;
            }
        }
        long breached = ((Number) w.bind(em.createQuery("select count(w) from WorkOrder w" + w.sql + " and w.slaBreached = true", Long.class)).getSingleResult()).longValue();

        // 3. SLA compliance over completed work in the window
        Instant slaSince = now.minus(days == null ? SLA_WINDOW_DAYS : days, ChronoUnit.DAYS);
        long slaTotal = w.bind(em.createQuery("select count(w) from WorkOrder w" + w.sql
                + " and w.completedAt is not null and w.completedAt >= :slaSince", Long.class)).setParameter("slaSince", slaSince).getSingleResult();
        long slaMet = w.bind(em.createQuery("select count(w) from WorkOrder w" + w.sql
                + " and w.completedAt is not null and w.completedAt >= :slaSince and w.completedAt <= w.slaDueAt", Long.class))
                .setParameter("slaSince", slaSince).getSingleResult();
        Double pct = slaTotal == 0 ? null : Math.round(slaMet * 1000.0 / slaTotal) / 10.0;

        // 4. breakdown by technician
        Map<Long, long[]> tech = new LinkedHashMap<>();      // [open, completed]
        Map<Long, String> techNames = new HashMap<>();
        for (UserAccount u : users.findByRoleAndActiveTrueOrderByName(Role.TECHNICIAN)) {
            if (technicianId == null || technicianId.equals(u.getId())) {
                tech.put(u.getId(), new long[2]);
                techNames.put(u.getId(), u.getName());
            }
        }
        for (Object[] row : w.bind(em.createQuery("select a.id, a.name, w.status, count(w) from WorkOrder w join w.assignedTo a"
                + w.sql + " group by a.id, a.name, w.status", Object[].class)).getResultList()) {
            Long id = (Long) row[0];
            WorkOrderStatus st = (WorkOrderStatus) row[2];
            long n = (Long) row[3];
            tech.computeIfAbsent(id, k -> new long[2]);
            techNames.putIfAbsent(id, (String) row[1]);
            if (st.isActive()) tech.get(id)[0] += n;
            else if (st == WorkOrderStatus.COMPLETED || st == WorkOrderStatus.CLOSED) tech.get(id)[1] += n;
        }
        List<TechnicianLoad> byTechnician = tech.entrySet().stream()
                .map(e -> new TechnicianLoad(e.getKey(), techNames.get(e.getKey()), e.getValue()[0], e.getValue()[1],
                        overdueByTech.getOrDefault(e.getKey(), 0L)))
                .sorted(Comparator.comparingLong(TechnicianLoad::open).reversed()).toList();

        // 5. breakdown by site
        Map<Long, long[]> siteMap = new LinkedHashMap<>();   // [total, open]
        Map<Long, String> siteNames = new HashMap<>();
        for (Object[] row : w.bind(em.createQuery("select s.id, s.name, w.status, count(w) from WorkOrder w join w.site s"
                + w.sql + " group by s.id, s.name, w.status", Object[].class)).getResultList()) {
            Long id = (Long) row[0];
            long n = (Long) row[3];
            siteMap.computeIfAbsent(id, k -> new long[2]);
            siteNames.putIfAbsent(id, (String) row[1]);
            siteMap.get(id)[0] += n;
            if (((WorkOrderStatus) row[2]).isActive()) siteMap.get(id)[1] += n;
        }
        List<SiteLoad> bySite = siteMap.entrySet().stream()
                .map(e -> new SiteLoad(e.getKey(), siteNames.get(e.getKey()), e.getValue()[0], e.getValue()[1],
                        overdueBySite.getOrDefault(e.getKey(), 0L)))
                .sorted(Comparator.comparingLong(SiteLoad::total).reversed()).toList();

        return new Summary(new Filters(customerId, siteId, technicianId, days), total, open, overdue, atRisk, breached,
                statusCounts, new SlaCompliance(slaMet, slaTotal, pct), byTechnician, bySite, overdueOrders, now);
    }
}
