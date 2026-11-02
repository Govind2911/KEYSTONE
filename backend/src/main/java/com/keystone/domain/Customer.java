package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 100) private String contactName;
    @Column(length = 150) private String contactEmail;
    @Column(length = 30) private String contactPhone;
    @Column(length = 300) private String address;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
