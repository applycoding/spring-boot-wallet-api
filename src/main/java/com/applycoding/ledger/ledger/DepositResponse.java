package com.applycoding.ledger.ledger;

import java.time.Instant;
import java.util.UUID;

public record DepositResponse(
        UUID transactionId,
        UUID accountId,
        String amount,
        String currency,
        String balance,
        Instant postedAt
) {
}
