package com.keystone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CustomerDtos {
    private CustomerDtos() {}

    public record CustomerRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 100) String contactName,
            @Email @Size(max = 150) String contactEmail,
            @Size(max = 30) String contactPhone,
            @Size(max = 300) String address,
            Boolean active) {}

    public record CustomerDto(Long id, String name, String contactName, String contactEmail, String contactPhone,
                              String address, boolean active, long siteCount, Instant createdAt) {}

    public record SiteRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 250) String addressLine,
            @NotBlank @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 20) String postalCode,
            @Size(max = 100) String country) {}

    public record SiteDto(Long id, Long customerId, String customerName, String name, String addressLine,
                          String city, String state, String postalCode, String country) {}
}
