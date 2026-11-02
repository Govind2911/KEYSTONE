package com.keystone.controller;

import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.dto.CustomerDtos.*;
import com.keystone.security.AppUserPrincipal;
import com.keystone.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Customers & Sites")
public class CustomerController {
    private final CustomerService service;

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Operation(summary = "Search customers", description = "Paginated list; q matches customer name.")
    public PageResponse<CustomerDto> list(@RequestParam(required = false) String q, Pageable pageable) {
        return service.list(q, pageable);
    }

    @GetMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Operation(summary = "Get one customer")
    public CustomerDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Operation(summary = "Create a customer")
    public CustomerDto create(@Valid @RequestBody CustomerRequest req) {
        return service.create(req);
    }

    @PutMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Operation(summary = "Edit a customer")
    public CustomerDto update(@PathVariable Long id, @Valid @RequestBody CustomerRequest req) {
        return service.update(id, req);
    }

    @GetMapping("/customers/{id}/sites")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    @Operation(summary = "List a customer's sites", description = "Staff may list any customer's sites. A CUSTOMER user may only list their own organisation's (others return 404).")
    public PageResponse<SiteDto> listSites(@PathVariable Long id, @RequestParam(required = false) String q,
                                           Pageable pageable, @AuthenticationPrincipal AppUserPrincipal me) {
        return service.listSites(id, q, pageable, me);
    }

    @PostMapping("/customers/{id}/sites")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Operation(summary = "Add a site to a customer")
    public SiteDto createSite(@PathVariable Long id, @Valid @RequestBody SiteRequest req) {
        return service.createSite(id, req);
    }

    @PutMapping("/sites/{siteId}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Operation(summary = "Edit a site")
    public SiteDto updateSite(@PathVariable Long siteId, @Valid @RequestBody SiteRequest req) {
        return service.updateSite(siteId, req);
    }
}
