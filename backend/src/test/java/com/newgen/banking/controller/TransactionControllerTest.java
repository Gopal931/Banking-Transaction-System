package com.newgen.banking.controller;

import com.newgen.banking.dto.DepositRequest;
import com.newgen.banking.dto.TransferRequest;
import com.newgen.banking.dto.WithdrawRequest;
import com.newgen.banking.exception.GlobalExceptionHandler;
import com.newgen.banking.exception.InvalidRequestException;
import com.newgen.banking.model.Account;
import com.newgen.banking.model.BankTransaction;
import com.newgen.banking.model.Customer;
import com.newgen.banking.model.TransactionAudit;
import com.newgen.banking.repository.BankTransactionRepository;
import com.newgen.banking.repository.TransactionAuditRepository;
import com.newgen.banking.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionControllerTest {

    private MockMvc mockMvc;
    private AccountControllerTest.InMemoryAccountRepository accountRepo;
    private InMemoryBankTransactionRepository txRepo;
    private InMemoryTransactionAuditRepository auditRepo;
    private TransactionService transactionService;
    private Account activeAccount;
    private Account targetActiveAccount;
    private Account frozenAccount;

    @BeforeEach
    void setUp() {
        accountRepo = new AccountControllerTest.InMemoryAccountRepository();
        txRepo = new InMemoryBankTransactionRepository();
        auditRepo = new InMemoryTransactionAuditRepository();
        transactionService = new TransactionService(accountRepo, txRepo, auditRepo);

        Customer owner = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
        owner.setId(1L);
        activeAccount = accountRepo.save(new Account(owner, "ACCT-CHK-8921", "CHECKING", new BigDecimal("1000.00"), "USD", "ACTIVE"));
        targetActiveAccount = accountRepo.save(new Account(owner, "ACCT-SAV-3410", "SAVINGS", new BigDecimal("500.00"), "USD", "ACTIVE"));
        frozenAccount = accountRepo.save(new Account(owner, "ACCT-FRZ-9999", "CHECKING", new BigDecimal("500.00"), "USD", "FROZEN"));

        TransactionController controller = new TransactionController(transactionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. Successful deposit returns 201 CREATED, updates balance, and creates transaction and CREDIT audit")
    void testDepositSuccess() throws Exception {
        String payload = """
                {
                  "accountId": %d,
                  "amount": 250.50,
                  "description": "Payroll deposit"
                }
                """.formatted(activeAccount.getId());

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.transactionReference").value(org.hamcrest.Matchers.startsWith("TXN-")))
                .andExpect(jsonPath("$.sourceAccountId").doesNotExist())
                .andExpect(jsonPath("$.targetAccountId").value(activeAccount.getId()))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(250.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.description").value("Payroll deposit"))
                .andExpect(jsonPath("$.updatedBalance").value(1250.50));

        assertThat(accountRepo.findById(activeAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("1250.50");
        assertThat(txRepo.findAll()).hasSize(1);
        BankTransaction savedTx = txRepo.findAll().get(0);
        assertThat(savedTx.getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(savedTx.getSourceAccount()).isNull();
        assertThat(savedTx.getTargetAccount().getId()).isEqualTo(activeAccount.getId());

        assertThat(auditRepo.findAll()).hasSize(1);
        TransactionAudit audit = auditRepo.findAll().get(0);
        assertThat(audit.getActionType()).isEqualTo("CREDIT");
        assertThat(audit.getTransaction().getId()).isEqualTo(savedTx.getId());
        assertThat(audit.getAccount().getId()).isEqualTo(activeAccount.getId());
        assertThat(audit.getAmount()).isEqualByComparingTo("250.50");
        assertThat(audit.getBalanceBefore()).isEqualByComparingTo("1000.00");
        assertThat(audit.getBalanceAfter()).isEqualByComparingTo("1250.50");
    }

    @Test
    @DisplayName("2. Deposit to nonexistent account returns 404 NOT FOUND")
    void testDepositNonexistentAccount() throws Exception {
        String payload = """
                {
                  "accountId": 99999,
                  "amount": 100.00
                }
                """;

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found with id: 99999"));
    }

    @Test
    @DisplayName("3. Deposit to inactive account returns 409 CONFLICT")
    void testDepositInactiveAccount() throws Exception {
        String payload = """
                {
                  "accountId": %d,
                  "amount": 100.00
                }
                """.formatted(frozenAccount.getId());

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not active")));
    }

    @Test
    @DisplayName("4. Deposit with zero amount returns 400 BAD REQUEST")
    void testDepositZeroAmount() throws Exception {
        String payload = """
                {
                  "accountId": %d,
                  "amount": 0.00
                }
                """.formatted(activeAccount.getId());

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.amount").value("Deposit amount must be greater than zero"));
    }

    @Test
    @DisplayName("5. Deposit with negative amount returns 400 BAD REQUEST")
    void testDepositNegativeAmount() throws Exception {
        String payload = """
                {
                  "accountId": %d,
                  "amount": -25.00
                }
                """.formatted(activeAccount.getId());

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.amount").value("Deposit amount must be greater than zero"));
    }

    @Test
    @DisplayName("6. Deposit with null amount returns 400 BAD REQUEST and service rejects null amount")
    void testDepositNullAmount() throws Exception {
        String payload = """
                {
                  "accountId": %d
                }
                """.formatted(activeAccount.getId());

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.amount").value("Amount is required"));

        assertThatThrownBy(() -> transactionService.deposit(new DepositRequest(activeAccount.getId(), null, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("7. Successful withdrawal returns 201 CREATED, deducts balance, and creates DEBIT audit")
    void testWithdrawSuccess() throws Exception {
        String payload = """
                {
                  "accountId": %d,
                  "amount": 250.00,
                  "description": "ATM withdrawal"
                }
                """.formatted(activeAccount.getId());

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.transactionReference").value(org.hamcrest.Matchers.startsWith("TXN-")))
                .andExpect(jsonPath("$.sourceAccountId").value(activeAccount.getId()))
                .andExpect(jsonPath("$.targetAccountId").doesNotExist())
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.description").value("ATM withdrawal"))
                .andExpect(jsonPath("$.updatedBalance").value(750.00));

        assertThat(accountRepo.findById(activeAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("750.00");
        assertThat(txRepo.findAll()).hasSize(1);
        BankTransaction savedTx = txRepo.findAll().get(0);
        assertThat(savedTx.getTransactionType()).isEqualTo("WITHDRAWAL");
        assertThat(savedTx.getSourceAccount().getId()).isEqualTo(activeAccount.getId());
        assertThat(savedTx.getTargetAccount()).isNull();

        assertThat(auditRepo.findAll()).hasSize(1);
        TransactionAudit audit = auditRepo.findAll().get(0);
        assertThat(audit.getActionType()).isEqualTo("DEBIT");
        assertThat(audit.getBalanceBefore()).isEqualByComparingTo("1000.00");
        assertThat(audit.getBalanceAfter()).isEqualByComparingTo("750.00");
    }

    @Test
    @DisplayName("8. Exact-balance withdrawal leaves 0.00 balance")
    void testWithdrawExactBalance() throws Exception {
        String payload = """
                {
                  "accountId": %d,
                  "amount": 1000.00
                }
                """.formatted(activeAccount.getId());

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.updatedBalance").value(0.00));

        assertThat(accountRepo.findById(activeAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("9. Withdrawal with insufficient balance returns 400 BAD REQUEST and leaves balance unchanged")
    void testWithdrawInsufficientBalance() throws Exception {
        String payload = """
                {
                  "accountId": %d,
                  "amount": 1500.00
                }
                """.formatted(activeAccount.getId());

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient funds")));

        assertThat(accountRepo.findById(activeAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("10. Withdrawal from nonexistent account returns 404 NOT FOUND")
    void testWithdrawNonexistentAccount() throws Exception {
        String payload = """
                {
                  "accountId": 99999,
                  "amount": 50.00
                }
                """;

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("11. Withdrawal from inactive account returns 409 CONFLICT")
    void testWithdrawInactiveAccount() throws Exception {
        String payload = """
                {
                  "accountId": %d,
                  "amount": 50.00
                }
                """.formatted(frozenAccount.getId());

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("12. Withdrawal with zero, negative, or null amount returns 400 BAD REQUEST")
    void testWithdrawInvalidAmounts() throws Exception {
        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\": " + activeAccount.getId() + ", \"amount\": 0.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").value("Withdrawal amount must be greater than zero"));

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\": " + activeAccount.getId() + ", \"amount\": -10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").value("Withdrawal amount must be greater than zero"));

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\": " + activeAccount.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").value("Amount is required"));

        assertThatThrownBy(() -> transactionService.withdraw(new WithdrawRequest(activeAccount.getId(), null, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("13. Successful transfer returns 201 CREATED, updates both balances, and creates 1 tx + DEBIT & CREDIT audits")
    void testTransferSuccess() throws Exception {
        String payload = """
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 300.00,
                  "description": "Savings transfer"
                }
                """.formatted(activeAccount.getId(), targetActiveAccount.getId());

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.transactionReference").value(org.hamcrest.Matchers.startsWith("TXN-")))
                .andExpect(jsonPath("$.sourceAccountId").value(activeAccount.getId()))
                .andExpect(jsonPath("$.targetAccountId").value(targetActiveAccount.getId()))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.description").value("Savings transfer"))
                .andExpect(jsonPath("$.updatedBalance").value(700.00));

        assertThat(accountRepo.findById(activeAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("700.00");
        assertThat(accountRepo.findById(targetActiveAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("800.00");

        assertThat(txRepo.findAll()).hasSize(1);
        BankTransaction savedTx = txRepo.findAll().get(0);
        assertThat(savedTx.getTransactionType()).isEqualTo("TRANSFER");

        List<TransactionAudit> audits = auditRepo.findByTransactionId(savedTx.getId());
        assertThat(audits).hasSize(2);
        assertThat(audits).anyMatch(a -> "DEBIT".equals(a.getActionType())
                && a.getAccount().getId().equals(activeAccount.getId())
                && a.getBalanceBefore().compareTo(new BigDecimal("1000.00")) == 0
                && a.getBalanceAfter().compareTo(new BigDecimal("700.00")) == 0);
        assertThat(audits).anyMatch(a -> "CREDIT".equals(a.getActionType())
                && a.getAccount().getId().equals(targetActiveAccount.getId())
                && a.getBalanceBefore().compareTo(new BigDecimal("500.00")) == 0
                && a.getBalanceAfter().compareTo(new BigDecimal("800.00")) == 0);
    }

    @Test
    @DisplayName("14. Exact-balance transfer leaves source at 0.00 and adds full amount to target")
    void testTransferExactBalance() throws Exception {
        String payload = """
                {
                  "sourceAccountId": %d,
                  "targetAccountId": %d,
                  "amount": 1000.00
                }
                """.formatted(activeAccount.getId(), targetActiveAccount.getId());

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.updatedBalance").value(0.00));

        assertThat(accountRepo.findById(activeAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("0.00");
        assertThat(accountRepo.findById(targetActiveAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("15. Transfer fails with 404 when source or target account does not exist")
    void testTransferNonexistentAccounts() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": 99999, \"targetAccountId\": " + targetActiveAccount.getId() + ", \"amount\": 50.00}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + activeAccount.getId() + ", \"targetAccountId\": 99999, \"amount\": 50.00}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("16. Transfer fails with 409 when source or target account is inactive")
    void testTransferInactiveAccounts() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + frozenAccount.getId() + ", \"targetAccountId\": " + targetActiveAccount.getId() + ", \"amount\": 50.00}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + activeAccount.getId() + ", \"targetAccountId\": " + frozenAccount.getId() + ", \"amount\": 50.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("17. Transfer fails with 400 for same account, currency mismatch, insufficient funds, and invalid amounts")
    void testTransferValidationFailures() throws Exception {
        // Same source and target
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + activeAccount.getId() + ", \"targetAccountId\": " + activeAccount.getId() + ", \"amount\": 50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Source and target accounts must be different"));

        // Insufficient source balance
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + activeAccount.getId() + ", \"targetAccountId\": " + targetActiveAccount.getId() + ", \"amount\": 5000.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient funds")));

        // Zero, negative, and null amounts
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + activeAccount.getId() + ", \"targetAccountId\": " + targetActiveAccount.getId() + ", \"amount\": 0.00}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + activeAccount.getId() + ", \"targetAccountId\": " + targetActiveAccount.getId() + ", \"amount\": -10.00}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + activeAccount.getId() + ", \"targetAccountId\": " + targetActiveAccount.getId() + "}"))
                .andExpect(status().isBadRequest());

        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(activeAccount.getId(), targetActiveAccount.getId(), null, null)))
                .isInstanceOf(InvalidRequestException.class);

        // Currency mismatch
        Customer owner = activeAccount.getCustomer();
        Account eurAccount = accountRepo.save(new Account(owner, "ACCT-EUR-7777", "CHECKING", new BigDecimal("200.00"), "EUR", "ACTIVE"));
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\": " + activeAccount.getId() + ", \"targetAccountId\": " + eurAccount.getId() + ", \"amount\": 50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Currency mismatch")));

        // Verify balances remained untouched after all rejected transfers
        assertThat(accountRepo.findById(activeAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(accountRepo.findById(targetActiveAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("18. Retrieve DEPOSIT, WITHDRAWAL, and TRANSFER transactions by ID and reference")
    void testRetrieveTransactionsByIdAndReference() throws Exception {
        BankTransaction deposit = txRepo.save(new BankTransaction(
                "TXN-DEP-001", null, activeAccount, "DEPOSIT",
                new BigDecimal("200.00"), "USD", "SUCCESS", "Deposit ref test"));
        BankTransaction withdrawal = txRepo.save(new BankTransaction(
                "TXN-WDR-002", activeAccount, null, "WITHDRAWAL",
                new BigDecimal("100.00"), "USD", "SUCCESS", "Withdrawal ref test"));
        BankTransaction transfer = txRepo.save(new BankTransaction(
                "TXN-TRF-003", activeAccount, targetActiveAccount, "TRANSFER",
                new BigDecimal("150.00"), "USD", "SUCCESS", "Transfer ref test"));

        // Deposit by ID (sourceAccountId null)
        mockMvc.perform(get("/api/transactions/" + deposit.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deposit.getId()))
                .andExpect(jsonPath("$.transactionReference").value("TXN-DEP-001"))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.sourceAccountId").doesNotExist())
                .andExpect(jsonPath("$.targetAccountId").value(activeAccount.getId()))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Withdrawal by reference (targetAccountId null)
        mockMvc.perform(get("/api/transactions/reference/TXN-WDR-002").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(withdrawal.getId()))
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.sourceAccountId").value(activeAccount.getId()))
                .andExpect(jsonPath("$.targetAccountId").doesNotExist());

        // Transfer by ID and reference (both source and target present)
        mockMvc.perform(get("/api/transactions/" + transfer.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceAccountId").value(activeAccount.getId()))
                .andExpect(jsonPath("$.targetAccountId").value(targetActiveAccount.getId()));

        mockMvc.perform(get("/api/transactions/reference/TXN-TRF-003").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transfer.getId()))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"));
    }

    @Test
    @DisplayName("19. Retrieve nonexistent transaction by ID or reference returns 404, blank reference returns 400")
    void testRetrieveTransactionNotFoundAndBlankReference() throws Exception {
        mockMvc.perform(get("/api/transactions/99999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Transaction not found with id: 99999"));

        mockMvc.perform(get("/api/transactions/reference/TXN-MISSING-999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Transaction not found with reference: TXN-MISSING-999"));

        mockMvc.perform(get("/api/transactions/reference/   ").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Transaction reference must not be blank"));

        assertThatThrownBy(() -> transactionService.getTransactionByReference(" "))
                .isInstanceOf(InvalidRequestException.class);
    }

    static class InMemoryBankTransactionRepository implements BankTransactionRepository {
        private final Map<Long, BankTransaction> store = new ConcurrentHashMap<>();
        private long idSequence = 0L;

        @Override
        public Optional<BankTransaction> findByTransactionReference(String transactionReference) {
            return store.values().stream()
                    .filter(t -> transactionReference.equals(t.getTransactionReference()))
                    .findFirst();
        }

        @Override
        public List<BankTransaction> findByStatus(String status) {
            return store.values().stream().filter(t -> status.equals(t.getStatus())).toList();
        }

        @Override
        public List<BankTransaction> findByTransactionType(String transactionType) {
            return store.values().stream().filter(t -> transactionType.equals(t.getTransactionType())).toList();
        }

        @Override
        public Page<BankTransaction> findByAccountId(Long accountId, Pageable pageable) {
            List<BankTransaction> matching = store.values().stream()
                    .filter(t -> (t.getSourceAccount() != null && accountId.equals(t.getSourceAccount().getId()))
                            || (t.getTargetAccount() != null && accountId.equals(t.getTargetAccount().getId())))
                    .sorted(Comparator.comparing(BankTransaction::getCreatedAt).reversed()
                            .thenComparing(Comparator.comparing(BankTransaction::getId).reversed()))
                    .toList();

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), matching.size());
            List<BankTransaction> slice = start >= matching.size() ? Collections.emptyList() : matching.subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(slice, pageable, matching.size());
        }

        @Override
        public <S extends BankTransaction> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(++idSequence);
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<BankTransaction> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
        @Override public List<BankTransaction> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<BankTransaction> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(BankTransaction entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends BankTransaction> entities) {}
        @Override public void deleteAll() { store.clear(); }
        @Override public <S extends BankTransaction> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void flush() {}
        @Override public <S extends BankTransaction> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends BankTransaction> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<BankTransaction> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public BankTransaction getOne(Long id) { return store.get(id); }
        @Override public BankTransaction getById(Long id) { return store.get(id); }
        @Override public BankTransaction getReferenceById(Long id) { return store.get(id); }
        @Override public <S extends BankTransaction> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends BankTransaction> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends BankTransaction> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends BankTransaction> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends BankTransaction> long count(Example<S> example) { return 0; }
        @Override public <S extends BankTransaction> boolean exists(Example<S> example) { return false; }
        @Override public <S extends BankTransaction, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<BankTransaction> findAll(Sort sort) { return findAll(); }
        @Override public Page<BankTransaction> findAll(Pageable pageable) { return Page.empty(); }
    }

    static class InMemoryTransactionAuditRepository implements TransactionAuditRepository {
        private final Map<Long, TransactionAudit> store = new ConcurrentHashMap<>();
        private long idSequence = 0L;

        @Override
        public List<TransactionAudit> findByAccountId(Long accountId) {
            return store.values().stream()
                    .filter(a -> a.getAccount() != null && accountId.equals(a.getAccount().getId()))
                    .toList();
        }

        @Override
        public List<TransactionAudit> findByTransactionId(Long transactionId) {
            return store.values().stream()
                    .filter(a -> a.getTransaction() != null && transactionId.equals(a.getTransaction().getId()))
                    .toList();
        }

        @Override
        public <S extends TransactionAudit> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(++idSequence);
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<TransactionAudit> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
        @Override public List<TransactionAudit> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<TransactionAudit> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(TransactionAudit entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends TransactionAudit> entities) {}
        @Override public void deleteAll() { store.clear(); }
        @Override public <S extends TransactionAudit> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void flush() {}
        @Override public <S extends TransactionAudit> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends TransactionAudit> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<TransactionAudit> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public TransactionAudit getOne(Long id) { return store.get(id); }
        @Override public TransactionAudit getById(Long id) { return store.get(id); }
        @Override public TransactionAudit getReferenceById(Long id) { return store.get(id); }
        @Override public <S extends TransactionAudit> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends TransactionAudit> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends TransactionAudit> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends TransactionAudit> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends TransactionAudit> long count(Example<S> example) { return 0; }
        @Override public <S extends TransactionAudit> boolean exists(Example<S> example) { return false; }
        @Override public <S extends TransactionAudit, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<TransactionAudit> findAll(Sort sort) { return findAll(); }
        @Override public Page<TransactionAudit> findAll(Pageable pageable) { return Page.empty(); }
    }
}
