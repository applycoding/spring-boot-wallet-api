package com.applycoding.ledger.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {

    private final IdempotencyRepository idempotency;
    private final LedgerTransaction ledgerTransaction;
    private final ObjectMapper objectMapper;

    public LedgerService(
            IdempotencyRepository idempotency,
            LedgerTransaction ledgerTransaction,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.idempotency = idempotency;
        this.ledgerTransaction = ledgerTransaction;
        this.objectMapper = objectMapper;
    }

    public DepositResponse deposit(UUID userId, DepositRequest request, String idempotencyKey) {
        DepositCommand command = new DepositCommand(request.accountId(), Money.of(request.amount(), request.currency()));
        String hash = RequestHasher.deposit(command);
        return switch (decide(userId, idempotencyKey, hash)) {
            case REPLAY -> read(userId, idempotencyKey, DepositResponse.class);
            case CONFLICT -> throw new IdempotencyConflictException();
            case EXECUTE -> {
                try {
                    yield ledgerTransaction.deposit(userId, command, idempotencyKey, hash);
                } catch (DataIntegrityViolationException ex) {
                    yield replayAfterRace(ex, userId, idempotencyKey, hash, DepositResponse.class);
                }
            }
        };
    }

    public TransferResponse transfer(UUID userId, TransferRequest request, String idempotencyKey) {
        TransferCommand command = new TransferCommand(
                request.fromAccountId(),
                request.toAccountId(),
                Money.of(request.amount(), request.currency()));
        String hash = RequestHasher.transfer(command);
        return switch (decide(userId, idempotencyKey, hash)) {
            case REPLAY -> read(userId, idempotencyKey, TransferResponse.class);
            case CONFLICT -> throw new IdempotencyConflictException();
            case EXECUTE -> {
                try {
                    yield ledgerTransaction.transfer(userId, command, idempotencyKey, hash);
                } catch (DataIntegrityViolationException ex) {
                    yield replayAfterRace(ex, userId, idempotencyKey, hash, TransferResponse.class);
                }
            }
        };
    }

    private IdempotencyDecider.Outcome decide(UUID userId, String idempotencyKey, String hash) {
        Optional<IdempotencyRecord> existing = idempotency.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        return IdempotencyDecider.decide(existing.map(IdempotencyRecord::getRequestHash), hash);
    }

    private <T> T read(UUID userId, String idempotencyKey, Class<T> type) {
        IdempotencyRecord record = idempotency.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElseThrow();
        return IdempotencyResponses.read(record, type, objectMapper);
    }

    private <T> T replayAfterRace(
            DataIntegrityViolationException ex,
            UUID userId,
            String idempotencyKey,
            String hash,
            Class<T> type) {
        Optional<IdempotencyRecord> record = idempotency.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (record.isEmpty()) {
            throw ex;
        }
        if (!record.get().getRequestHash().equals(hash)) {
            throw new IdempotencyConflictException();
        }
        return IdempotencyResponses.read(record.get(), type, objectMapper);
    }
}
