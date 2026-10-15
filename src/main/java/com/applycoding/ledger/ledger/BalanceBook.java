package com.applycoding.ledger.ledger;

import com.applycoding.ledger.account.AccountType;
import java.math.BigDecimal;

/**
 * Customer wallets are liabilities: a credit increases the spendable balance.
 * Clearing accounts are assets: a debit increases cash held for that currency.
 */
public final class BalanceBook {

    private BalanceBook() {
    }

    public static BigDecimal apply(
            AccountType accountType,
            BigDecimal balance,
            EntryDirection direction,
            BigDecimal amount) {
        BigDecimal next = balance.add(signed(accountType, direction, amount));
        if (accountType == AccountType.CUSTOMER && next.signum() < 0) {
            throw new InsufficientFundsException();
        }
        return next;
    }

    private static BigDecimal signed(AccountType accountType, EntryDirection direction, BigDecimal amount) {
        int sign = switch (accountType) {
            case CLEARING -> direction == EntryDirection.DEBIT ? 1 : -1;
            case CUSTOMER -> direction == EntryDirection.DEBIT ? -1 : 1;
        };
        return amount.multiply(BigDecimal.valueOf(sign));
    }
}
