package com.applycoding.ledger.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private IdempotencyRepository idempotency;

    @Mock
    private LedgerTransaction ledgerTransaction;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(idempotency, ledgerTransaction, objectMapper);
    }

    @Test
    void depositReplaysAStoredSuccessWithoutPostingAgain() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        DepositRequest request = new DepositRequest(accountId, new BigDecimal("10.50"), "USD");
        DepositCommand command = new DepositCommand(accountId, Money.of(request.amount(), request.currency()));
        DepositResponse stored = new DepositResponse(
                UUID.randomUUID(), accountId, "10.50", "USD", "10.50", Instant.parse("2026-10-15T00:00:00Z"));
        IdempotencyRecord record = IdempotencyRecord.completed(
                userId,
                "key-12345",
                "DEPOSIT",
                RequestHasher.deposit(command),
                objectMapper.writeValueAsString(stored),
                Instant.parse("2026-10-15T00:00:00Z"));
        when(idempotency.findByUserIdAndIdempotencyKey(userId, "key-12345")).thenReturn(Optional.of(record));

        DepositResponse response = ledgerService.deposit(userId, request, "key-12345");

        assertEquals(stored, response);
        verify(ledgerTransaction, never()).deposit(any(), any(), any(), any());
    }

    @Test
    void depositRejectsAReusedKeyWithADifferentBody() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        DepositRequest request = new DepositRequest(accountId, new BigDecimal("10.00"), "USD");
        IdempotencyRecord record = IdempotencyRecord.completed(
                userId, "key-12345", "DEPOSIT", "different-hash", "{}", Instant.now());
        when(idempotency.findByUserIdAndIdempotencyKey(userId, "key-12345")).thenReturn(Optional.of(record));

        assertThrows(IdempotencyConflictException.class,
                () -> ledgerService.deposit(userId, request, "key-12345"));
        verify(ledgerTransaction, never()).deposit(any(), any(), any(), any());
    }
}
