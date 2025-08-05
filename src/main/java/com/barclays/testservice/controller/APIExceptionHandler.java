package com.barclays.testservice.controller;

import com.barclays.testservice.exception.*;
import com.barclays.testservice.model.BadRequestErrorResponse;
import com.barclays.testservice.model.BadRequestErrorResponseDetailsInner;
import com.barclays.testservice.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class APIExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidDetailsSuppliedException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDetailsSupplied(InvalidDetailsSuppliedException e) {
        return new ResponseEntity<>(
                new ErrorResponse("Invalid details supplied"),
                HttpStatus.valueOf(400)
        );
    }

    @ExceptionHandler(InvalidUserCredentialsSuppliedException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUserCredentialsSupplied(InvalidUserCredentialsSuppliedException e) {
        return new ResponseEntity<>(
                new ErrorResponse("Invalid user credentials details supplied"),
                HttpStatus.valueOf(401)
        );
    }

    @ExceptionHandler(UserNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotAllowed(UserNotAllowedException e) {
        return new ResponseEntity<>(
                new ErrorResponse("The user is not allowed to access the transaction"),
                HttpStatus.valueOf(403)
        );
    }

    @ExceptionHandler(UserHasAccountsException.class)
    public ResponseEntity<ErrorResponse> handleUserNotAllowed(UserHasAccountsException e) {
        return new ResponseEntity<>(
                new ErrorResponse("A user cannot be deleted when they are associated with a bank account"),
                HttpStatus.valueOf(409)
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        return new ResponseEntity<>(
                new ErrorResponse("User was not found"),
                HttpStatus.valueOf(404)
        );
    }

    @ExceptionHandler(BankAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBankAccountNotFound(BankAccountNotFoundException e) {
        return new ResponseEntity<>(
                new ErrorResponse("Bank Account was not found"),
                HttpStatus.valueOf(404)
        );
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBankAccountNotFound(TransactionNotFoundException e) {
        return new ResponseEntity<>(
                new ErrorResponse("Transaction was not found"),
                HttpStatus.valueOf(404)
        );
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleBankAccountNotFound(InsufficientFundsException e) {
        return new ResponseEntity<>(
                new ErrorResponse("Insufficient funds to process transaction"),
                HttpStatus.valueOf(422)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BadRequestErrorResponse> handleUnexpected(Exception e) {
        return new ResponseEntity<>(
                new BadRequestErrorResponse()
                        .message("Unexpected error occurred")
                        .addDetailsItem(
                                new BadRequestErrorResponseDetailsInner(
                                        "500",
                                        e.getLocalizedMessage(),
                                        "UNEXPECTED_ERROR"
                                )
                        ),
                HttpStatus.valueOf(500)
        );
    }

}
