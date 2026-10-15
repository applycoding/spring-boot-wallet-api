package com.applycoding.ledger.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    Optional<Account> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Account> findByAccountTypeAndCurrency(AccountType accountType, String currency);
}
