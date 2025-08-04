package com.barclays.testservice.controller;

import com.barclays.testservice.api.AccountApi;
import com.barclays.testservice.model.*;
import com.barclays.testservice.service.AccountService;
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
public class AccountController implements AccountApi {

    private final AccountService accountService;

    @Override
    public ResponseEntity<BankAccountResponse> createAccount(CreateBankAccountRequest createBankAccountRequest) {
        return new ResponseEntity<>(
                toBankAccountResponse(
                        accountService.createAccount(
                                fromCreateBankAccountRequest(createBankAccountRequest),
                                getAuthUserId()
                        )
                ),
                HttpStatus.valueOf(201)
        );
    }

    @Override
    public ResponseEntity<Void> deleteAccountByAccountNumber(String accountNumber) {
        accountService.deleteAccountByAccountNumber(
                accountNumber,
                getAuthUserId()
        );
        return new ResponseEntity<>(HttpStatus.valueOf(204));
    }

    @Override
    public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(String accountNumber) {
        return new ResponseEntity<>(
                toBankAccountResponse(
                        accountService.getAccountByAccountNumber(
                                accountNumber,
                                getAuthUserId()
                        )
                ),
                HttpStatus.valueOf(200)
        );
    }

    @Override
    public ResponseEntity<ListBankAccountsResponse> listAccounts() {
        return new ResponseEntity<>(
                new ListBankAccountsResponse(
                    accountService.getAccountsByUserId(getAuthUserId())
                            .stream().map(this::toBankAccountResponse)
                            .collect(Collectors.toList()
                    )
                ),
                HttpStatus.valueOf(200)
        );
    }

    @Override
    public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(String accountNumber, UpdateBankAccountRequest updateBankAccountRequest) {
        return new ResponseEntity<>(
                toBankAccountResponse(
                        accountService.updateAccountByAccountNumber(
                                accountNumber,
                                fromUpdateBankAccountRequest(updateBankAccountRequest),
                                getAuthUserId()
                        )
                ),
                HttpStatus.valueOf(200)
        );
    }

    private String getAuthUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Rest / Domain Object Converters
    private BankAccountResponse toBankAccountResponse(BankAccount bankAccount) {
        return new BankAccountResponse(
                bankAccount.getAccountNumber(),
                BankAccountResponse.SortCodeEnum.fromValue(bankAccount.getSortCode()),
                bankAccount.getName(),
                BankAccountResponse.AccountTypeEnum.fromValue(bankAccount.getAccountType()),
                bankAccount.getBalance().doubleValue(),
                BankAccountResponse.CurrencyEnum.fromValue(bankAccount.getCurrency()),
                OffsetDateTime.ofInstant(bankAccount.getCreatedOn(), ZoneId.systemDefault()),
                OffsetDateTime.ofInstant(bankAccount.getLastUpdatedOn(), ZoneId.systemDefault())
        );
    }

    private BankAccount fromCreateBankAccountRequest(CreateBankAccountRequest createBankAccountRequest) {
        return BankAccount.builder()
                .name(createBankAccountRequest.getName())
                .accountType(createBankAccountRequest.getAccountType().getValue())
                .build();
    }

    private BankAccount fromUpdateBankAccountRequest(UpdateBankAccountRequest updateBankAccountRequest) {
        return BankAccount.builder()
                .name(updateBankAccountRequest.getName())
                .accountType(updateBankAccountRequest.getAccountType().getValue())
                .build();
    }

}
