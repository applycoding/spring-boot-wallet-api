package com.applycoding.ledger.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email @Schema(example = "ada@example.com") String email,
        @NotBlank @Schema(example = "password123") String password
) {
}
