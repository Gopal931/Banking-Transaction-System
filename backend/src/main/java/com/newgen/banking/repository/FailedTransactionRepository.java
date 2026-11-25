package com.newgen.banking.repository;

import com.newgen.banking.model.FailedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedTransactionRepository extends JpaRepository<FailedTransaction, Long> {

    List<FailedTransaction> findByErrorCode(String errorCode);

    List<FailedTransaction> findByTransactionReference(String transactionReference);
}
