package com.applycoding.ledger.ledger;

import java.time.Instant;
import java.util.UUID;

public record TransactionView(
        UUID transactionId,
        String type,
        String direction,
        String amount,
        String currency,
        String balanceAfter,
        UUID counterpartyAccountId,
        Instant postedAt
) {
}
