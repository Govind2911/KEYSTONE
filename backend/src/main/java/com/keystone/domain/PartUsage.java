package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "part_usages")
@Getter @Setter @NoArgsConstructor
public class PartUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false) private WorkOrder workOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false) private Part part;
    @Column(nullable = false) private int qtyUsed;
    /** Price snapshot: later price changes never rewrite old jobs. */
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitCost;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "used_by_id", nullable = false) private UserAccount usedBy;
    @Column(nullable = false, updatable = false) private Instant usedAt;
    @Column(length = 500) private String note;

    @PrePersist void onCreate() { if (usedAt == null) usedAt = Instant.now(); }
}
