package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "attachments")
@Getter @Setter @NoArgsConstructor
public class Attachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false) private WorkOrder workOrder;
    @Column(nullable = false, length = 200) private String fileName;
    @Column(nullable = false, length = 100) private String contentType;
    @Column(nullable = false) private long sizeBytes;
    @Column(nullable = false, length = 300) private String storageKey;
    @Column(nullable = false, length = 20) private String storageProvider = "LOCAL";
    @Column(length = 600) private String publicUrl;
    @Column(length = 300) private String cloudPublicId;
    @Column(length = 20) private String resourceType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false) private UserAccount uploadedBy;
    @Column(nullable = false, updatable = false) private Instant uploadedAt;

    @PrePersist void onCreate() { if (uploadedAt == null) uploadedAt = Instant.now(); }
}
