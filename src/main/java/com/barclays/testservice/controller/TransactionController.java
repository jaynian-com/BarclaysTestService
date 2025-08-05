package com.barclays.testservice.controller;

import com.barclays.testservice.api.TransactionApi;
import com.barclays.testservice.model.CreateTransactionRequest;
import com.barclays.testservice.model.ListTransactionsResponse;
import com.barclays.testservice.model.Transaction;
import com.barclays.testservice.model.TransactionResponse;
import com.barclays.testservice.service.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class TransactionController implements TransactionApi {

    private final TransactionService transactionService;

    @Override
    public ResponseEntity<TransactionResponse> createTransaction(String accountNumber, CreateTransactionRequest createTransactionRequest) {
        return new ResponseEntity<>(
                toTransactionResponse(
                        transactionService.createTransaction(
                                fromCreateTransactionRequest(createTransactionRequest),
                                accountNumber,
                                getAuthUserId()
                        )
                ),
                HttpStatus.valueOf(201)
        );
    }

    @Override
    public ResponseEntity<TransactionResponse> fetchAccountTransactionByID(String accountNumber, String transactionId) {
        return new ResponseEntity<>(
                toTransactionResponse(
                        transactionService.getTransactionByIdAndAccountNumber(
                                accountNumber,
                                transactionId,
                                getAuthUserId()
                        )
                ),
                HttpStatus.valueOf(200)
        );
    }

    @Override
    public ResponseEntity<ListTransactionsResponse> listAccountTransaction(String accountNumber) {
        return new ResponseEntity<>(
                new ListTransactionsResponse(
                        transactionService.getTransactionsByAccount(accountNumber, getAuthUserId())
                                .stream().map(this::toTransactionResponse)
                                .collect(Collectors.toList()
                                )
                ),
                HttpStatus.valueOf(200)
        );
    }

    private String getAuthUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        var response = new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                TransactionResponse.CurrencyEnum.fromValue(transaction.getCurrency()),
                TransactionResponse.TypeEnum.fromValue(transaction.getType()),
                OffsetDateTime.ofInstant(transaction.getCreatedOn(), ZoneId.systemDefault())
        );
        response.setUserId(getAuthUserId());
        response.setReference("N/A");
        return response;
    }

    private Transaction fromCreateTransactionRequest(CreateTransactionRequest createTransactionRequest) {
        return Transaction.builder()
                .amount(createTransactionRequest.getAmount())
                .currency(createTransactionRequest.getCurrency().getValue())
                .type(createTransactionRequest.getType().getValue())
                .build();
    }


}
