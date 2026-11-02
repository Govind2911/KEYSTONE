package com.keystone.repository;

import com.keystone.domain.Role;
import com.keystone.domain.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Page<UserAccount> findByRole(Role role, Pageable pageable);
    List<UserAccount> findByRoleInAndActiveTrue(Collection<Role> roles);
    List<UserAccount> findByRoleAndActiveTrueOrderByName(Role role);
    List<UserAccount> findByCustomerIdAndActiveTrue(Long customerId);
}
