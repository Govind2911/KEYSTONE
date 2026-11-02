package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "time_logs")
@Getter @Setter @NoArgsConstructor
public class TimeLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false) private WorkOrder workOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false) private UserAccount technician;
    @Column(nullable = false) private int minutes;
    @Column(length = 500) private String note;
    @Column(nullable = false, updatable = false) private Instant loggedAt;

    @PrePersist void onCreate() { if (loggedAt == null) loggedAt = Instant.now(); }
}
