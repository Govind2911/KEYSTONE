package com.keystone.controller;

import com.keystone.domain.Category;
import com.keystone.domain.Role;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.dto.UserDtos.*;
import com.keystone.security.AppUserPrincipal;
import com.keystone.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {
    private final UserService users;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List users (manager)", description = "Paginated; optionally filter by role.")
    public PageResponse<UserDto> list(@RequestParam(required = false) Role role, Pageable pageable) {
        return users.list(role, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Create a user (manager)", description = "CUSTOMER users must supply customerId; staff users must not.")
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return users.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update a user (manager)", description = "Change name, phone, role, customer link, or deactivate.")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req,
                          @AuthenticationPrincipal AppUserPrincipal me) {
        return users.update(id, req, me);
    }

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Reset a user's password (manager)")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req,
                              @AuthenticationPrincipal AppUserPrincipal me) {
        users.resetPassword(id, req, me);
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Operation(summary = "Active technicians with current workload", description = "Used by dispatch to assign fairly. Pass ?category=HVAC to put technicians with that skill first (skillMatch=true).")
    public List<TechnicianDto> technicians(@RequestParam(required = false) Category category) {
        return users.technicians(category);
    }
}
