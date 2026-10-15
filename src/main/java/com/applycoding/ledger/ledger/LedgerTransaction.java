package com.applycoding.ledger.ledger;

import com.applycoding.ledger.account.Account;
import com.applycoding.ledger.account.AccountRepository;
import com.applycoding.ledger.account.AccountType;
import com.applycoding.ledger.web.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerTransaction {

    private final AccountRepository accounts;
    private final JournalRepository journals;
    private final LedgerEntryRepository entries;
    private final IdempotencyRepository idempotency;
    private final ClearingAccountService clearingAccounts;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LedgerTransaction(
            AccountRepository accounts,
            JournalRepository journals,
            LedgerEntryRepository entries,
            IdempotencyRepository idempotency,
            ClearingAccountService clearingAccounts,
            ObjectMapper objectMapper,
            Clock clock) {
        this.accounts = accounts;
        this.journals = journals;
        this.entries = entries;
        this.idempotency = idempotency;
        this.clearingAccounts = clearingAccounts;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public DepositResponse deposit(UUID userId, DepositCommand command, String idempotencyKey, String requestHash) {
        Optional<DepositResponse> replay = replay(userId, idempotencyKey, requestHash, DepositResponse.class);
        if (replay.isPresent()) {
            return replay.get();
        }

        Account customer = accounts.findByIdAndOwnerId(command.accountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        if (!customer.getCurrency().equals(command.money().currency())) {
            throw new CurrencyMismatchException(customer.getCurrency(), command.money().currency());
        }

        Account clearing = managedClearing(command.money().currency());
        JournalDraft draft = PostingRules.deposit(clearing, customer, command.money());
        Posted posted = post(draft, userId, List.of(clearing, customer));
        DepositResponse response = new DepositResponse(
                posted.journalId(),
                customer.getId(),
                command.money().toPlainString(),
                command.money().currency(),
                CurrencySupport.format(customer.getBalance(), customer.getCurrency()),
                posted.postedAt());
        store(userId, idempotencyKey, "DEPOSIT", requestHash, response);
        return response;
    }

    @Transactional
    public TransferResponse transfer(UUID userId, TransferCommand command, String idempotencyKey, String requestHash) {
        Optional<TransferResponse> replay = replay(userId, idempotencyKey, requestHash, TransferResponse.class);
        if (replay.isPresent()) {
            return replay.get();
        }

        Account from = accounts.findByIdAndOwnerId(command.fromAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        Account to = accounts.findById(command.toAccountId())
                .filter(account -> account.getAccountType() == AccountType.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));
        if (!from.getCurrency().equals(command.money().currency())) {
            throw new CurrencyMismatchException(from.getCurrency(), command.money().currency());
        }
        if (!to.getCurrency().equals(command.money().currency())) {
            throw new CurrencyMismatchException(to.getCurrency(), command.money().currency());
        }

        JournalDraft draft = PostingRules.transfer(from, to, command.money());
        Posted posted = post(draft, userId, List.of(from, to));
        TransferResponse response = new TransferResponse(
                posted.journalId(),
                from.getId(),
                to.getId(),
                command.money().toPlainString(),
                command.money().currency(),
                CurrencySupport.format(from.getBalance(), from.getCurrency()),
                CurrencySupport.format(to.getBalance(), to.getCurrency()),
                posted.postedAt());
        store(userId, idempotencyKey, "TRANSFER", requestHash, response);
        return response;
    }

    private Account managedClearing(String currency) {
        Account clearing = clearingAccounts.getOrCreate(currency);
        return accounts.findById(clearing.getId())
                .orElseThrow(() -> new IllegalStateException("Clearing account missing for " + currency));
    }

    private Posted post(JournalDraft draft, UUID createdBy, List<Account> participants) {
        Instant postedAt = clock.instant();
        long postingSequence = journals.nextJournalSequence().longValue();
        Journal journal = journals.save(Journal.create(draft.type(), createdBy, postedAt));

        Map<UUID, BigDecimal> balanceAfter = new HashMap<>();
        List<Account> ordered = participants.stream()
                .sorted(Comparator.comparing(Account::getId))
                .toList();
        for (Account account : ordered) {
            for (Leg leg : draft.legs()) {
                if (leg.accountId().equals(account.getId())) {
                    account.apply(leg.direction(), leg.money().amount());
                    balanceAfter.put(account.getId(), account.getBalance());
                }
            }
        }

        List<LedgerEntry> stored = new ArrayList<>();
        for (Leg leg : draft.legs()) {
            stored.add(LedgerEntry.create(
                    journal.getId(),
                    leg.accountId(),
                    leg.direction(),
                    leg.money(),
                    balanceAfter.get(leg.accountId()),
                    postingSequence,
                    postedAt));
        }
        entries.saveAll(stored);
        return new Posted(journal.getId(), postedAt);
    }

    private <T> Optional<T> replay(UUID userId, String idempotencyKey, String requestHash, Class<T> type) {
        Optional<IdempotencyRecord> existing = idempotency.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        IdempotencyDecider.Outcome outcome = IdempotencyDecider.decide(
                existing.map(IdempotencyRecord::getRequestHash), requestHash);
        return switch (outcome) {
            case REPLAY -> Optional.of(IdempotencyResponses.read(existing.orElseThrow(), type, objectMapper));
            case CONFLICT -> throw new IdempotencyConflictException();
            case EXECUTE -> Optional.empty();
        };
    }

    private void store(UUID userId, String idempotencyKey, String operation, String requestHash, Object response) {
        idempotency.saveAndFlush(IdempotencyRecord.completed(
                userId,
                idempotencyKey,
                operation,
                requestHash,
                IdempotencyResponses.write(response, objectMapper),
                clock.instant()));
    }

    private record Posted(UUID journalId, Instant postedAt) {
    }
}
