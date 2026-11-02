package com.keystone.dto;

import com.keystone.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {
    private UserDtos() {}

    public record UserDto(Long id, String name, String email, String phone, Role role,
                          Long customerId, String customerName, boolean active, Instant createdAt, String skills) {}

    public record CreateUserRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password,
            @Size(max = 30) String phone,
            @NotNull Role role,
            Long customerId,
            @Size(max = 100) String skills) {}

    public record UpdateUserRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 30) String phone,
            @NotNull Role role,
            Long customerId,
            @NotNull Boolean active,
            @Size(max = 100) String skills) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String newPassword) {}

    /** Lightweight technician view for the dispatch dropdown, including current load. */
    public record TechnicianDto(Long id, String name, String email, String skills, boolean skillMatch, long openJobs) {}
}
