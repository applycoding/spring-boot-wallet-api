package com.applycoding.ledger.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestHasherTest {

    @Test
    void sameNormalizedMoneyProducesTheSameHash() {
        UUID accountId = UUID.randomUUID();
        String first = RequestHasher.deposit(new DepositCommand(accountId, Money.of(new BigDecimal("10.5"), "USD")));
        String second = RequestHasher.deposit(new DepositCommand(accountId, Money.of(new BigDecimal("10.50"), "USD")));
        String other = RequestHasher.deposit(new DepositCommand(accountId, Money.of(new BigDecimal("10.51"), "USD")));

        assertEquals(first, second);
        assertNotEquals(first, other);
    }
}
