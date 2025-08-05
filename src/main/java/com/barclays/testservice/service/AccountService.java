package com.barclays.testservice.service;

import com.barclays.testservice.exception.BankAccountNotFoundException;
import com.barclays.testservice.exception.UserNotAllowedException;
import com.barclays.testservice.model.BankAccount;
import com.barclays.testservice.model.BankAccountResponse;
import com.barclays.testservice.repository.BankAccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class AccountService {

    private static final String ACC_ID_PREFIX = "01";

    private final BankAccountRepository bankAccountRepository;

    public BankAccount createAccount(BankAccount newBankAccount , String authUserId) {

        newBankAccount.setAccountNumber(getNextBankAccountId());
        newBankAccount.setUserId(authUserId);
        newBankAccount.setSortCode(BankAccountResponse.SortCodeEnum._10_10_10.getValue());
        newBankAccount.setBalance(0.0);
        newBankAccount.setCurrency(BankAccountResponse.CurrencyEnum.GBP.getValue());
        return bankAccountRepository.save(newBankAccount);
    }

    public BankAccount getAccountByAccountNumber(String accountNumber, String authUserId) {

        var fetchedBankAccount = bankAccountRepository.findById(accountNumber)
                .orElseThrow(BankAccountNotFoundException::new);

        checkUserIdAllowed(fetchedBankAccount, authUserId);

        return fetchedBankAccount;
    }

    public List<BankAccount> getAccountsByUserId(String authUserId) {
        return bankAccountRepository.findByUserId(authUserId);
    }


    public BankAccount updateAccountByAccountNumber(String accountNumber, BankAccount updateBankAccount, String authUserId) {

        var fetchedBankAccount = bankAccountRepository.findById(accountNumber)
                .orElseThrow(BankAccountNotFoundException::new);

        checkUserIdAllowed(fetchedBankAccount, authUserId);

        // Copy potentially updated fields to the fetched object
        fetchedBankAccount.setName(updateBankAccount.getName());
        fetchedBankAccount.setAccountType(updateBankAccount.getAccountType());

        return bankAccountRepository.save(fetchedBankAccount);
    }

    public void deleteAccountByAccountNumber(String accountNumber, String authUserId) {

        var fetchedBankAccount = bankAccountRepository.findById(accountNumber)
                .orElseThrow(BankAccountNotFoundException::new);

        checkUserIdAllowed(fetchedBankAccount, authUserId);

        bankAccountRepository.deleteById(accountNumber);
    }

    public boolean checkUserHasBankAccounts(String authUserId) {
        return bankAccountRepository.existsByUserId(authUserId);
    }


    private void checkUserIdAllowed(BankAccount bankAccount, String authUserId) {
        if(!authUserId.equals(bankAccount.getUserId())) {
            throw new UserNotAllowedException();
        }
    }


    private String getNextBankAccountId() {
        return ACC_ID_PREFIX + String.format("%06d",bankAccountRepository.getNextSequenceValue());
    }

}
