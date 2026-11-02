package com.keystone.controller;

import com.keystone.dto.ReportDtos.Summary;
import com.keystone.security.AppUserPrincipal;
import com.keystone.service.ReportingService;
import com.keystone.service.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Reports & SLA")
public class ReportController {
    private final ReportingService reports;
    private final SlaService sla;

    @GetMapping("/reports/summary")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Dashboard metrics (manager)",
            description = "Counts by status, overdue and at-risk work, SLA compliance, and breakdowns by technician and site. All optional filters apply to every figure.")
    public Summary summary(@RequestParam(required = false) Long customerId,
                           @RequestParam(required = false) Long siteId,
                           @RequestParam(required = false) Long technicianId,
                           @RequestParam(required = false) Integer days,
                           @AuthenticationPrincipal AppUserPrincipal me) {
        return reports.summary(customerId, siteId, technicianId, days, me);
    }

    @PostMapping("/sla/scan")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Run the SLA check now (manager)", description = "The same scan the scheduler runs every minute: flags at-risk/breached work orders and notifies managers.")
    public Map<String, Integer> scan() {
        return Map.of("flagged", sla.scan());
    }
}
