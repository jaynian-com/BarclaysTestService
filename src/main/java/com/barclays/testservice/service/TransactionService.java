package com.barclays.testservice.service;

import com.barclays.testservice.exception.TransactionNotFoundException;
import com.barclays.testservice.model.Transaction;
import com.barclays.testservice.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class TransactionService {

    private static final String TRANS_ID_PREFIX = "tan-";

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public Transaction createTransaction(Transaction newTransaction, String accountNumber, String authUserId) {
        var bankAccount = accountService.getAccountByAccountNumber(accountNumber, authUserId);

        newTransaction.setId(getNextTransactionId());
        newTransaction.setAccountNumber(bankAccount.getAccountNumber());
        return transactionRepository.save(newTransaction);
    }

    public Transaction getTransactionByIdAndAccountNumber(String accountNumber, String transactionId, String authUserId) {
        var bankAccount = accountService.getAccountByAccountNumber(accountNumber, authUserId);

        return transactionRepository.findByIdAndAccountNumber(transactionId, bankAccount.getAccountNumber())
                .orElseThrow(TransactionNotFoundException::new);
    }

    public List<Transaction> getTransactionsByAccount(String accountNumber, String authUserId) {
        var bankAccount = accountService.getAccountByAccountNumber(accountNumber, authUserId);

        return transactionRepository.findByAccountNumber(bankAccount.getAccountNumber());
    }


    private String getNextTransactionId() {
        // NOT SURE WHY OPENAPI SCHEMA ONLY ALLOWS ONE CHARACTER
        return TRANS_ID_PREFIX + (char) ('A' + transactionRepository.getNextSequenceValue());
    }

}
