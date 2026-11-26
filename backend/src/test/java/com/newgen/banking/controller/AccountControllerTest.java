package com.newgen.banking.controller;

import com.newgen.banking.exception.GlobalExceptionHandler;
import com.newgen.banking.model.Account;
import com.newgen.banking.model.Customer;
import com.newgen.banking.repository.AccountRepository;
import com.newgen.banking.repository.CustomerRepository;
import com.newgen.banking.service.AccountService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerTest {

    private MockMvc mockMvc;
    private InMemoryAccountRepository accountRepo;

    @BeforeEach
    void setUp() {
        accountRepo = new InMemoryAccountRepository();
        CustomerRepository customerRepo = new CustomerControllerTest.InMemoryCustomerRepository();
        AccountService accountService = new AccountService(accountRepo, customerRepo);
        AccountController accountController = new AccountController(accountService);

        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. Successful account creation returns 201 CREATED")
    void testCreateAccountSuccess() throws Exception {
        String payload = """
                {
                  "customerId": 1,
                  "accountType": "SAVINGS",
                  "initialBalance": 1500.00,
                  "currency": "USD"
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.accountNumber").value(org.hamcrest.Matchers.startsWith("ACCT-SAV-")))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(1500.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("2. Nonexistent customer returns 404 NOT FOUND")
    void testCreateAccountCustomerNotFound() throws Exception {
        String payload = """
                {
                  "customerId": 99999,
                  "accountType": "CHECKING",
                  "initialBalance": 100.00
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 99999"));
    }

    @Test
    @DisplayName("3. Invalid account type returns 400 BAD REQUEST")
    void testCreateAccountInvalidType() throws Exception {
        String payload = """
                {
                  "customerId": 1,
                  "accountType": "CRYPTO_WALLET",
                  "initialBalance": 500.00
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported account type")));
    }

    @Test
    @DisplayName("4. Negative initial balance returns 400 BAD REQUEST")
    void testCreateAccountInvalidInitialBalance() throws Exception {
        String payload = """
                {
                  "customerId": 1,
                  "accountType": "CHECKING",
                  "initialBalance": -50.00
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.initialBalance").value("Initial balance must be zero or positive"));
    }

    @Test
    @DisplayName("5. Unique account numbers are generated across multiple creations")
    void testUniqueAccountNumberGeneration() throws Exception {
        String payload = """
                {
                  "customerId": 1,
                  "accountType": "CHECKING"
                }
                """;

        MvcResult first = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(first.getResponse().getContentAsString())
                .isNotEqualTo(second.getResponse().getContentAsString());
        assertThat(accountRepo.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("6. Account number collision that cannot be resolved returns 409 CONFLICT")
    void testAccountNumberConflictReturns409() throws Exception {
        accountRepo.setForceCollision(true);

        String payload = """
                {
                  "customerId": 1,
                  "accountType": "CHECKING"
                }
                """;

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("7. Retrieve existing account by ID returns 200 OK")
    void testGetAccountByIdSuccess() throws Exception {
        Customer owner = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
        owner.setId(1L);
        Account seeded = new Account(owner, "ACCT-CHK-8921", "CHECKING", new java.math.BigDecimal("12450.25"), "USD", "ACTIVE");
        Account saved = accountRepo.save(seeded);

        mockMvc.perform(get("/api/accounts/" + saved.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("ACCT-CHK-8921"))
                .andExpect(jsonPath("$.accountType").value("CHECKING"))
                .andExpect(jsonPath("$.balance").value(12450.25));
    }

    @Test
    @DisplayName("8. Retrieve nonexistent account by ID returns 404 NOT FOUND")
    void testGetAccountByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/99999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found with id: 99999"));
    }

    @Test
    @DisplayName("9. Retrieve existing account by account number returns 200 OK")
    void testGetAccountByNumberSuccess() throws Exception {
        Customer owner = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
        owner.setId(1L);
        accountRepo.save(new Account(owner, "ACCT-SAV-3410", "SAVINGS", new java.math.BigDecimal("35800.50"), "USD", "ACTIVE"));

        mockMvc.perform(get("/api/accounts/number/ACCT-SAV-3410")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACCT-SAV-3410"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(35800.50));
    }

    @Test
    @DisplayName("10. Retrieve nonexistent account by account number returns 404 NOT FOUND")
    void testGetAccountByNumberNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/number/ACCT-NONE-0000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found with accountNumber: ACCT-NONE-0000"));
    }

    @Test
    @DisplayName("11. Validate active account with matching owner returns 200 OK")
    void testValidateActiveAccountAndOwnershipSuccess() throws Exception {
        Customer owner = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
        owner.setId(1L);
        Account saved = accountRepo.save(new Account(owner, "ACCT-CHK-8921", "CHECKING", new java.math.BigDecimal("12450.25"), "USD", "ACTIVE"));

        mockMvc.perform(get("/api/accounts/" + saved.getId() + "/validate")
                        .param("customerId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("12. Validate nonexistent account returns 404 NOT FOUND")
    void testValidateNonexistentAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/99999/validate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("13. Validate inactive account returns 409 CONFLICT")
    void testValidateInactiveAccountReturns409() throws Exception {
        Customer owner = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
        owner.setId(1L);
        Account saved = accountRepo.save(new Account(owner, "ACCT-FRZ-1111", "CHECKING", new java.math.BigDecimal("100.00"), "USD", "FROZEN"));

        mockMvc.perform(get("/api/accounts/" + saved.getId() + "/validate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not active")));
    }

    @Test
    @DisplayName("14. Validate wrong customer ownership returns 400 BAD REQUEST")
    void testValidateWrongOwnershipReturns400() throws Exception {
        Customer owner = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
        owner.setId(1L);
        Account saved = accountRepo.save(new Account(owner, "ACCT-CHK-2222", "CHECKING", new java.math.BigDecimal("100.00"), "USD", "ACTIVE"));

        mockMvc.perform(get("/api/accounts/" + saved.getId() + "/validate")
                        .param("customerId", "99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("does not belong to customer 99")));
    }

    @Test
    @DisplayName("15. Valid status transition updates status and preserves balance")
    void testValidStatusTransitionPreservesBalance() throws Exception {
        Customer owner = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
        owner.setId(1L);
        Account saved = accountRepo.save(new Account(owner, "ACCT-CHK-3333", "CHECKING", new java.math.BigDecimal("5432.10"), "USD", "ACTIVE"));

        mockMvc.perform(patch("/api/accounts/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"FROZEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"))
                .andExpect(jsonPath("$.balance").value(5432.10));

        mockMvc.perform(patch("/api/accounts/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(5432.10));
    }

    @Test
    @DisplayName("16. Invalid status value returns 400 BAD REQUEST and terminal CLOSED transition returns 409 CONFLICT")
    void testInvalidStatusAndTerminalTransition() throws Exception {
        Customer owner = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
        owner.setId(1L);
        Account saved = accountRepo.save(new Account(owner, "ACCT-CHK-4444", "CHECKING", new java.math.BigDecimal("100.00"), "USD", "CLOSED"));

        mockMvc.perform(patch("/api/accounts/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ARBITRARY_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(patch("/api/accounts/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid status transition")));
    }

    static class InMemoryAccountRepository implements AccountRepository {
        private final Map<Long, Account> store = new ConcurrentHashMap<>();
        private long idSequence = 0L;
        private boolean forceCollision = false;

        void setForceCollision(boolean forceCollision) {
            this.forceCollision = forceCollision;
        }

        @Override
        public Optional<Account> findByAccountNumber(String accountNumber) {
            if (forceCollision) {
                return Optional.of(new Account());
            }
            return store.values().stream()
                    .filter(a -> accountNumber.equals(a.getAccountNumber()))
                    .findFirst();
        }

        @Override
        public List<Account> findByCustomerId(Long customerId) {
            return store.values().stream()
                    .filter(a -> a.getCustomer() != null && customerId.equals(a.getCustomer().getId()))
                    .toList();
        }

        @Override
        public List<Account> findByStatus(String status) {
            return store.values().stream()
                    .filter(a -> status.equals(a.getStatus()))
                    .toList();
        }

        @Override
        public <S extends Account> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(++idSequence);
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<Account> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
        @Override public List<Account> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<Account> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(Account entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends Account> entities) {}
        @Override public void deleteAll() { store.clear(); }
        @Override public <S extends Account> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void flush() {}
        @Override public <S extends Account> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Account> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Account> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Account getOne(Long id) { return store.get(id); }
        @Override public Account getById(Long id) { return store.get(id); }
        @Override public Account getReferenceById(Long id) { return store.get(id); }
        @Override public <S extends Account> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Account> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Account> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Account> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends Account> long count(Example<S> example) { return 0; }
        @Override public <S extends Account> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Account, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<Account> findAll(Sort sort) { return findAll(); }
        @Override public Page<Account> findAll(Pageable pageable) { return Page.empty(); }
    }
}
