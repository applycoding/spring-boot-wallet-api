package com.applycoding.ledger.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.applycoding.ledger.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository users;

    @Mock
    private UserRegistration registration;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailAndRejectsDuplicatesBeforeInsert() {
        when(users.existsByEmail("ada@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> authService.register(new RegisterRequest(" Ada@Example.com ", "password123")));

        verify(registration, never()).insert(any(), any());
    }

    @Test
    void loginRejectsAWrongPassword() {
        AppUser user = AppUser.create("ada@example.com", "hash", Instant.parse("2026-10-15T00:00:00Z"));
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ada@example.com", "password123")));
    }

    @Test
    void loginIssuesATokenForTheStoredUser() {
        AppUser user = AppUser.create("ada@example.com", "hash", Instant.parse("2026-10-15T00:00:00Z"));
        when(users.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(jwtService.generate(user.getId(), user.getEmail())).thenReturn("token");
        when(jwtService.expiresInSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(new LoginRequest("ada@example.com", "password123"));

        assertEquals("token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
    }
}
