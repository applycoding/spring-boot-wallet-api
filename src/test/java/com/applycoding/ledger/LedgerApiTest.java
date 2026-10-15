package com.applycoding.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.applycoding.ledger.account.AccountRepository;
import com.applycoding.ledger.account.AccountType;
import com.applycoding.ledger.ledger.InsufficientFundsException;
import com.applycoding.ledger.ledger.LedgerEntryRepository;
import com.applycoding.ledger.ledger.LedgerService;
import com.applycoding.ledger.ledger.TransferRequest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LedgerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private LedgerEntryRepository entries;

    @Test
    void depositsAndTransfersKeepABalancedLedger() throws Exception {
        String payerEmail = email();
        String payeeEmail = email();
        register(payerEmail);
        String payer = login(payerEmail);
        String payee = registerAndLogin(payeeEmail);

        UUID payerAccount = createAccount(payer, "USD", "Everyday");
        UUID payeeAccount = createAccount(payee, "USD", "Savings");
        BigDecimal clearingBefore = clearingBalance("USD");

        MvcResult deposit = mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(payer))
                        .header("Idempotency-Key", "deposit-happy-path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(payerAccount, "100.00", "USD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value("100.00"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();
        String transactionId = JsonPath.read(deposit.getResponse().getContentAsString(), "$.transactionId");

        mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(payer))
                        .header("Idempotency-Key", "transfer-happy-path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(payerAccount, payeeAccount, "25.00", "USD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromBalance").value("75.00"))
                .andExpect(jsonPath("$.toBalance").value("25.00"));

        mockMvc.perform(get("/api/v1/accounts/{id}", payerAccount)
                        .header(HttpHeaders.AUTHORIZATION, bearer(payer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("75.00"));

        mockMvc.perform(get("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(payer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/accounts/{id}/transactions", payerAccount)
                        .header(HttpHeaders.AUTHORIZATION, bearer(payer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.content[0].amount").value("25.00"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value("75.00"))
                .andExpect(jsonPath("$.content[0].counterpartyAccountId").value(payeeAccount.toString()))
                .andExpect(jsonPath("$.content[1].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[1].direction").value("CREDIT"))
                .andExpect(jsonPath("$.content[1].counterpartyAccountId").value(nullValue()));

        long legs = entries.findByJournalIdIn(List.of(UUID.fromString(transactionId))).size();
        assertEquals(2, legs);
        assertEquals(0, clearingBalance("USD").subtract(clearingBefore).compareTo(new BigDecimal("100.0000")));
    }

    @Test
    void idempotencyKeyReplaysSuccessAndConflictsOnADifferentBody() throws Exception {
        String token = registerAndLogin(email());
        UUID accountId = createAccount(token, "USD", "Idempotent");

        MvcResult first = mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "same-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(accountId, "40.00", "USD")))
                .andExpect(status().isCreated())
                .andReturn();
        String transactionId = JsonPath.read(first.getResponse().getContentAsString(), "$.transactionId");

        mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "same-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(accountId, "40.00", "USD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.balance").value("40.00"));

        mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "same-key-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(accountId, "41.00", "USD")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.balance").value("40.00"));
    }

    @Test
    void rejectsInvalidMoneyInsufficientFundsAndCrossCurrencyTransfers() throws Exception {
        String token = registerAndLogin(email());
        String other = registerAndLogin(email());
        UUID usd = createAccount(token, "USD", "USD");
        UUID eur = createAccount(other, "EUR", "EUR");
        createAccount(token, "JPY", "Yen");

        mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(usd, "10.00", "USD")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "yen-deposit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","amount":"1500","currency":"JPY"}
                                """.formatted(accountIdByName(token, "Yen"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value("1500"));

        mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "bad-currency")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(usd, "1.00", "EUR")))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "seed-usd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(usd, "10.00", "USD")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "too-much")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(usd, eur, "10.00", "USD")))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "overdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(usd, usd, "11.00", "USD")))
                .andExpect(status().isBadRequest());

        UUID second = createAccount(token, "USD", "Second");
        mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "overdraw-other")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(usd, second, "11.00", "USD")))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "overdraw-other")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(usd, second, "4.00", "USD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromBalance").value("6.00"));

        mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"XYZ\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "zero-amount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(usd, "0.00", "USD")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hidesOtherUsersAccountsAndRequiresAToken() throws Exception {
        String owner = registerAndLogin(email());
        String stranger = registerAndLogin(email());
        UUID accountId = createAccount(owner, "GBP", "Pounds");

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(stranger)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(stranger))
                        .header("Idempotency-Key", "not-your-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(accountId, "5.00", "GBP")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"short"}
                                """.formatted(email())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.com","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationConflicts() throws Exception {
        String address = email();
        register(address);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(address)))
                .andExpect(status().isConflict());
    }

    @Test
    void openApiAndHealthArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Ledger API"));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void concurrentTransfersCannotOverdraw() throws Exception {
        String address = email();
        UUID userId = register(address);
        String token = login(address);
        UUID source = createAccount(token, "USD", "Source");
        UUID first = createAccount(token, "USD", "First");
        UUID second = createAccount(token, "USD", "Second");
        mockMvc.perform(post("/api/v1/deposits")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .header("Idempotency-Key", "seed-concurrent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositBody(source, "100.00", "USD")))
                .andExpect(status().isCreated());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        try {
            for (UUID destination : List.of(first, second)) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("timed out waiting to start");
                    }
                    try {
                        ledgerService.transfer(
                                userId,
                                new TransferRequest(source, destination, new BigDecimal("60.00"), "USD"),
                                UUID.randomUUID().toString());
                        return "ok";
                    } catch (RuntimeException ex) {
                        if (causedBy(ex, InsufficientFundsException.class)
                                || causedBy(ex, OptimisticLockingFailureException.class)) {
                            return "conflict";
                        }
                        throw ex;
                    }
                }));
            }
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("workers did not start");
            }
            start.countDown();
            long successes = 0;
            for (Future<String> result : results) {
                if ("ok".equals(result.get(10, TimeUnit.SECONDS))) {
                    successes++;
                }
            }
            assertEquals(1, successes);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(0, accounts.findById(source).orElseThrow().getBalance().compareTo(new BigDecimal("40")));
        BigDecimal received = accounts.findById(first).orElseThrow().getBalance()
                .add(accounts.findById(second).orElseThrow().getBalance());
        assertEquals(0, received.compareTo(new BigDecimal("60")));
    }

    private UUID register(String address) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(address)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.userId"));
    }

    private String registerAndLogin(String address) throws Exception {
        register(address);
        return login(address);
    }

    private String login(String address) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(address)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private UUID createAccount(String token, String currency, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"%s","name":"%s"}
                                """.formatted(currency, name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(fractionZero(currency)))
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private String accountIdByName(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        List<String> ids = JsonPath.read(result.getResponse().getContentAsString(),
                "$.[?(@.name == '" + name + "')].id");
        return ids.getFirst();
    }

    private BigDecimal clearingBalance(String currency) {
        return accounts.findByAccountTypeAndCurrency(AccountType.CLEARING, currency)
                .map(account -> account.getBalance())
                .orElse(BigDecimal.ZERO);
    }

    private static String depositBody(UUID accountId, String amount, String currency) {
        return """
                {"accountId":"%s","amount":"%s","currency":"%s"}
                """.formatted(accountId, amount, currency);
    }

    private static String transferBody(UUID from, UUID to, String amount, String currency) {
        return """
                {"fromAccountId":"%s","toAccountId":"%s","amount":"%s","currency":"%s"}
                """.formatted(from, to, amount, currency);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String email() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private static String fractionZero(String currency) {
        int digits = Currency.getInstance(currency).getDefaultFractionDigits();
        if (digits == 0) {
            return "0";
        }
        return "0." + "0".repeat(digits);
    }

    private static boolean causedBy(Throwable ex, Class<? extends Throwable> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
