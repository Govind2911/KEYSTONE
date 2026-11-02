package com.keystone.service;

import com.keystone.domain.UserAccount;
import com.keystone.dto.AuthDtos.LoginRequest;
import com.keystone.dto.AuthDtos.LoginResponse;
import com.keystone.dto.UserDtos.UserDto;
import com.keystone.exception.ApiException;
import com.keystone.repository.UserRepository;
import com.keystone.security.AppUserDetailsService;
import com.keystone.security.AppUserPrincipal;
import com.keystone.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        UserAccount u = users.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        // Same error for "no such user" and "wrong password" so accounts cannot be enumerated.
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            log.warn("Failed login for {}", req.email());
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!u.isActive()) throw new DisabledException("Account deactivated");
        AppUserPrincipal principal = AppUserDetailsService.toPrincipal(u);
        Instant expires = jwt.expiryFromNow();
        log.info("Login: {} ({})", u.getEmail(), u.getRole());
        return new LoginResponse(jwt.generate(principal, expires), "Bearer", expires, UserService.toDtoStatic(u));
    }

    @Transactional(readOnly = true)
    public UserDto me(AppUserPrincipal me) {
        return UserService.toDtoStatic(users.findById(me.id()).orElseThrow(() -> new ApiException.NotFound("User not found")));
    }
}
