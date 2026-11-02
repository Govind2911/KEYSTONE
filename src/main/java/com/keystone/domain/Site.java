package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sites")
@Getter @Setter @NoArgsConstructor
public class Site {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, length = 250) private String addressLine;
    @Column(nullable = false, length = 100) private String city;
    @Column(length = 100) private String state;
    @Column(length = 20) private String postalCode;
    @Column(length = 100) private String country;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
