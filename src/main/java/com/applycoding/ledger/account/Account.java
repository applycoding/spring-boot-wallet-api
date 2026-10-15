package com.applycoding.ledger.account;

import com.applycoding.ledger.ledger.BalanceBook;
import com.applycoding.ledger.ledger.EntryDirection;
import com.applycoding.ledger.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account extends AssignedIdEntity {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.UNNECESSARY);

    @Column(name = "owner_id")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private AccountType accountType;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {
    }

    public static Account customer(UUID ownerId, String name, String currency, Instant createdAt) {
        Account account = new Account();
        account.assignId(UUID.randomUUID());
        account.ownerId = ownerId;
        account.accountType = AccountType.CUSTOMER;
        account.name = name;
        account.currency = currency;
        account.balance = ZERO;
        account.createdAt = createdAt;
        return account;
    }

    public static Account clearing(String currency, Instant createdAt) {
        Account account = new Account();
        account.assignId(UUID.randomUUID());
        account.accountType = AccountType.CLEARING;
        account.name = "Clearing " + currency;
        account.currency = currency;
        account.balance = ZERO;
        account.createdAt = createdAt;
        return account;
    }

    public void apply(EntryDirection direction, BigDecimal amount) {
        BigDecimal next = BalanceBook.apply(accountType, balance, direction, amount);
        this.balance = next.setScale(4, RoundingMode.UNNECESSARY);
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
