package com.keystone.repository;

import com.keystone.domain.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {
    Page<Site> findByCustomerId(Long customerId, Pageable pageable);
    Page<Site> findByCustomerIdAndNameContainingIgnoreCase(Long customerId, String q, Pageable pageable);
    long countByCustomerId(Long customerId);
}
