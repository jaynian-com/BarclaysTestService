package com.barclays.testservice.controller;

import com.barclays.testservice.api.UserApi;
import com.barclays.testservice.model.*;
import com.barclays.testservice.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@RestController
@AllArgsConstructor
@Slf4j
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<UserResponse> createUser(String password, CreateUserRequest createUserRequest) {
        return new ResponseEntity<>(
                toUserResponse(
                        userService.createUser(
                                fromCreateUserRequest(createUserRequest),
                                password
                        )
                ),
                HttpStatus.valueOf(201)
        );
    }

    @Override
    public ResponseEntity<Void> deleteUserByID(String userId) {
        userService.deleteUserByUserId(
                userId,
                getAuthUserId()
        );
        return new ResponseEntity<>(HttpStatus.valueOf(204));
    }

    @Override
    public ResponseEntity<UserResponse> fetchUserByID(String userId) {
        return new ResponseEntity<>(
                toUserResponse(
                        userService.getUserByUserId(
                                userId,
                                getAuthUserId()
                        )
                ),
                HttpStatus.valueOf(200)
        );
    }

    @Override
    public ResponseEntity<UserResponse> updateUserByID(String userId, UpdateUserRequest updateUserRequest) {
        return new ResponseEntity<>(
                toUserResponse(
                        userService.updateUserByUserId(
                                userId,
                                fromUpdateUserRequest(updateUserRequest),
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

    private UserResponse toUserResponse(User user) {

        var addressResponse = new CreateUserRequestAddress();
        var address = user.getAddress();
        addressResponse.setLine1(address.getLine1());
        addressResponse.setLine2(address.getLine2());
        addressResponse.setLine3(address.getLine3());
        addressResponse.setTown(address.getTown());
        addressResponse.setCounty(address.getCounty());
        addressResponse.setPostcode(address.getPostcode());

        return new UserResponse(
                user.getId(),
                user.getName(),
                addressResponse,
                user.getPhoneNumber(),
                user.getEmail(),
                OffsetDateTime.ofInstant(user.getCreatedOn(), ZoneId.systemDefault()),
                OffsetDateTime.ofInstant(user.getLastUpdatedOn(), ZoneId.systemDefault())
        );
    }

    private User fromCreateUserRequest(CreateUserRequest createUserRequest) {
        return User.builder()
                .name(createUserRequest.getName())
                .address(Address.builder()
                        .line1(createUserRequest.getAddress().getLine1())
                        .line2(createUserRequest.getAddress().getLine2())
                        .line3(createUserRequest.getAddress().getLine3())
                        .town(createUserRequest.getAddress().getTown())
                        .county(createUserRequest.getAddress().getCounty())
                        .postcode(createUserRequest.getAddress().getPostcode())
                        .build()
                )
                .phoneNumber(createUserRequest.getPhoneNumber())
                .email(createUserRequest.getEmail())
                .build();
    }

    private User fromUpdateUserRequest(UpdateUserRequest updateUserRequest) {
        return User.builder()
                .name(updateUserRequest.getName())
                .address(Address.builder()
                        .line1(updateUserRequest.getAddress().getLine1())
                        .line2(updateUserRequest.getAddress().getLine2())
                        .line3(updateUserRequest.getAddress().getLine3())
                        .town(updateUserRequest.getAddress().getTown())
                        .county(updateUserRequest.getAddress().getCounty())
                        .postcode(updateUserRequest.getAddress().getPostcode())
                        .build()
                )
                .phoneNumber(updateUserRequest.getPhoneNumber())
                .email(updateUserRequest.getEmail())
                .build();
    }

}
