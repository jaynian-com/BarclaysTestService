package com.barclays.testservice.repository;

import com.barclays.testservice.model.Transaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends CrudRepository<Transaction, String> {
    @Query(value = "SELECT NEXTVAL('transaction_seq')", nativeQuery = true)
    Long getNextSequenceValue();

    Optional<Transaction> findByIdAndAccountNumber(String id, String accountNumber);

    List<Transaction> findByAccountNumber(String accountNumber);

}
