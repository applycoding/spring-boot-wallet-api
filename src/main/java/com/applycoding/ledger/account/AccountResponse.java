package com.applycoding.ledger.account;

import com.applycoding.ledger.ledger.CurrencySupport;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        String currency,
        String balance,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getCurrency(),
                CurrencySupport.format(account.getBalance(), account.getCurrency()),
                account.getCreatedAt());
    }
}
