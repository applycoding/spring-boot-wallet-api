package com.applycoding.ledger.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdempotencyDeciderTest {

    @Test
    void decidesExecuteReplayOrConflict() {
        assertEquals(IdempotencyDecider.Outcome.EXECUTE, IdempotencyDecider.decide(Optional.empty(), "abc"));
        assertEquals(IdempotencyDecider.Outcome.REPLAY, IdempotencyDecider.decide(Optional.of("abc"), "abc"));
        assertEquals(IdempotencyDecider.Outcome.CONFLICT, IdempotencyDecider.decide(Optional.of("abc"), "def"));
    }
}
