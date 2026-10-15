package com.applycoding.ledger.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            throw new InvalidMoneyException("amount is required");
        }
        String currency = CurrencySupport.requireIsoCode(currencyCode);
        if (amount.signum() <= 0) {
            throw new InvalidMoneyException("amount must be positive");
        }
        int scale = CurrencySupport.fractionDigits(currency);
        try {
            return new Money(amount.setScale(scale, RoundingMode.UNNECESSARY), currency);
        } catch (ArithmeticException ex) {
            throw new InvalidMoneyException(
                    "amount scale exceeds " + scale + " fraction digits for " + currency);
        }
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public String toPlainString() {
        return amount.toPlainString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Money money)) {
            return false;
        }
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
