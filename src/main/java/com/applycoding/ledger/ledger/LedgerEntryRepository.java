package com.applycoding.ledger.ledger;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByAccountId(UUID accountId, Pageable pageable);

    List<LedgerEntry> findByJournalIdIn(Collection<UUID> journalIds);
}
