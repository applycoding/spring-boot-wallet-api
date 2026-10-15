package com.applycoding.ledger.ledger;

import com.applycoding.ledger.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry extends AssignedIdEntity {

    @Column(name = "journal_id", nullable = false)
    private UUID journalId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private EntryDirection direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "posting_sequence", nullable = false)
    private long postingSequence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public static LedgerEntry create(
            UUID journalId,
            UUID accountId,
            EntryDirection direction,
            Money money,
            BigDecimal balanceAfter,
            long postingSequence,
            Instant createdAt) {
        LedgerEntry entry = new LedgerEntry();
        entry.assignId(UUID.randomUUID());
        entry.journalId = journalId;
        entry.accountId = accountId;
        entry.direction = direction;
        entry.amount = money.amount().setScale(4, RoundingMode.UNNECESSARY);
        entry.currency = money.currency();
        entry.balanceAfter = balanceAfter.setScale(4, RoundingMode.UNNECESSARY);
        entry.postingSequence = postingSequence;
        entry.createdAt = createdAt;
        return entry;
    }

    public UUID getJournalId() {
        return journalId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public EntryDirection getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
