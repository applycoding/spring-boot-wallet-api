package com.applycoding.ledger.ledger;

import java.util.UUID;

public record Leg(UUID accountId, EntryDirection direction, Money money) {
}
