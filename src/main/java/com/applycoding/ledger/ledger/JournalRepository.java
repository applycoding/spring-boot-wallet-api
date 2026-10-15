package com.applycoding.ledger.ledger;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JournalRepository extends JpaRepository<Journal, UUID> {

    @Query(value = "select nextval('journal_sequence')", nativeQuery = true)
    Number nextJournalSequence();
}
