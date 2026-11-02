package com.keystone.service;

import com.keystone.domain.Category;
import com.keystone.domain.Customer;
import com.keystone.domain.Role;
import com.keystone.domain.UserAccount;
import com.keystone.domain.WorkOrderStatus;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.dto.UserDtos.*;
import com.keystone.exception.ApiException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import com.keystone.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** User administration (manager only). There is deliberately no public self-registration. */
@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;
    private final CustomerRepository customers;
    private final WorkOrderRepository workOrders;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public PageResponse<UserDto> list(Role role, Pageable pageable) {
        Pageable p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
        Page<UserAccount> page = role == null ? users.findAll(p) : users.findByRole(role, p);
        return PageResponse.of(page, this::toDto);
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        String email = req.email().trim();
        if (users.existsByEmailIgnoreCase(email)) throw new ApiException.Conflict("A user with that email already exists");
        UserAccount u = new UserAccount();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPhone(blankToNull(req.phone()));
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(req.role());
        u.setSkills(normalizeSkills(req.role(), req.skills()));
        u.setCustomer(resolveCustomer(req.role(), req.customerId()));
        u.setActive(true);
        log.info("User {} ({}) created", email, req.role());
        return toDto(users.save(u));
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest req, AppUserPrincipal me) {
        UserAccount u = users.findById(id).orElseThrow(() -> new ApiException.NotFound("User not found"));
        if (u.getId().equals(me.id()) && (!req.active() || req.role() != u.getRole())) {
            throw new ApiException.Conflict("You cannot deactivate yourself or change your own role");
        }
        u.setName(req.name().trim());
        u.setPhone(blankToNull(req.phone()));
        u.setRole(req.role());
        u.setSkills(normalizeSkills(req.role(), req.skills()));
        u.setCustomer(resolveCustomer(req.role(), req.customerId()));
        u.setActive(req.active());
        log.info("User {} updated by {}", u.getEmail(), me.email());
        return toDto(u);
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req, AppUserPrincipal me) {
        UserAccount u = users.findById(id).orElseThrow(() -> new ApiException.NotFound("User not found"));
        u.setPasswordHash(encoder.encode(req.newPassword()));
        log.info("Password for {} reset by {}", u.getEmail(), me.email());
    }

    /** Active technicians with open-job counts. When a category is given, technicians with that skill come first, then the least loaded. */
    @Transactional(readOnly = true)
    public List<TechnicianDto> technicians(Category category) {
        Map<Long, Long> load = new HashMap<>();
        for (Object[] row : workOrders.countOpenByTechnician(WorkOrderStatus.ACTIVE)) {
            load.put((Long) row[0], (Long) row[1]);
        }
        return users.findByRoleAndActiveTrueOrderByName(Role.TECHNICIAN).stream()
                .map(t -> new TechnicianDto(t.getId(), t.getName(), t.getEmail(), t.getSkills(),
                        category != null && hasSkill(t.getSkills(), category), load.getOrDefault(t.getId(), 0L)))
                .sorted(Comparator.comparing((TechnicianDto t) -> !t.skillMatch())
                        .thenComparingLong(TechnicianDto::openJobs).thenComparing(TechnicianDto::name))
                .toList();
    }

    private static boolean hasSkill(String skills, Category c) {
        return skills != null && Arrays.asList(skills.split(",")).contains(c.name());
    }

    private String normalizeSkills(Role role, String raw) {
        if (role != Role.TECHNICIAN || raw == null || raw.isBlank()) return null;
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String v = part.trim().toUpperCase();
            if (v.isEmpty()) continue;
            try {
                Category c = Category.valueOf(v);
                if (c != Category.GENERAL && !out.contains(c.name())) out.add(c.name());
            } catch (IllegalArgumentException e) {
                throw new ApiException.BadRequest("Unknown skill: " + part.trim());
            }
        }
        return out.isEmpty() ? null : String.join(",", out);
    }

    private Customer resolveCustomer(Role role, Long customerId) {
        if (role == Role.CUSTOMER) {
            if (customerId == null) throw new ApiException.BadRequest("A customer user must be linked to a customer (customerId)");
            return customers.findById(customerId).orElseThrow(() -> new ApiException.BadRequest("Customer not found"));
        }
        if (customerId != null) throw new ApiException.BadRequest("Only CUSTOMER users can be linked to a customer");
        return null;
    }

    private UserDto toDto(UserAccount u) {
        Customer c = u.getCustomer();
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(),
                c == null ? null : c.getId(), c == null ? null : c.getName(), u.isActive(), u.getCreatedAt(), u.getSkills());
    }

    public static UserDto toDtoStatic(UserAccount u) {
        Customer c = u.getCustomer();
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(),
                c == null ? null : c.getId(), c == null ? null : c.getName(), u.isActive(), u.getCreatedAt(), u.getSkills());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
