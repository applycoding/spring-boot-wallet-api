package com.applycoding.ledger.user;

import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistration {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserRegistration(AppUserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public UserResponse insert(String email, String rawPassword) {
        AppUser user = AppUser.create(email, passwordEncoder.encode(rawPassword), clock.instant());
        users.saveAndFlush(user);
        return new UserResponse(user.getId(), user.getEmail());
    }
}
