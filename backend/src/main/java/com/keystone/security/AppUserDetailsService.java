package com.keystone.security;

import com.keystone.domain.UserAccount;
import com.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    @Override
    @Transactional(readOnly = true)
    public AppUserPrincipal loadUserByUsername(String email) {
        UserAccount u = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return toPrincipal(u);
    }

    public static AppUserPrincipal toPrincipal(UserAccount u) {
        Long customerId = u.getCustomer() == null ? null : u.getCustomer().getId();
        return new AppUserPrincipal(u.getId(), u.getName(), u.getEmail(), u.getPasswordHash(),
                u.getRole(), customerId, u.isActive());
    }
}
