package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** Append-only audit row. @Immutable here, and a database trigger enforces it for everyone else. */
@Entity
@Immutable
@Table(name = "work_order_status_history")
@Getter @Setter @NoArgsConstructor
public class WorkOrderStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false) private WorkOrder workOrder;
    @Enumerated(EnumType.STRING) @Column(length = 20) private WorkOrderStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WorkOrderStatus toStatus;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false) private UserAccount changedBy;
    @Column(nullable = false, updatable = false) private Instant changedAt;
    @Column(length = 500) private String note;

    @PrePersist void onCreate() { if (changedAt == null) changedAt = Instant.now(); }
}
