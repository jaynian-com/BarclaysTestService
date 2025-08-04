package com.barclays.testservice.repository;

import com.barclays.testservice.model.BankAccount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface BankAccountRepository extends CrudRepository<BankAccount, String> {
    @Query(value = "SELECT NEXTVAL('bankaccount_seq')", nativeQuery = true)
    Long getNextSequenceValue();

    List<BankAccount> findByUserId(String userId);
}
