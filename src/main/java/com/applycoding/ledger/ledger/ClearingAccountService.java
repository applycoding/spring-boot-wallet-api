package com.applycoding.ledger.ledger;

import com.applycoding.ledger.account.Account;
import com.applycoding.ledger.account.AccountRepository;
import com.applycoding.ledger.account.AccountType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ClearingAccountService {

    private final AccountRepository accounts;
    private final ClearingAccountWriter writer;

    public ClearingAccountService(AccountRepository accounts, ClearingAccountWriter writer) {
        this.accounts = accounts;
        this.writer = writer;
    }

    public Account getOrCreate(String currency) {
        try {
            return writer.createIfAbsent(currency);
        } catch (DataIntegrityViolationException ex) {
            return accounts.findByAccountTypeAndCurrency(AccountType.CLEARING, currency)
                    .orElseThrow(() -> ex);
        }
    }
}
