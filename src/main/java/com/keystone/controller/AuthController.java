package com.keystone.controller;

import com.keystone.dto.AuthDtos.LoginRequest;
import com.keystone.dto.AuthDtos.LoginResponse;
import com.keystone.dto.UserDtos.UserDto;
import com.keystone.security.AppUserPrincipal;
import com.keystone.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService auth;

    @PostMapping("/login")
    @SecurityRequirements   // public endpoint: no bearer token needed
    @Operation(summary = "Log in", description = "Verifies email and password and returns a signed, expiring JWT plus the user's role.")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Returns the profile behind the presented token.")
    public UserDto me(@AuthenticationPrincipal AppUserPrincipal me) {
        return auth.me(me);
    }
}
