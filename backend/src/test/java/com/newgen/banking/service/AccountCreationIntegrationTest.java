package com.newgen.banking.service;

import com.newgen.banking.dto.AccountRequest;
import com.newgen.banking.dto.AccountResponse;
import com.newgen.banking.exception.ConflictException;
import com.newgen.banking.exception.InvalidRequestException;
import com.newgen.banking.exception.ResourceNotFoundException;
import com.newgen.banking.model.Account;
import com.newgen.banking.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests verifying AccountService persists accounts to PostgreSQL
 * and validates customer existence using live banking_db.
 * Automatically rolls back changes via @Transactional.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class AccountCreationIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("Account is persisted in PostgreSQL with generated ID, unique account number, and ACTIVE status")
    void testCreateAccountPersistedInPostgres() {
        AccountRequest request = new AccountRequest(1L, "SAVINGS", new BigDecimal("2500.00"), "USD");

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getId()).isGreaterThan(0L);
        assertThat(response.getCustomerId()).isEqualTo(1L);
        assertThat(response.getAccountNumber()).startsWith("ACCT-SAV-");
        assertThat(response.getAccountNumber().length()).isLessThanOrEqualTo(20);
        assertThat(response.getBalance()).isEqualByComparingTo("2500.00");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        Optional<Account> persisted = accountRepository.findById(response.getId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getAccountNumber()).isEqualTo(response.getAccountNumber());
    }

    @Test
    @DisplayName("Creating account for nonexistent customer throws ResourceNotFoundException")
    void testCreateAccountCustomerNotFoundInPostgres() {
        AccountRequest request = new AccountRequest(999999L, "CHECKING", BigDecimal.ZERO, "USD");

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("Retrieve account by ID and account number from PostgreSQL")
    void testGetAccountByIdAndNumberInPostgres() {
        AccountResponse byId = accountService.getAccountById(1L);
        assertThat(byId.getAccountNumber()).isEqualTo("ACCT-CHK-8921");

        AccountResponse byNumber = accountService.getAccountByAccountNumber("ACCT-SAV-3410");
        assertThat(byNumber.getAccountType()).isEqualTo("SAVINGS");
    }

    @Test
    @DisplayName("Retrieve accounts by customer ID from PostgreSQL and reject nonexistent customer")
    void testGetAccountsByCustomerIdInPostgres() {
        List<AccountResponse> accounts = accountService.getAccountsByCustomerId(1L);
        assertThat(accounts).hasSizeGreaterThanOrEqualTo(2);

        assertThatThrownBy(() -> accountService.getAccountsByCustomerId(999999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("Validate active account, ownership, and status transitions in PostgreSQL")
    void testAccountValidationAndStatusUpdateInPostgres() {
        AccountResponse active = accountService.validateAccount(1L, 1L);
        assertThat(active.getStatus()).isEqualTo("ACTIVE");

        assertThatThrownBy(() -> accountService.validateAccountOwnership(1L, 2L))
                .isInstanceOf(InvalidRequestException.class);

        AccountResponse frozen = accountService.updateAccountStatus(1L, "FROZEN");
        assertThat(frozen.getStatus()).isEqualTo("FROZEN");
        assertThat(frozen.getBalance()).isEqualByComparingTo(active.getBalance());

        assertThatThrownBy(() -> accountService.validateActiveAccount(1L))
                .isInstanceOf(ConflictException.class);

        AccountResponse closed = accountService.updateAccountStatus(1L, "CLOSED");
        assertThat(closed.getStatus()).isEqualTo("CLOSED");

        assertThatThrownBy(() -> accountService.updateAccountStatus(1L, "ACTIVE"))
                .isInstanceOf(ConflictException.class);
    }
}
