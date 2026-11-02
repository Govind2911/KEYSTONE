package com.keystone.controller;

import com.keystone.dto.CommonDtos.AuditEntry;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit log")
public class AuditController {
    private final AuditService service;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Audit log (manager)", description = "Every status change across all work orders, newest first. Built from the append-only status history.")
    public PageResponse<AuditEntry> recent(Pageable pageable) {
        return service.recent(pageable);
    }
}
