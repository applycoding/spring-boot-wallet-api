package com.applycoding.ledger.account;

import com.applycoding.ledger.ledger.TransactionPage;
import com.applycoding.ledger.ledger.TransactionQuery;
import com.applycoding.ledger.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;
    private final TransactionQuery transactionQuery;

    public AccountController(AccountService accountService, TransactionQuery transactionQuery) {
        this.accountService = accountService;
        this.transactionQuery = transactionQuery;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(CurrentUser.id(), request);
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.list(CurrentUser.id());
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return accountService.get(CurrentUser.id(), id);
    }

    @GetMapping("/{id}/transactions")
    public TransactionPage transactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionQuery.history(CurrentUser.id(), id, page, size);
    }
}
