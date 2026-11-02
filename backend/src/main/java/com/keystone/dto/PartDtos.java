package com.keystone.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class PartDtos {
    private PartDtos() {}

    public record PartRequest(
            @NotBlank @Size(max = 50) String sku,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String description,
            @NotNull @DecimalMin("0.00") BigDecimal unitCost,
            @NotNull @Min(0) Integer stockQty) {}

    public record PartDto(Long id, String sku, String name, String description, BigDecimal unitCost, int stockQty) {}

    public record RestockRequest(@NotNull @Min(1) @Max(100000) Integer quantity) {}

    public record PartUsageRequest(
            @NotNull Long partId,
            @NotNull @Min(1) @Max(10000) Integer quantity,
            @Size(max = 500) String note) {}

    public record PartUsageDto(Long id, Long partId, String sku, String partName, int quantity,
                               BigDecimal unitCost, BigDecimal lineCost, String usedBy, Instant usedAt, String note) {}

    public record TimeLogRequest(
            @NotNull @Min(1) @Max(1440) Integer minutes,
            @Size(max = 500) String note) {}

    public record TimeLogDto(Long id, Long technicianId, String technicianName, int minutes, String note, Instant loggedAt) {}
}
