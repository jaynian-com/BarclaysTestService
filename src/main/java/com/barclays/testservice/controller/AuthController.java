package com.barclays.testservice.controller;

import com.barclays.testservice.api.AuthApi;
import com.barclays.testservice.model.AuthUserRequest;
import com.barclays.testservice.model.AuthUserResponse;
import com.barclays.testservice.service.AuthUserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AuthController implements AuthApi {

    private final AuthUserDetailsService authUserDetailsService;

    @Override
    public ResponseEntity<AuthUserResponse> token(AuthUserRequest authUserRequest) {
        var token = authUserDetailsService.getAuthenticationToken(
                authUserRequest.getUserId(),
                authUserRequest.getPassword()
        );

        return ResponseEntity.ok(new AuthUserResponse(token));
    }

}
