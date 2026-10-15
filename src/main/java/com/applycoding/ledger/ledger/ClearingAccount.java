package com.applycoding.ledger.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "clearing_accounts")
public class ClearingAccount {

    @Id
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    protected ClearingAccount() {
    }

    public ClearingAccount(String currency, UUID accountId) {
        this.currency = currency;
        this.accountId = accountId;
    }

    public String getCurrency() {
        return currency;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
