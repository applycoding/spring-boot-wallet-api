package com.applycoding.ledger.ledger;

import com.applycoding.ledger.account.Account;
import com.applycoding.ledger.account.AccountType;
import java.util.List;

public final class PostingRules {

    private PostingRules() {
    }

    public static JournalDraft deposit(Account clearing, Account customer, Money money) {
        requireType(clearing, AccountType.CLEARING);
        requireType(customer, AccountType.CUSTOMER);
        requireCurrency(clearing, money);
        requireCurrency(customer, money);
        return new JournalDraft(JournalType.DEPOSIT, List.of(
                new Leg(clearing.getId(), EntryDirection.DEBIT, money),
                new Leg(customer.getId(), EntryDirection.CREDIT, money)));
    }

    public static JournalDraft transfer(Account from, Account to, Money money) {
        requireType(from, AccountType.CUSTOMER);
        requireType(to, AccountType.CUSTOMER);
        requireCurrency(from, money);
        requireCurrency(to, money);
        if (from.getId().equals(to.getId())) {
            throw new InvalidTransferException("Cannot transfer to the same account");
        }
        if (from.getBalance().compareTo(money.amount()) < 0) {
            throw new InsufficientFundsException(from.getId());
        }
        return new JournalDraft(JournalType.TRANSFER, List.of(
                new Leg(from.getId(), EntryDirection.DEBIT, money),
                new Leg(to.getId(), EntryDirection.CREDIT, money)));
    }

    private static void requireType(Account account, AccountType expected) {
        if (account.getAccountType() != expected) {
            throw new InvalidTransferException("Account " + account.getId() + " must be " + expected);
        }
    }

    private static void requireCurrency(Account account, Money money) {
        if (!account.getCurrency().equals(money.currency())) {
            throw new CurrencyMismatchException(account.getCurrency(), money.currency());
        }
    }
}
