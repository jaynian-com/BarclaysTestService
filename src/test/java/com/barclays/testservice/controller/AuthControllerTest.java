package com.barclays.testservice.controller;

import com.barclays.testservice.model.Address;
import com.barclays.testservice.model.AuthUserRequest;
import com.barclays.testservice.model.AuthUserResponse;
import com.barclays.testservice.model.User;
import com.barclays.testservice.repository.UserRepository;
import com.barclays.testservice.util.JWTUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String AUTH_URL = "/auth/token";
    private static final String DUMMY_TOKEN = "DUMMY-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository mockUserRepository;

    @MockitoBean
    PasswordEncoder passwordEncoder;

    @MockitoBean
    JWTUtil jwtUtil;

    @BeforeEach
    void setUp() {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // Scenario: Authenticate a user with correct details
    @Test
    void should_getToken_when_validDetailsSupplied() throws Exception {
        // GIVEN
        var userId = "usr-123";
        var password = "password123";
        var timestamp = Instant.now();

        var authRequest = new AuthUserRequest(
                userId,
                password
        );

        var fetchedUser = User.builder()
                .id(userId)
                .name("Test User")
                .password(password)
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

        // WHEN-THEN
        when(mockUserRepository.findById(userId)).thenReturn(Optional.of(fetchedUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtil.generateToken(userId)).thenReturn(DUMMY_TOKEN);

        var expectedResponse = new AuthUserResponse(
                DUMMY_TOKEN
        );

        mockMvc.perform(post(AUTH_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().is(200))
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    // Scenario: Attempt to authenticate a user with incorrect user Id
    @Test
    void shouldNot_getToken_when_inValidUserSupplied() throws Exception {
        // GIVEN
        var userId = "usr-123";
        var password = "password123";

        var authRequest = new AuthUserRequest(
                userId,
                password
        );

        // WHEN-THEN
        when(mockUserRepository.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(post(AUTH_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.message").value("Invalid user credentials details supplied"));
    }

    // Scenario: Attempt to authenticate a user with incorrect user password
    @Test
    void shouldNot_getToken_when_invalidPasswordSupplied() throws Exception {
        // GIVEN
        var userId = "usr-123";
        var password = "password123";
        var timestamp = Instant.now();

        var authRequest = new AuthUserRequest(
                userId,
                password
        );

        var fetchedUser = User.builder()
                .id(userId)
                .name("Test User")
                .password(password)
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


        // WHEN-THEN
        when(mockUserRepository.findById(userId)).thenReturn(Optional.of(fetchedUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        mockMvc.perform(post(AUTH_URL)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.message").value("Invalid user credentials details supplied"));
    }

}
