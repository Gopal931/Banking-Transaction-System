package com.newgen.banking.repository;

import com.newgen.banking.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByTransactionReference(String transactionReference);

    List<BankTransaction> findByStatus(String status);

    List<BankTransaction> findByTransactionType(String transactionType);
}
