package com.applycoding.ledger.ledger;

import com.applycoding.ledger.account.Account;
import com.applycoding.ledger.account.AccountRepository;
import com.applycoding.ledger.account.AccountType;
import com.applycoding.ledger.web.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionQuery {

    private final AccountRepository accounts;
    private final JournalRepository journals;
    private final LedgerEntryRepository entries;

    public TransactionQuery(AccountRepository accounts, JournalRepository journals, LedgerEntryRepository entries) {
        this.accounts = accounts;
        this.journals = journals;
        this.entries = entries;
    }

    @Transactional(readOnly = true)
    public TransactionPage history(UUID userId, UUID accountId, int page, int size) {
        Account account = accounts.findByIdAndOwnerId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<LedgerEntry> result = entries.findByAccountId(
                account.getId(),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "postingSequence")));
        if (result.isEmpty()) {
            return new TransactionPage(List.of(), result.getNumber(), result.getSize(), 0, 0);
        }

        List<UUID> journalIds = result.stream().map(LedgerEntry::getJournalId).distinct().toList();
        Map<UUID, Journal> journalById = journals.findAllById(journalIds).stream()
                .collect(Collectors.toMap(Journal::getId, Function.identity()));
        Map<UUID, List<LedgerEntry>> entriesByJournal = entries.findByJournalIdIn(journalIds).stream()
                .collect(Collectors.groupingBy(LedgerEntry::getJournalId));

        List<TransactionView> views = result.stream()
                .map(entry -> toView(entry, journalById.get(entry.getJournalId()), entriesByJournal))
                .toList();
        return new TransactionPage(
                views,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private TransactionView toView(
            LedgerEntry entry,
            Journal journal,
            Map<UUID, List<LedgerEntry>> entriesByJournal) {
        UUID counterparty = null;
        if (journal.getJournalType() == JournalType.TRANSFER) {
            counterparty = entriesByJournal.getOrDefault(journal.getId(), List.of()).stream()
                    .map(LedgerEntry::getAccountId)
                    .filter(id -> !id.equals(entry.getAccountId()))
                    .findFirst()
                    .orElse(null);
            if (counterparty != null && isClearing(counterparty)) {
                counterparty = null;
            }
        }
        return new TransactionView(
                journal.getId(),
                journal.getJournalType().name(),
                entry.getDirection().name(),
                CurrencySupport.format(entry.getAmount(), entry.getCurrency()),
                entry.getCurrency(),
                CurrencySupport.format(entry.getBalanceAfter(), entry.getCurrency()),
                counterparty,
                entry.getCreatedAt());
    }

    private boolean isClearing(UUID accountId) {
        return accounts.findById(accountId)
                .map(account -> account.getAccountType() == AccountType.CLEARING)
                .orElse(false);
    }
}
