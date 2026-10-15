package com.applycoding.ledger.user;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
