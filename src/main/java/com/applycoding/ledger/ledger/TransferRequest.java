package com.applycoding.ledger.ledger;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID fromAccountId,
        @NotNull UUID toAccountId,
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 15, fraction = 4)
        @Schema(example = "25.50")
        BigDecimal amount,
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code")
        @Schema(example = "USD")
        String currency
) {
}
