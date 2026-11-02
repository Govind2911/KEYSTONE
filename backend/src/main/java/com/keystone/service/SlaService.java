package com.keystone.service;

import com.keystone.domain.NotificationType;
import com.keystone.domain.Priority;
import com.keystone.domain.Role;
import com.keystone.domain.SlaStatus;
import com.keystone.domain.WorkOrder;
import com.keystone.domain.WorkOrderStatus;
import com.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

/** SLA due dates, live SLA status, and the breach/at-risk scan run by {@link SlaScheduler}. */
@Service
@RequiredArgsConstructor
public class SlaService {
    private static final Logger log = LoggerFactory.getLogger(SlaService.class);

    private final WorkOrderRepository workOrders;
    private final NotificationService notifications;

    @Value("${keystone.sla.at-risk-threshold-percent:75}")
    private int atRiskPercent;

    public Instant dueFor(Priority priority, Instant from) {
        return from.plus(priority.slaHours(), ChronoUnit.HOURS);
    }

    /** How much of the SLA window has been used, 0..100 (capped for display). */
    public int percentElapsed(WorkOrder w, Instant now) {
        long total = Duration.between(w.getCreatedAt(), w.getSlaDueAt()).toSeconds();
        if (total <= 0) return 100;
        long used = Duration.between(w.getCreatedAt(), now).toSeconds();
        return (int) Math.max(0, Math.min(100, used * 100 / total));
    }

    public SlaStatus statusOf(WorkOrder w, Instant now) {
        WorkOrderStatus s = w.getStatus();
        if (s == WorkOrderStatus.CANCELLED) return SlaStatus.NONE;
        if (s == WorkOrderStatus.COMPLETED || s == WorkOrderStatus.CLOSED) {
            if (w.getCompletedAt() == null) return SlaStatus.NONE;
            return w.getCompletedAt().isAfter(w.getSlaDueAt()) ? SlaStatus.BREACHED : SlaStatus.MET;
        }
        if (now.isAfter(w.getSlaDueAt())) return SlaStatus.BREACHED;
        return percentElapsed(w, now) >= atRiskPercent ? SlaStatus.AT_RISK : SlaStatus.ON_TRACK;
    }

    /** Called when a job completes: if it finished after its due time, flag the breach even if the scan missed it. */
    public void flagIfLate(WorkOrder w, Instant completedAt) {
        if (!w.isSlaBreached() && completedAt.isAfter(w.getSlaDueAt())) {
            w.setSlaBreached(true);
            w.setSlaBreachedAt(completedAt);
        }
    }

    /** Recomputes the due date after a priority change; a deadline now in the future clears old breach flags. */
    public void recalculate(WorkOrder w, Instant now) {
        w.setSlaDueAt(dueFor(w.getPriority(), w.getCreatedAt()));
        if (w.getSlaDueAt().isAfter(now)) {
            w.setSlaBreached(false);
            w.setSlaBreachedAt(null);
            w.setSlaRiskNotified(false);
        }
    }

    /**
     * Flags active work orders that have just crossed into "at risk" or "breached" and notifies once each.
     * @return number of work orders newly flagged
     */
    @Transactional
    public int scan() {
        Instant now = Instant.now();
        int flagged = 0;
        for (WorkOrder w : workOrders.findByStatusIn(WorkOrderStatus.ACTIVE)) {
            if (!w.isSlaBreached() && now.isAfter(w.getSlaDueAt())) {
                w.setSlaBreached(true);
                w.setSlaBreachedAt(now);
                flagged++;
                String msg = "SLA BREACHED: " + w.getCode() + " \"" + w.getTitle() + "\" (" + w.getPriority() + ")";
                notifications.notifyRoles(EnumSet.of(Role.MANAGER, Role.DISPATCHER), NotificationType.SLA_BREACHED, msg, w.getId());
                if (w.getAssignedTo() != null) {
                    notifications.notifyUser(w.getAssignedTo().getId(), NotificationType.SLA_BREACHED, msg, w.getId());
                }
                log.warn("SLA breach detected: {} priority={} status={}", w.getCode(), w.getPriority(), w.getStatus());
            } else if (!w.isSlaBreached() && !w.isSlaRiskNotified() && percentElapsed(w, now) >= atRiskPercent) {
                w.setSlaRiskNotified(true);
                flagged++;
                String msg = "SLA at risk: " + w.getCode() + " \"" + w.getTitle() + "\" is " + percentElapsed(w, now) + "% through its window";
                notifications.notifyRoles(EnumSet.of(Role.MANAGER, Role.DISPATCHER), NotificationType.SLA_AT_RISK, msg, w.getId());
                log.info("SLA at risk: {}", w.getCode());
            }
        }
        return flagged;
    }
}
