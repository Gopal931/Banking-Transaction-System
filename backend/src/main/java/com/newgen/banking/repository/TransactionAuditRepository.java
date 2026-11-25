package com.newgen.banking.repository;

import com.newgen.banking.model.TransactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAudit, Long> {

    List<TransactionAudit> findByAccountId(Long accountId);

    List<TransactionAudit> findByTransactionId(Long transactionId);
}
