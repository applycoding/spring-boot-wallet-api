package com.applycoding.ledger.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) @Schema(example = "ada@example.com") String email,
        @NotBlank @Size(min = 8, max = 72) @Schema(example = "password123") String password
) {
}
