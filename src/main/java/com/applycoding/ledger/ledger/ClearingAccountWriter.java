package com.applycoding.ledger.ledger;

import com.applycoding.ledger.account.Account;
import com.applycoding.ledger.account.AccountRepository;
import com.applycoding.ledger.account.AccountType;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClearingAccountWriter {

    private final AccountRepository accounts;
    private final ClearingAccountRepository clearingAccounts;
    private final Clock clock;

    public ClearingAccountWriter(AccountRepository accounts, ClearingAccountRepository clearingAccounts, Clock clock) {
        this.accounts = accounts;
        this.clearingAccounts = clearingAccounts;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Account createIfAbsent(String currency) {
        return accounts.findByAccountTypeAndCurrency(AccountType.CLEARING, currency)
                .orElseGet(() -> {
                    Account clearing = accounts.saveAndFlush(Account.clearing(currency, clock.instant()));
                    clearingAccounts.saveAndFlush(new ClearingAccount(currency, clearing.getId()));
                    return clearing;
                });
    }
}
