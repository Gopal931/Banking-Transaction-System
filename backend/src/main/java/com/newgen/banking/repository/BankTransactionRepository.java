package com.newgen.banking.repository;

import com.newgen.banking.model.BankTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByTransactionReference(String transactionReference);

    List<BankTransaction> findByStatus(String status);

    List<BankTransaction> findByTransactionType(String transactionType);

    @Query("SELECT t FROM BankTransaction t WHERE t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId")
    Page<BankTransaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}
