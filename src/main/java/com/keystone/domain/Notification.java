package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private NotificationType type;
    @Column(nullable = false, length = 300) private String message;
    private Long workOrderId;
    @Column(nullable = false) private boolean seen;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
