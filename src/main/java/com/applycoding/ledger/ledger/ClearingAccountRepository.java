package com.applycoding.ledger.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClearingAccountRepository extends JpaRepository<ClearingAccount, String> {
}
