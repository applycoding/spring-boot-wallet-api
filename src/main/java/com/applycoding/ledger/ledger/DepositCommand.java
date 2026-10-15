package com.applycoding.ledger.ledger;

import java.util.UUID;

public record DepositCommand(UUID accountId, Money money) {
}
