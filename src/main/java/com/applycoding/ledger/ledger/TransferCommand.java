package com.applycoding.ledger.ledger;

import java.util.UUID;

public record TransferCommand(UUID fromAccountId, UUID toAccountId, Money money) {
}
