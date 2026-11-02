package com.keystone.repository;

import com.keystone.domain.Part;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    Page<Part> findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(String sku, String name, Pageable pageable);
    boolean existsBySkuIgnoreCase(String sku);
    Optional<Part> findBySkuIgnoreCase(String sku);

    /** SELECT ... FOR UPDATE: serialises concurrent stock changes on the same part. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Part p where p.id = :id")
    Optional<Part> findByIdForUpdate(@Param("id") Long id);
}
