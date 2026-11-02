package com.keystone.controller;

import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.dto.PartDtos.*;
import com.keystone.security.AppUserPrincipal;
import com.keystone.service.InventoryService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Parts & time")
public class PartController {
    private final InventoryService service;

    @GetMapping("/parts")
    @PreAuthorize("hasAnyRole('DISPATCHER','TECHNICIAN','MANAGER')")
    @Operation(summary = "Search the parts catalogue", description = "Paginated; q matches SKU or name.")
    public PageResponse<PartDto> list(@RequestParam(required = false) String q, Pageable pageable) {
        return service.listParts(q, pageable);
    }

    @PostMapping("/parts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Add a part (manager)")
    public PartDto create(@Valid @RequestBody PartRequest req) {
        return service.createPart(req);
    }

    @PutMapping("/parts/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Edit a part (manager)")
    public PartDto update(@PathVariable Long id, @Valid @RequestBody PartRequest req) {
        return service.updatePart(id, req);
    }

    @PostMapping("/parts/{id}/restock")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Add stock to a part (manager)")
    public PartDto restock(@PathVariable Long id, @Valid @RequestBody RestockRequest req) {
        return service.restock(id, req);
    }

    @PostMapping("/work-orders/{id}/parts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER')")
    @Operation(summary = "Log parts used on a job",
            description = "Records usage and decrements stock in a single transaction. 409 if stock is insufficient or the job is not in a loggable state.")
    public PartUsageDto usePart(@PathVariable Long id, @Valid @RequestBody PartUsageRequest req,
                                @AuthenticationPrincipal AppUserPrincipal me) {
        return service.usePart(id, req, me);
    }

    @GetMapping("/work-orders/{id}/parts")
    @PreAuthorize("hasAnyRole('DISPATCHER','TECHNICIAN','MANAGER')")
    @Operation(summary = "Parts used on a job")
    public List<PartUsageDto> usage(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        return service.listUsage(id, me);
    }

    @PostMapping("/work-orders/{id}/time")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TECHNICIAN','MANAGER')")
    @Operation(summary = "Log time against a job", description = "Minutes (1-1440) and an optional note.")
    public TimeLogDto logTime(@PathVariable Long id, @Valid @RequestBody TimeLogRequest req,
                              @AuthenticationPrincipal AppUserPrincipal me) {
        return service.logTime(id, req, me);
    }

    @GetMapping("/work-orders/{id}/time")
    @PreAuthorize("hasAnyRole('DISPATCHER','TECHNICIAN','MANAGER')")
    @Operation(summary = "Time logged on a job")
    public List<TimeLogDto> time(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        return service.listTime(id, me);
    }
}
