package com.applycoding.ledger.ledger;

import java.util.Optional;

public final class IdempotencyDecider {

    public enum Outcome {
        EXECUTE,
        REPLAY,
        CONFLICT
    }

    private IdempotencyDecider() {
    }

    public static Outcome decide(Optional<String> storedHash, String requestHash) {
        if (storedHash.isEmpty()) {
            return Outcome.EXECUTE;
        }
        if (storedHash.get().equals(requestHash)) {
            return Outcome.REPLAY;
        }
        return Outcome.CONFLICT;
    }
}
