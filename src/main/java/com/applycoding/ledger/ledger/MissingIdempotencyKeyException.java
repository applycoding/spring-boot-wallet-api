package com.applycoding.ledger.ledger;

public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException() {
        super("Idempotency-Key header is required and must be 8-200 characters (letters, digits, '.', '_', ':', '-')");
    }
}
