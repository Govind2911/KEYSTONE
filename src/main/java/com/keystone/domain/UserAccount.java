package com.keystone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 150) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Column(length = 30) private String phone;
    /** Technicians only: comma-separated categories they can cover, e.g. "HVAC,PLUMBING". */
    @Column(length = 100) private String skills;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    /** Set only for CUSTOMER users: the organisation whose data they may see. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
