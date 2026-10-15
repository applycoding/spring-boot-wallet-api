package com.applycoding.ledger.ledger;

import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        UUID fromAccountId,
        UUID toAccountId,
        String amount,
        String currency,
        String fromBalance,
        String toBalance,
        Instant postedAt
) {
}
