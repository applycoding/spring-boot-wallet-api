package com.applycoding.ledger.ledger;

import com.applycoding.ledger.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(name = "uq_idempotency_user_key", columnNames = {"user_id", "idempotency_key"})
)
public class IdempotencyRecord extends AssignedIdEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(nullable = false, length = 32)
    private String operation;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_body", nullable = false, length = 8000)
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public static IdempotencyRecord completed(
            UUID userId,
            String idempotencyKey,
            String operation,
            String requestHash,
            String responseBody,
            Instant createdAt) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.assignId(UUID.randomUUID());
        record.userId = userId;
        record.idempotencyKey = idempotencyKey;
        record.operation = operation;
        record.requestHash = requestHash;
        record.responseBody = responseBody;
        record.createdAt = createdAt;
        return record;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
