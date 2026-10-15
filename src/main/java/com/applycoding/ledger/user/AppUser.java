package com.applycoding.ledger.user;

import com.applycoding.ledger.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser extends AssignedIdEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUser() {
    }

    public static AppUser create(String email, String passwordHash, Instant createdAt) {
        AppUser user = new AppUser();
        user.assignId(UUID.randomUUID());
        user.email = email;
        user.passwordHash = passwordHash;
        user.createdAt = createdAt;
        return user;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
