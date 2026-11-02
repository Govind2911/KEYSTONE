package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "work_orders")
@Getter @Setter @NoArgsConstructor
public class WorkOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Human-readable unique code, e.g. WO-000042. */
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, length = 150) private String title;
    @Column(nullable = false, length = 3000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WorkOrderStatus status = WorkOrderStatus.NEW;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Priority priority = Priority.MEDIUM;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Category category = Category.GENERAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false) private Site site;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id") private UserAccount assignedTo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false) private UserAccount createdBy;

    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant closedAt;

    @Column(nullable = false) private Instant slaDueAt;
    @Column(nullable = false) private boolean slaBreached;
    private Instant slaBreachedAt;
    @Column(nullable = false) private boolean slaRiskNotified;

    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    /** Optimistic locking: two people changing the same order at once -> second gets a 409. */
    @Version private long version;

    @PrePersist void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
