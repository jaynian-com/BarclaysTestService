package com.barclays.testservice.controller;

import com.barclays.testservice.model.*;
import com.barclays.testservice.repository.BankAccountRepository;
import com.barclays.testservice.repository.TransactionRepository;
import com.barclays.testservice.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.barclays.testservice.model.BankAccountResponse.AccountTypeEnum.PERSONAL;
import static com.barclays.testservice.model.BankAccountResponse.SortCodeEnum._10_10_10;
import static com.barclays.testservice.model.CreateTransactionRequest.CurrencyEnum.GBP;
import static com.barclays.testservice.model.CreateTransactionRequest.TypeEnum.DEPOSIT;
import static com.barclays.testservice.model.CreateTransactionRequest.TypeEnum.WITHDRAWAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    private static final String ACCOUNTS_URL = "/v1/accounts";
    private static final String TRANSACTIONS_URL = "/transactions";
    private static final String AUTHED_USER_ID = "usr-123";
    private static final String OTHER_USER_ID = "usr-456";
    private static final String DUMMY_TOKEN = "DUMMY-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BankAccountRepository mockBankAccountRepository;

    @MockitoBean
    private TransactionRepository mockTransactionRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claims(claims -> {
                    claims.put("sub", AUTHED_USER_ID);
                    claims.put("scope", "write");
                })
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtDecoder.decode(any())).thenReturn(jwt);
    }

    /*
        CREATE A TRANSACTION SCENARIOS
     */

    // Scenario: User wants to deposit money into their bank account
    @Test
    void should_createDepositTransaction_when_validDetailsSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var timestamp = Instant.now();

        var accountRequest = new CreateTransactionRequest(
                59.99,
                GBP,
                DEPOSIT
        );

        var savedTransaction = Transaction.builder()
                .accountNumber(accountNumber)
                .amount(accountRequest.getAmount())
                .currency(accountRequest.getCurrency().getValue())
                .type(accountRequest.getType().getValue())
                .createdOn(timestamp)
                .createdOn(timestamp)
                .build();

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();
        var savedBankAccount = copyBankAccount(fetchedBankAccount);
        savedBankAccount.setBalance(savedBankAccount.getBalance() + accountRequest.getAmount());

        when(mockTransactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));
        when(mockBankAccountRepository.save(any(BankAccount.class))).thenReturn(savedBankAccount);

        // WHEN-THEN
        var expectedResponse = toTransactionResponse(savedTransaction);

        mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().is(201))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    // Scenario: User wants to withdraw money from their bank account
    @Test
    void should_createWithdrawTransaction_when_sufficientFund() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var timestamp = Instant.now();

        var accountRequest = new CreateTransactionRequest(
                59.99,
                GBP,
                WITHDRAWAL
        );

        var savedTransaction = Transaction.builder()
                .accountNumber(accountNumber)
                .amount(accountRequest.getAmount())
                .currency(accountRequest.getCurrency().getValue())
                .type(accountRequest.getType().getValue())
                .createdOn(timestamp)
                .createdOn(timestamp)
                .build();

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(60.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();
        var savedBankAccount = copyBankAccount(fetchedBankAccount);
        savedBankAccount.setBalance(savedBankAccount.getBalance() - accountRequest.getAmount());

        when(mockTransactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));
        when(mockBankAccountRepository.save(any(BankAccount.class))).thenReturn(savedBankAccount);

        // WHEN-THEN
        var expectedResponse = toTransactionResponse(savedTransaction);

        mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().is(201))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    // Scenario: User wants to withdraw money from their bank account, but they have insufficient funds
    @Test
    void shouldNot_createWithdrawTransaction_when_insufficientFund() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var timestamp = Instant.now();

        var accountRequest = new CreateTransactionRequest(
                59.99,
                GBP,
                WITHDRAWAL
        );

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(55.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();
        var savedBankAccount = copyBankAccount(fetchedBankAccount);
        savedBankAccount.setBalance(savedBankAccount.getBalance() - accountRequest.getAmount());

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));

        // WHEN-THEN
        mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Insufficient funds to process transaction"));
    }

    // Scenario: User wants to deposit or withdraw money from another user's bank account
    @Test
    void shouldNot_createTransaction_when_otherUserAccount() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var timestamp = Instant.now();

        var accountRequest = new CreateTransactionRequest(
                59.99,
                GBP,
                WITHDRAWAL
        );

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(OTHER_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(60.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));

        // WHEN-THEN
        mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.message").value("The user is not allowed to access the transaction"));
    }

    // Scenario: User wants to deposit or withdraw money into a none-existent bank account
    @Test
    void shouldNot_createTransaction_when_accountNotExists() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);

        var accountRequest = new CreateTransactionRequest(
                59.99,
                GBP,
                WITHDRAWAL
        );

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.empty());

        // WHEN-THEN
        mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Bank Account was not found"));
    }

    // Scenario: User wants to deposit or withdraw money without supplying all the data
    @Test
    void shouldNot_createTransaction_when_invalidRequest() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);

        var accountRequest = new CreateTransactionRequest(
                null,
                GBP,
                WITHDRAWAL
        );

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.empty());

        // WHEN-THEN
        mockMvc.perform(post(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.detail").value("Invalid request content."));
    }


    /*
        LIST TRANSACTIONS SCENARIOS
     */

    // Scenario: User wants to view all their transactions
    @Test
    void should_listTransactions_when_validDetailsSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var timestamp = Instant.now();

        var transactions = List.of(
                Transaction.builder()
                        .id("tan-A")
                        .accountNumber(accountNumber)
                        .amount(56.99)
                        .currency(GBP.getValue())
                        .type(DEPOSIT.getValue())
                        .createdOn(timestamp)
                        .createdOn(timestamp)
                        .build(),
                Transaction.builder()
                        .id("tan-B")
                        .accountNumber(accountNumber)
                        .amount(34.99)
                        .currency(GBP.getValue())
                        .type(WITHDRAWAL.getValue())
                        .createdOn(timestamp)
                        .createdOn(timestamp)
                        .build(),
                Transaction.builder()
                        .id("tan-C")
                        .accountNumber(accountNumber)
                        .amount(12.34)
                        .currency(GBP.getValue())
                        .type(DEPOSIT.getValue())
                        .createdOn(timestamp)
                        .createdOn(timestamp)
                        .build()
        );

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));
        when(mockTransactionRepository.findByAccountNumber(accountNumber)).thenReturn(transactions);

        // WHEN-THEN
        var expectedResponse = new ListTransactionsResponse(
                transactions.stream()
                        .map(this::toTransactionResponse)
                        .toList()
        );

        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(200))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    // Scenario: User wants to view all transactions on another user's bank account
    @Test
    void shouldNot_listTransactions_when_validDetailsSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var timestamp = Instant.now();

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(OTHER_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));

        // WHEN-THEN
        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.message").value("The user is not allowed to access the transaction"));
    }

    // Scenario: User wants to view all transactions on a non-existent bank account
    @Test
    void shouldNot_listTransactions_when_accountNotExists() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.empty());

        // WHEN-THEN
        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Bank Account was not found"));
    }


    private TransactionResponse toTransactionResponse(Transaction transaction) {
        var response = new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                TransactionResponse.CurrencyEnum.fromValue(transaction.getCurrency()),
                TransactionResponse.TypeEnum.fromValue(transaction.getType()),
                OffsetDateTime.ofInstant(transaction.getCreatedOn(), ZoneId.systemDefault())
        );
        response.setUserId(AUTHED_USER_ID);
        response.setReference("N/A");
        return response;
    }


    /*
        FETCH A TRANSACTION SCENARIOS
     */

    // Scenario: User wants to fetch a transaction on their bank account
    @Test
    void should_fetchTransaction_when_validDetailsSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var transactionId = "tan-A";
        var timestamp = Instant.now();

        var transaction = Transaction.builder()
                .id(transactionId)
                .accountNumber(accountNumber)
                .amount(56.99)
                .currency(GBP.getValue())
                .type(DEPOSIT.getValue())
                .createdOn(timestamp)
                .createdOn(timestamp)
                .build();

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));
        when(mockTransactionRepository.findByIdAndAccountNumber(transactionId, accountNumber)).thenReturn(Optional.of(transaction));

        // WHEN-THEN
        var expectedResponse = toTransactionResponse(transaction);

        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL + "/" + transactionId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(200))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    // Scenario: User wants to fetch a transaction on another user's bank account
    @Test
    void shouldNot_fetchTransaction_when_otherUserAccount() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var transactionId = "tan-A";
        var timestamp = Instant.now();

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(OTHER_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));

        // WHEN-THEN
        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL + "/" + transactionId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.message").value("The user is not allowed to access the transaction"));
    }

    // Scenario: User wants to fetch a transaction on a none-existent bank account
    // Scenario: User wants to fetch a transaction against the wrong bank account
    //
    // NOTE: Transaction select is keyed on both Transaction ID & Account Number,
    // hence both scenarios are covered by this test as it's not possible to
    // select transaction from the wrong account
    @Test
    void shouldNot_fetchTransaction_when_accountNotExists() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var transactionId = "tan-A";

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.empty());

        // WHEN-THEN
        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL + "/" + transactionId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Bank Account was not found"));
    }

    // Scenario: User wants to fetch a transaction on a none-existent transaction ID
    @Test
    void shouldNot_fetchTransaction_when_transactionNotExists() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var transactionId = "tan-A";
        var timestamp = Instant.now();

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));
        when(mockTransactionRepository.findByIdAndAccountNumber(transactionId, accountNumber)).thenReturn(Optional.empty());

        // WHEN-THEN
        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL + "/" + transactionId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Transaction was not found"));
    }

    // Scenario: User wants to fetch a transaction against the wrong bank account
    @Test
    void shouldNot_fetchTransaction_when_transactionForOtherAccount() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d", 123);
        var transactionId = "tan-A";
        var timestamp = Instant.now();

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("USER ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(BankAccountResponse.CurrencyEnum.GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));
        when(mockTransactionRepository.findByIdAndAccountNumber(transactionId, accountNumber)).thenReturn(Optional.empty());

        // WHEN-THEN
        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber + TRANSACTIONS_URL + "/" + transactionId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Transaction was not found"));
    }


    private BankAccount copyBankAccount(BankAccount bankAccount) {
        return BankAccount.builder()
                .accountNumber(bankAccount.getAccountNumber())
                .userId(bankAccount.getUserId())
                .name(bankAccount.getName())
                .accountType(bankAccount.getAccountType())
                .sortCode(bankAccount.getSortCode())
                .balance(bankAccount.getBalance())
                .currency(bankAccount.getCurrency())
                .createdOn(bankAccount.getCreatedOn())
                .lastUpdatedOn(bankAccount.getLastUpdatedOn())
                .build();
    }

}
