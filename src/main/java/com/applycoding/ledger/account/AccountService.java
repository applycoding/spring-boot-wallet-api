package com.applycoding.ledger.account;

import com.applycoding.ledger.ledger.CurrencySupport;
import com.applycoding.ledger.web.ResourceNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final Clock clock;

    public AccountService(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse create(UUID userId, CreateAccountRequest request) {
        String currency = CurrencySupport.requireIsoCode(request.currency());
        String name = request.name() == null || request.name().isBlank()
                ? currency + " wallet"
                : request.name().trim();
        Account account = accounts.save(Account.customer(userId, name, currency, clock.instant()));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(UUID userId) {
        return accounts.findByOwnerIdOrderByCreatedAtAsc(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID userId, UUID accountId) {
        return AccountResponse.from(requireOwned(userId, accountId));
    }

    private Account requireOwned(UUID userId, UUID accountId) {
        return accounts.findByIdAndOwnerId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }
}
