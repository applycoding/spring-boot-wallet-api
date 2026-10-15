package com.applycoding.ledger.ledger;

import com.applycoding.ledger.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journals")
public class Journal extends AssignedIdEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "journal_type", nullable = false, length = 16)
    private JournalType journalType;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Journal() {
    }

    public static Journal create(JournalType journalType, UUID createdBy, Instant createdAt) {
        Journal journal = new Journal();
        journal.assignId(UUID.randomUUID());
        journal.journalType = journalType;
        journal.createdBy = createdBy;
        journal.createdAt = createdAt;
        return journal;
    }

    public JournalType getJournalType() {
        return journalType;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
