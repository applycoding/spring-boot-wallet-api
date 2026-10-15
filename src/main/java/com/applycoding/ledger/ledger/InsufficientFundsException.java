package com.applycoding.ledger.ledger;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("Insufficient funds");
    }

    public InsufficientFundsException(UUID accountId) {
        super("Insufficient funds in account " + accountId);
    }
}
