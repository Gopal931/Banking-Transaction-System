package com.newgen.banking.service;

import com.newgen.banking.dto.DepositRequest;
import com.newgen.banking.dto.PagedResponse;
import com.newgen.banking.dto.TransactionResponse;
import com.newgen.banking.dto.TransferRequest;
import com.newgen.banking.dto.WithdrawRequest;
import com.newgen.banking.exception.ConflictException;
import com.newgen.banking.exception.InsufficientFundsException;
import com.newgen.banking.exception.InvalidRequestException;
import com.newgen.banking.exception.ResourceNotFoundException;
import com.newgen.banking.model.Account;
import com.newgen.banking.model.BankTransaction;
import com.newgen.banking.model.TransactionAudit;
import com.newgen.banking.repository.AccountRepository;
import com.newgen.banking.repository.BankTransactionRepository;
import com.newgen.banking.repository.TransactionAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;


 //Service handling atomic banking transactions.

@Service
public class TransactionService {

    private static final int MAX_REF_ATTEMPTS = 5;
    private static final int MAX_PAGE_SIZE = 100;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final AccountRepository accountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final TransactionAuditRepository transactionAuditRepository;

    public TransactionService(AccountRepository accountRepository,
                              BankTransactionRepository bankTransactionRepository,
                              TransactionAuditRepository transactionAuditRepository) {
        this.accountRepository = accountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.transactionAuditRepository = transactionAuditRepository;
    }

    
    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        BigDecimal amount = validatePositiveAmount(request.getAmount(), "Deposit");
        Account account = lockAndValidateActiveAccount(request.getAccountId());

        BigDecimal previousBalance = account.getBalance();
        BigDecimal newBalance = previousBalance.add(amount);
        OffsetDateTime now = OffsetDateTime.now();

        account.setBalance(newBalance);
        account.setUpdatedAt(now);
        accountRepository.save(account);

        String description = resolveDescription(request.getDescription(), "Account Deposit");
        BankTransaction savedTx = saveTransaction(null, account, "DEPOSIT", amount, account.getCurrency(), description, now);
        saveAudit(savedTx, account, "CREDIT", amount, previousBalance, newBalance, now);

