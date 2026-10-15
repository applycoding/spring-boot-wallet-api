package com.applycoding.ledger.user;

import java.util.UUID;

public record UserResponse(UUID userId, String email) {
}
