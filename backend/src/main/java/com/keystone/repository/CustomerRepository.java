package com.keystone.repository;

import com.keystone.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Page<Customer> findByNameContainingIgnoreCase(String q, Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
    Optional<Customer> findByNameIgnoreCase(String name);
}