        return TransactionResponse.fromEntity(savedTx, newBalance);
    }

  
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        BigDecimal amount = validatePositiveAmount(request.getAmount(), "Withdrawal");
        Account account = lockAndValidateActiveAccount(request.getAccountId());

        BigDecimal previousBalance = account.getBalance();
        if (previousBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + account.getAccountNumber() +
                            " (available: " + previousBalance + ", requested: " + amount + ")");
        }

        BigDecimal newBalance = previousBalance.subtract(amount);
        OffsetDateTime now = OffsetDateTime.now();

        account.setBalance(newBalance);
        account.setUpdatedAt(now);
        accountRepository.save(account);

        String description = resolveDescription(request.getDescription(), "Account Withdrawal");
        BankTransaction savedTx = saveTransaction(account, null, "WITHDRAWAL", amount, account.getCurrency(), description, now);
        saveAudit(savedTx, account, "DEBIT", amount, previousBalance, newBalance, now);

        return TransactionResponse.fromEntity(savedTx, newBalance);
    }

    /**
     * Atomically transfers funds between two active accounts of the same currency.
     * Locks accounts in deterministic ascending ID order to prevent deadlocks,
     * creates one TRANSFER BankTransaction, and records DEBIT and CREDIT audit entries.
     */
    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        if (request.getSourceAccountId() == null || request.getTargetAccountId() == null) {
            throw new InvalidRequestException("Source and target account IDs are required");
        }
        if (request.getSourceAccountId().equals(request.getTargetAccountId())) {
            throw new InvalidRequestException("Source and target accounts must be different");
        }

        BigDecimal amount = validatePositiveAmount(request.getAmount(), "Transfer");

        Long sourceId = request.getSourceAccountId();
        Long targetId = request.getTargetAccountId();
        Long firstLockId = sourceId < targetId ? sourceId : targetId;
        Long secondLockId = sourceId < targetId ? targetId : sourceId;

        Account firstLocked = lockAndValidateActiveAccount(firstLockId);
        Account secondLocked = lockAndValidateActiveAccount(secondLockId);

        Account sourceAccount = sourceId.equals(firstLockId) ? firstLocked : secondLocked;
        Account targetAccount = targetId.equals(firstLockId) ? firstLocked : secondLocked;

        if (!sourceAccount.getCurrency().equalsIgnoreCase(targetAccount.getCurrency())) {
            throw new InvalidRequestException(
                    "Currency mismatch between source (" + sourceAccount.getCurrency() +
                            ") and target (" + targetAccount.getCurrency() + ") accounts");
        }

        BigDecimal sourcePreviousBalance = sourceAccount.getBalance();
        if (sourcePreviousBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in source account " + sourceAccount.getAccountNumber() +
                            " (available: " + sourcePreviousBalance + ", requested: " + amount + ")");
        }

        BigDecimal targetPreviousBalance = targetAccount.getBalance();
        BigDecimal sourceNewBalance = sourcePreviousBalance.subtract(amount);
        BigDecimal targetNewBalance = targetPreviousBalance.add(amount);
        OffsetDateTime now = OffsetDateTime.now();

        sourceAccount.setBalance(sourceNewBalance);
        sourceAccount.setUpdatedAt(now);
        accountRepository.save(sourceAccount);

        targetAccount.setBalance(targetNewBalance);
        targetAccount.setUpdatedAt(now);
        accountRepository.save(targetAccount);

        String description = resolveDescription(request.getDescription(), "Account Transfer");
        BankTransaction savedTx = saveTransaction(
                sourceAccount, targetAccount, "TRANSFER", amount, sourceAccount.getCurrency(), description, now);

        saveAudit(savedTx, sourceAccount, "DEBIT", amount, sourcePreviousBalance, sourceNewBalance, now);
        saveAudit(savedTx, targetAccount, "CREDIT", amount, targetPreviousBalance, targetNewBalance, now);

        return TransactionResponse.fromEntity(savedTx, sourceNewBalance);
    }

     //Retrieves a transaction by its database ID.
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        BankTransaction tx = bankTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return TransactionResponse.fromEntity(tx);
    }

     //Retrieves a transaction by its unique transaction reference.
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new InvalidRequestException("Transaction reference must not be blank");
        }
        String trimmedRef = reference.trim();
        BankTransaction tx = bankTransactionRepository.findByTransactionReference(trimmedRef)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference: " + trimmedRef));
        return TransactionResponse.fromEntity(tx);
    }

    /**
     * Retrieves paginated transaction history for an account (as source or target), newest first.
     */
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getAccountTransactions(Long accountId, int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("Page number must be zero or positive");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found with id: " + accountId);
        }

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<TransactionResponse> transactions = bankTransactionRepository
                .findByAccountId(accountId, pageable)
                .map(TransactionResponse::fromEntity);

        return PagedResponse.fromPage(transactions);
    }

    private BigDecimal validatePositiveAmount(BigDecimal rawAmount, String operation) {
        if (rawAmount == null) {
            throw new InvalidRequestException(operation + " amount is required");
        }
        BigDecimal scaled = rawAmount.setScale(2, RoundingMode.HALF_UP);
        if (scaled.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException(operation + " amount must be greater than zero");
        }
        return scaled;
    }

    private Account lockAndValidateActiveAccount(Long accountId) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ConflictException(
                    "Account " + account.getAccountNumber() + " is not active (current status: " + account.getStatus() + ")");
        }
        return account;
    }

    private String resolveDescription(String description, String defaultText) {
        return (description != null && !description.isBlank()) ? description.trim() : defaultText;
    }

    private BankTransaction saveTransaction(Account sourceAccount, Account targetAccount, String type,
                                            BigDecimal amount, String currency, String description, OffsetDateTime now) {
        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionReference(generateUniqueReference());
        transaction.setSourceAccount(sourceAccount);
        transaction.setTargetAccount(targetAccount);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus("SUCCESS");
        transaction.setDescription(description);
        transaction.setCreatedAt(now);
        return bankTransactionRepository.save(transaction);
    }

    private void saveAudit(BankTransaction transaction, Account account, String actionType,
                           BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter, OffsetDateTime now) {
        TransactionAudit audit = new TransactionAudit();
        audit.setTransaction(transaction);
        audit.setAccount(account);
        audit.setActionType(actionType);
        audit.setAmount(amount);
        audit.setBalanceBefore(balanceBefore);
        audit.setBalanceAfter(balanceAfter);
        audit.setCreatedAt(now);
        transactionAuditRepository.save(audit);
    }

    private String generateUniqueReference() {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        for (int attempt = 0; attempt < MAX_REF_ATTEMPTS; attempt++) {
            int randomPart = 100000 + RANDOM.nextInt(900000);
            String candidate = "TXN-" + datePart + "-" + randomPart;
            if (bankTransactionRepository.findByTransactionReference(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new ConflictException("Unable to generate a unique transaction reference");
    }
}
