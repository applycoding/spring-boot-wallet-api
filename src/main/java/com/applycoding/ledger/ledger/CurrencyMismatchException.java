package com.applycoding.ledger.ledger;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String accountCurrency, String requestedCurrency) {
        super("Currency " + requestedCurrency + " does not match account currency " + accountCurrency);
    }
}
