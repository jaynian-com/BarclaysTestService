package com.barclays.testservice.controller;

import com.barclays.testservice.model.BankAccount;
import com.barclays.testservice.model.BankAccountResponse;
import com.barclays.testservice.model.CreateBankAccountRequest;
import com.barclays.testservice.model.ListBankAccountsResponse;
import com.barclays.testservice.repository.BankAccountRepository;
import com.barclays.testservice.repository.TransactionRepository;
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

import static com.barclays.testservice.model.BankAccountResponse.CurrencyEnum.GBP;
import static com.barclays.testservice.model.BankAccountResponse.SortCodeEnum._10_10_10;
import static com.barclays.testservice.model.CreateBankAccountRequest.AccountTypeEnum.PERSONAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    private static final String ACCOUNTS_URL = "/v1/accounts";
    private static final String AUTHED_USER_ID = "usr-123";
    private static final String OTHER_USER_ID = "usr-456";
    private static final String DUMMY_TOKEN = "DUMMY-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        CREATE AN ACCOUNT SCENARIOS
     */

    // Scenario: User wants to create a new bank account
    @Test
    void should_createAccount_when_validDetailsSupplied() throws Exception {
        // GIVEN
        var nextAccountSequenceValue = 123L;
        var timestamp = Instant.now();

        var accountRequest = new CreateBankAccountRequest(
                "MY ACCOUNT",
                PERSONAL
        );

        var savedBankAccount = BankAccount.builder()
                .accountNumber("01" + String.format("%06d",nextAccountSequenceValue))
                .userId(AUTHED_USER_ID)
                .name(accountRequest.getName())
                .accountType(accountRequest.getAccountType().getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.save(any(BankAccount.class))).thenReturn(savedBankAccount);
        when(mockBankAccountRepository.getNextSequenceValue()).thenReturn(nextAccountSequenceValue);

        // WHEN-THEN
        var expectedResponse = toBankAccountResponse(savedBankAccount, timestamp);

        mockMvc.perform(post(ACCOUNTS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().is(201))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    // Scenario: User wants to create a new bank account without supplying all the required data
    @Test
    void should_notCreateAccount_when_invalidDetailsSupplied() throws Exception {
        // GIVEN
        var accountRequest = new CreateBankAccountRequest(
                null,
                PERSONAL
        );

        // WHEN-THEN
        mockMvc.perform(post(ACCOUNTS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.detail").value("Invalid request content."));
    }

    /*
        LIST ACCOUNT SCENARIOS
     */

    // Scenario: User wants to view all their bank accounts
    @Test
    void should_listAccounts_forAuthedUserId() throws Exception {
        // GIVEN
        var accountNumber1 = "01" + String.format("%06d",123L);
        var accountNumber2 = "01" + String.format("%06d",456L);
        var accountNumber3 = "01" + String.format("%06d",798L);
        var timestamp = Instant.now();

        var bankAccounts = List.of(
            BankAccount.builder()
                    .accountNumber(accountNumber1)
                    .userId(AUTHED_USER_ID)
                    .name("ACCOUNT 1")
                    .accountType(PERSONAL.getValue())
                    .sortCode(_10_10_10.getValue())
                    .currency(GBP.getValue())
                    .createdOn(timestamp)
                    .lastUpdatedOn(timestamp)
                    .build(),
                BankAccount.builder()
                        .accountNumber(accountNumber2)
                        .userId(AUTHED_USER_ID)
                        .name("ACCOUNT 2")
                        .accountType(PERSONAL.getValue())
                        .sortCode(_10_10_10.getValue())
                        .currency(GBP.getValue())
                        .createdOn(timestamp)
                        .lastUpdatedOn(timestamp)
                        .build(),
                BankAccount.builder()
                        .accountNumber(accountNumber3)
                        .userId(AUTHED_USER_ID)
                        .name("ACCOUNT 1")
                        .accountType(PERSONAL.getValue())
                        .sortCode(_10_10_10.getValue())
                        .currency(GBP.getValue())
                        .createdOn(timestamp)
                        .lastUpdatedOn(timestamp)
                        .build()
        );

        when(mockBankAccountRepository.findByUserId(AUTHED_USER_ID)).thenReturn(bankAccounts);

        // WHEN-THEN
        var expectedResponse = new ListBankAccountsResponse(
                bankAccounts.stream().map(
                bankAccount -> toBankAccountResponse(bankAccount, timestamp)
                ).toList()
        );

        mockMvc.perform(get(ACCOUNTS_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(200))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    /*
        FETCH BANK ACCOUNT SCENARIOS
     */

    // Scenario: User wants to fetch bank account details
    @Test
    void should_fetchAccount_when_validDetailsSuppliedAndAuthed() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d",123);
        var timestamp = Instant.now();

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("ACCOUNT 1")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .currency(GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));

        // WHEN-THEN
        var expectedResponse =  toBankAccountResponse(fetchedBankAccount, timestamp);

        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(200))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    // Scenario: User wants to fetch another user's bank account details
    @Test
    void shouldNot_fetchAccount_when_otherUserAccountSuppledAndAuthed() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d",123);
        var timestamp = Instant.now();

        var fetchedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(OTHER_USER_ID)
                .name("ACCOUNT 2")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .currency(GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(fetchedBankAccount));

        // WHEN-THEN
        mockMvc.perform(get(ACCOUNTS_URL + "/" + accountNumber)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.message").value("The user is not allowed to access the transaction"));
    }

    // Scenario: User wants to fetch a none-existent bank account
    @Test
    void shouldNot_fetchAccount_when_otherUserAccountNotExistsAndAuthed() throws Exception {
        // GIVEN
        var account1Number = "01" + String.format("%06d",123);

        when(mockBankAccountRepository.findById(account1Number)).thenReturn(Optional.empty());

        // WHEN-THEN
        mockMvc.perform(get(ACCOUNTS_URL + "/" + account1Number)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Bank Account was not found"));
    }

    /*
        UPDATE BANK ACCOUNT SCENARIOS
     */

    // Scenario: User wants to update their bank account details
    @Test
    void should_updateAccount_when_validDetailsSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d",123);
        var timestamp = Instant.now();

        var updateAccountRequest = new CreateBankAccountRequest(
                "UPDATED MY ACCOUNT",
                PERSONAL
        );

        var updateBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name(updateAccountRequest.getName())
                .accountType(updateAccountRequest.getAccountType().getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        var notUpdatedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("MY ACCOUNT")
                .accountType(updateAccountRequest.getAccountType().getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(notUpdatedBankAccount));
        when(mockBankAccountRepository.save(any(BankAccount.class))).thenReturn(notUpdatedBankAccount);

        // WHEN-THEN
        var expectedResponse = toBankAccountResponse(updateBankAccount, timestamp);

        mockMvc.perform(patch(ACCOUNTS_URL + "/" + accountNumber)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(updateAccountRequest)))
                .andExpect(status().is(200))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    // Scenario: User wants to update another user's bank account details
    @Test
    void shouldNot_updateAccount_when_otherUsersAccountSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d",123);
        var timestamp = Instant.now();

        var updateAccountRequest = new CreateBankAccountRequest(
                "UPDATED MY ACCOUNT",
                PERSONAL
        );

        var notUpdatedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(OTHER_USER_ID)
                .name("MY ACCOUNT")
                .accountType(updateAccountRequest.getAccountType().getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(notUpdatedBankAccount));

        // WHEN-THEN

        mockMvc.perform(patch(ACCOUNTS_URL + "/" + accountNumber)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(updateAccountRequest)))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.message").value("The user is not allowed to access the transaction"));

    }

    // Scenario: User wants to update a none-existent bank account
    @Test
    void shouldNot_updateAccount_when_accountNotExistsSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d",123);

        var updateAccountRequest = new CreateBankAccountRequest(
                "UPDATED MY ACCOUNT",
                PERSONAL
        );

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.empty());

        // WHEN-THEN

        mockMvc.perform(patch(ACCOUNTS_URL + "/" + accountNumber)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN)
                        .content(objectMapper.writeValueAsString(updateAccountRequest)))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Bank Account was not found"));

    }

    /*
        DELETE BANK ACCOUNT SCENARIOS
     */

    // Scenario: User wants to delete an existing bank account
    @Test
    void should_deleteAccount_when_validDetailsSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d",123);
        var timestamp = Instant.now();

        var notDeletedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(AUTHED_USER_ID)
                .name("MY ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(notDeletedBankAccount));

        // WHEN-THEN
        mockMvc.perform(delete(ACCOUNTS_URL + "/" + accountNumber)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(204));
    }

    // Scenario: User wants to delete another user's bank account details
    @Test
    void shouldNot_deleteAccount_when_anotherUserAccountSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d",123);
        var timestamp = Instant.now();

        var notDeletedBankAccount = BankAccount.builder()
                .accountNumber(accountNumber)
                .userId(OTHER_USER_ID)
                .name("MY ACCOUNT")
                .accountType(PERSONAL.getValue())
                .sortCode(_10_10_10.getValue())
                .balance(0.0)
                .currency(GBP.getValue())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.of(notDeletedBankAccount));

        // WHEN-THEN
        mockMvc.perform(delete(ACCOUNTS_URL + "/" + accountNumber)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.message").value("The user is not allowed to access the transaction"));
    }


    // Scenario: User wants to delete another user's bank account details
    @Test
    void shouldNot_deleteAccount_when_accountNotExistsSupplied() throws Exception {
        // GIVEN
        var accountNumber = "01" + String.format("%06d",123);

        when(mockBankAccountRepository.findById(accountNumber)).thenReturn(Optional.empty());

        // WHEN-THEN
        mockMvc.perform(delete(ACCOUNTS_URL + "/" + accountNumber)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Bank Account was not found"));
    }


    private BankAccountResponse toBankAccountResponse(BankAccount bankAccount, Instant timestamp) {
        return new BankAccountResponse(
                bankAccount.getAccountNumber(),
                BankAccountResponse.SortCodeEnum.fromValue(bankAccount.getSortCode()),
                bankAccount.getName(),
                BankAccountResponse.AccountTypeEnum.fromValue(bankAccount.getAccountType()),
                bankAccount.getBalance(),
                BankAccountResponse.CurrencyEnum.fromValue(bankAccount.getCurrency()),
                OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault()),
                OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        );
    }

}
