package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "parts")
@Getter @Setter @NoArgsConstructor
public class Part {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50) private String sku;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 500) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitCost;
    @Column(nullable = false) private int stockQty;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
