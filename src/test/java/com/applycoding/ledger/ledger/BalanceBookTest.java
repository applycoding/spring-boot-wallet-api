package com.applycoding.ledger.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.applycoding.ledger.account.AccountType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BalanceBookTest {

    @Test
    void creditsIncreaseCustomerLiabilitiesAndDebitsIncreaseClearingAssets() {
        BigDecimal customer = BalanceBook.apply(
                AccountType.CUSTOMER, new BigDecimal("10.00"), EntryDirection.CREDIT, new BigDecimal("2.50"));
        BigDecimal spent = BalanceBook.apply(
                AccountType.CUSTOMER, customer, EntryDirection.DEBIT, new BigDecimal("1.00"));
        BigDecimal clearing = BalanceBook.apply(
                AccountType.CLEARING, new BigDecimal("10.00"), EntryDirection.DEBIT, new BigDecimal("2.50"));

        assertEquals(0, new BigDecimal("11.50").compareTo(spent));
        assertEquals(0, new BigDecimal("12.50").compareTo(clearing));
    }

    @Test
    void customerBalanceCannotGoNegative() {
        assertThrows(InsufficientFundsException.class, () -> BalanceBook.apply(
                AccountType.CUSTOMER, new BigDecimal("5.00"), EntryDirection.DEBIT, new BigDecimal("5.01")));
    }
}
