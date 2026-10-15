package com.applycoding.ledger.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.applycoding.ledger.account.Account;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostingRulesTest {

    @Test
    void depositIsABalancedDebitToClearingAndCreditToTheCustomer() {
        Account clearing = Account.clearing("USD", Instant.now());
        Account customer = Account.customer(UUID.randomUUID(), "Everyday", "USD", Instant.now());
        Money money = Money.of(new BigDecimal("15.25"), "USD");

        JournalDraft draft = PostingRules.deposit(clearing, customer, money);

        assertEquals(JournalType.DEPOSIT, draft.type());
        assertEquals(EntryDirection.DEBIT, draft.legs().get(0).direction());
        assertEquals(clearing.getId(), draft.legs().get(0).accountId());
        assertEquals(EntryDirection.CREDIT, draft.legs().get(1).direction());
        assertEquals(customer.getId(), draft.legs().get(1).accountId());
        assertEquals(money, draft.legs().get(0).money());
    }

    @Test
    void transferRejectsSameAccountInsufficientFundsAndCurrencyMismatch() {
        Account from = funded("USD", "40.00");
        Account eur = funded("EUR", "40.00");

        assertThrows(InvalidTransferException.class,
                () -> PostingRules.transfer(from, from, Money.of(new BigDecimal("1.00"), "USD")));
        assertThrows(InsufficientFundsException.class,
                () -> PostingRules.transfer(from, funded("USD", "0.00"), Money.of(new BigDecimal("40.01"), "USD")));
        assertThrows(CurrencyMismatchException.class,
                () -> PostingRules.transfer(from, eur, Money.of(new BigDecimal("1.00"), "USD")));
    }

    @Test
    void unbalancedJournalIsRejected() {
        Account customer = funded("USD", "10.00");
        Money money = Money.of(new BigDecimal("1.00"), "USD");
        assertThrows(IllegalStateException.class, () -> new JournalDraft(JournalType.DEPOSIT, List.of(
                new Leg(customer.getId(), EntryDirection.DEBIT, money),
                new Leg(customer.getId(), EntryDirection.DEBIT, money))));
    }

    private static Account funded(String currency, String amount) {
        Account account = Account.customer(UUID.randomUUID(), currency + " wallet", currency, Instant.now());
        if (new BigDecimal(amount).signum() > 0) {
            account.apply(EntryDirection.CREDIT, new BigDecimal(amount));
        }
        return account;
    }
}
