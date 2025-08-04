package com.barclays.testservice.controller;

import com.barclays.testservice.model.*;
import com.barclays.testservice.repository.AddressRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    private static final String USER_URL = "/v1/users";
    private static final String AUTHED_USER_ID = "usr-123";
    private static final String OTHER_USER_ID = "usr-456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository mockUserRepository;

    @MockitoBean
    private AddressRepository mockAddressRepository;

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


    // Scenario: Create a new user
    @Test
    void should_createUser_when_validDetailsSupplied() throws Exception {
        // GIVEN
        var password = "password123";
        var nextUserSequenceValue = 123L;
        var nextAddressSequenceValue = 123L;
        var timestamp = Instant.now();

        var userRequest = new CreateUserRequest(
                "Test User",
                new CreateUserRequestAddress(
                        "line1",
                        "town",
                        "county",
                        "postcode"
                ),
                "0123456789",
                "my@email.com"
        );

        var savedUser = User.builder()
                .id("usr-" + nextUserSequenceValue)
                .name(userRequest.getName())
                .address(Address.builder()
                        .id("adr-" + nextAddressSequenceValue)
                        .line1(userRequest.getAddress().getLine1())
                        .town(userRequest.getAddress().getTown())
                        .county(userRequest.getAddress().getCounty())
                        .postcode(userRequest.getAddress().getPostcode())
                        .createdOn(timestamp)
                        .lastUpdatedOn(timestamp)
                        .build()
                ).phoneNumber(userRequest.getPhoneNumber())
                .email(userRequest.getEmail())
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockUserRepository.save(any(User.class))).thenReturn(savedUser);
        when(mockUserRepository.getNextSequenceValue()).thenReturn(nextUserSequenceValue);
        when(mockAddressRepository.getNextSequenceValue()).thenReturn(nextAddressSequenceValue);

        // WHEN-THEN
        var expectedResponse = new UserResponse(
                "usr-" + nextUserSequenceValue,
                savedUser.getName(),
                new CreateUserRequestAddress(
                        savedUser.getAddress().getLine1(),
                        savedUser.getAddress().getTown(),
                        savedUser.getAddress().getCounty(),
                        savedUser.getAddress().getPostcode()
                ),
                savedUser.getPhoneNumber(),
                savedUser.getEmail(),
                OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault()),
                OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        );


        mockMvc.perform(post(USER_URL + "/" + password)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(201))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    // Scenario: Create a new user without supplying all the required data
    @Test
    void should_notCreateUser_when_invalidDetailsSupplied() throws Exception {
        // GIVEN
        var password = "password123";

        var userRequest = new CreateUserRequest(
                null,
                new CreateUserRequestAddress(
                        "line1",
                        "town",
                        "county",
                        "postcode"
                ),
                "0123456789",
                "my@email.com"
        );

        // WHEN-THEN
        var expectedResponse = new BadRequestErrorResponse();
        expectedResponse.details(List.of(
                new BadRequestErrorResponseDetailsInner("type", "about:blank", "string"),
                new BadRequestErrorResponseDetailsInner("title", "Bad Request", "string")
        ));

        mockMvc.perform(post(USER_URL + "/" + password)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.detail").value("Invalid request content."));
    }

    // Scenario: User wants to fetch their details
    @Test
    void should_fetchUser_when_validDetailsSuppliedAndAuthed() throws Exception {
        // GIVEN
        var timestamp = Instant.now();
        var dummyToken = "DUMMY-TOKEN";

        // WHEN-THEN
        var fetchedUser = User.builder()
                .id(AUTHED_USER_ID)
                .name("Test User")
                .address(Address.builder()
                        .id("adr-456")
                        .line1("line1")
                        .town("town")
                        .county("county")
                        .postcode("postcode")
                        .createdOn(timestamp)
                        .lastUpdatedOn(timestamp)
                        .build()
                ).phoneNumber("")
                .email("")
                .createdOn(timestamp)
                .lastUpdatedOn(timestamp)
                .build();

        when(mockUserRepository.findById(AUTHED_USER_ID)).thenReturn(Optional.of(fetchedUser));

        // WHEN-THEN
        var expectedResponse = new UserResponse(
                AUTHED_USER_ID,
                fetchedUser.getName(),
                new CreateUserRequestAddress(
                        fetchedUser.getAddress().getLine1(),
                        fetchedUser.getAddress().getTown(),
                        fetchedUser.getAddress().getCounty(),
                        fetchedUser.getAddress().getPostcode()
                ),
                fetchedUser.getPhoneNumber(),
                fetchedUser.getEmail(),
                OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault()),
                OffsetDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        );

        mockMvc.perform(get(USER_URL + "/" + AUTHED_USER_ID)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + dummyToken)
                )
                .andExpect(status().is(200))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }


    // Scenario: User wants to fetch the details of another user
    @Test
    void should_notFetchUser_when_otherUserIdSuppliedAndAuthed() throws Exception {
        // GIVEN
        var dummyToken = "DUMMY-TOKEN";

        // WHEN-THEN
        mockMvc.perform(get(USER_URL + "/" + OTHER_USER_ID)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + dummyToken)
                )
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.message").value("The user is not allowed to access the transaction"));
    }

    // Scenario: User wants to fetch the details of a none-existent user
    @Test
    void should_notFetchUser_when_userNotExistsAndAuthed() throws Exception {
        // GIVEN
        var dummyToken = "DUMMY-TOKEN";

        // WHEN-THEN
        when(mockUserRepository.findById(AUTHED_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(USER_URL + "/" + AUTHED_USER_ID)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + dummyToken)
                )
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("User was not found"));
    }


    // TODO: DELETE USER SCENARIOS
    @Test
    void deleteUserByID() {
    }



    // TODO: UPDATE USER SCENARIOS
    @Test
    void updateUserByID() {
    }
}
