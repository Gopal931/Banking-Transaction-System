package com.newgen.banking.service;

import com.newgen.banking.dto.AccountRequest;
import com.newgen.banking.dto.AccountResponse;
import com.newgen.banking.exception.ConflictException;
import com.newgen.banking.exception.InvalidRequestException;
import com.newgen.banking.exception.ResourceNotFoundException;
import com.newgen.banking.model.Account;
import com.newgen.banking.model.Customer;
import com.newgen.banking.repository.AccountRepository;
import com.newgen.banking.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

 //Service handling bank account creation and validation.

@Service
public class AccountService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, String> ACCOUNT_TYPE_PREFIXES = Map.of(
            "CHECKING", "CHK",
            "SAVINGS", "SAV",
            "MONEY_MARKET", "MMK",
            "CERTIFICATE_OF_DEPOSIT", "CD"
    );
    private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            "ACTIVE", Set.of("DORMANT", "FROZEN", "CLOSED"),
            "DORMANT", Set.of("ACTIVE", "FROZEN", "CLOSED"),
            "FROZEN", Set.of("ACTIVE", "CLOSED"),
            "CLOSED", Set.of()
    );

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }
    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        String normalizedType = request.getAccountType().trim().toUpperCase();
        String typePrefix = ACCOUNT_TYPE_PREFIXES.get(normalizedType);
        if (typePrefix == null) {
            throw new InvalidRequestException(
                    "Unsupported account type '" + request.getAccountType() +
                            "'. Supported types: CHECKING, SAVINGS, MONEY_MARKET, CERTIFICATE_OF_DEPOSIT");
        }

        BigDecimal initialBalance = request.getInitialBalance() != null
                ? request.getInitialBalance().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("Initial balance must be zero or positive");
        }

        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency().trim().toUpperCase()
                : "USD";

        String accountNumber = generateUniqueAccountNumber(typePrefix);
        OffsetDateTime now = OffsetDateTime.now();

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountNumber(accountNumber);
        account.setAccountType(normalizedType);
        account.setBalance(initialBalance);
        account.setCurrency(currency);
        account.setStatus("ACTIVE");
        account.setCreatedAt(now);
        account.setUpdatedAt(now);

        Account savedAccount = accountRepository.save(account);
        return AccountResponse.fromEntity(savedAccount);
    }

     //Retrieves an account by its database ID.
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        return AccountResponse.fromEntity(account);
    }

    
     //Retrieves an account by its unique account number.
    
    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with accountNumber: " + accountNumber));
        return AccountResponse.fromEntity(account);
    }

 
     //Retrieves all accounts belonging to an existing customer.
  
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(AccountResponse::fromEntity)
                .toList();
    }

    
     // Validates that an account exists, is ACTIVE, and optionally belongs to customerId.
    
    @Transactional(readOnly = true)
    public AccountResponse validateAccount(Long accountId, Long customerId) {
        Account account = validateActiveAccount(accountId);
        if (customerId != null) {
            validateOwnership(account, customerId);
        }
        return AccountResponse.fromEntity(account);
    }


     // Validates that an account exists and is currently ACTIVE.
   
    @Transactional(readOnly = true)
    public Account validateActiveAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ConflictException(
                    "Account " + account.getAccountNumber() + " is not active (current status: " + account.getStatus() + ")");
        }
        return account;
    }


     //Validates that an account exists and belongs to the specified customer.
    
    @Transactional(readOnly = true)
    public Account validateAccountOwnership(Long accountId, Long customerId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        validateOwnership(account, customerId);
        return account;
    }

   
     //Updates an account's status following allowed transition rules, preserving balance and other fields.
    
    @Transactional
    public AccountResponse updateAccountStatus(Long accountId, String requestedStatus) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        String targetStatus = requestedStatus.trim().toUpperCase();
        if (!ALLOWED_STATUS_TRANSITIONS.containsKey(targetStatus)) {
            throw new InvalidRequestException(
                    "Unsupported account status '" + requestedStatus + "'. Supported statuses: ACTIVE, DORMANT, FROZEN, CLOSED");
        }

        String currentStatus = account.getStatus();
        if (currentStatus.equals(targetStatus)) {
            throw new ConflictException("Account is already in status: " + targetStatus);
        }

        Set<String> allowedTargets = ALLOWED_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            throw new ConflictException(
                    "Invalid status transition from '" + currentStatus + "' to '" + targetStatus + "'");
        }

        account.setStatus(targetStatus);
        account.setUpdatedAt(OffsetDateTime.now());

        Account saved = accountRepository.save(account);
        return AccountResponse.fromEntity(saved);
    }

    private void validateOwnership(Account account, Long customerId) {
        if (account.getCustomer() == null || !account.getCustomer().getId().equals(customerId)) {
            throw new InvalidRequestException(
                    "Account " + account.getId() + " does not belong to customer " + customerId);
        }
    }

    private String generateUniqueAccountNumber(String typePrefix) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            int suffix = 10000000 + RANDOM.nextInt(90000000);
            String candidate = "ACCT-" + typePrefix + "-" + suffix;
            if (accountRepository.findByAccountNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new ConflictException("Unable to generate a unique account number after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}
