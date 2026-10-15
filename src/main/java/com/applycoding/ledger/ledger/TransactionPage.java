package com.applycoding.ledger.ledger;

import java.util.List;

public record TransactionPage(
        List<TransactionView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
