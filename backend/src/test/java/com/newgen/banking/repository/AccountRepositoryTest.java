package com.newgen.banking.repository;

import com.newgen.banking.model.Account;
import com.newgen.banking.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("1. Find account by ID should return existing account")
    void testFindById() {
        Optional<Account> result = accountRepository.findById(1L);

        assertThat(result).isPresent();
        Account account = result.get();
        assertThat(account.getId()).isEqualTo(1L);
        assertThat(account.getAccountNumber()).isEqualTo("ACCT-CHK-8921");
        assertThat(account.getAccountType()).isEqualTo("CHECKING");
        assertThat(account.getCurrency()).isEqualTo("USD");
        assertThat(account.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("2. Find account by account number should return matching account")
    void testFindByAccountNumber() {
        Optional<Account> found = accountRepository.findByAccountNumber("ACCT-SAV-3410");

        assertThat(found).isPresent();
        assertThat(found.get().getAccountType()).isEqualTo("SAVINGS");
        assertThat(found.get().getStatus()).isEqualTo("ACTIVE");

        Optional<Account> notFound = accountRepository.findByAccountNumber("ACCT-NON-EXISTENT");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("3. Find accounts belonging to a customer")
    void testFindByCustomerId() {
        List<Account> customerAccounts = accountRepository.findByCustomerId(1L);

        assertThat(customerAccounts).isNotEmpty();
        assertThat(customerAccounts.size()).isGreaterThanOrEqualTo(2);
        assertThat(customerAccounts)
                .extracting(Account::getAccountNumber)
                .contains("ACCT-CHK-8921", "ACCT-SAV-3410");
    }

    @Test
    @DisplayName("4. Find accounts by status")
    void testFindByStatus() {
        List<Account> activeAccounts = accountRepository.findByStatus("ACTIVE");

        assertThat(activeAccounts).isNotEmpty();
        assertThat(activeAccounts.size()).isGreaterThanOrEqualTo(4);
        assertThat(activeAccounts).allMatch(a -> "ACTIVE".equals(a.getStatus()));
    }

    @Test
    @DisplayName("5. Save a new account using an existing customer and verify generated ID")
    void testSaveNewAccount() {
        Customer existingCustomer = customerRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Seed customer id=1 must exist"));

        String uniqueAccountNumber = "ACCT-TST-" + (System.currentTimeMillis() % 100000000L);
        Account newAccount = new Account();
        newAccount.setCustomer(existingCustomer);
        newAccount.setAccountNumber(uniqueAccountNumber);
        newAccount.setAccountType("SAVINGS");
        newAccount.setBalance(new BigDecimal("1500.00"));
        newAccount.setCurrency("USD");
        newAccount.setStatus("ACTIVE");
        newAccount.setCreatedAt(OffsetDateTime.now());
        newAccount.setUpdatedAt(OffsetDateTime.now());

        Account saved = accountRepository.save(newAccount);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);

        Optional<Account> retrieved = accountRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAccountNumber()).isEqualTo(uniqueAccountNumber);
        assertThat(retrieved.get().getBalance()).isEqualByComparingTo("1500.00");
        assertThat(retrieved.get().getCustomer().getId()).isEqualTo(1L);
    }
}
