package com.ledgerline.wallets;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns wallet errors into RFC 9457 problem details ({@code application/problem+json}), the error
 * format the API contract promises. Malformed requests (an unknown currency, an id that is not a
 * UUID) are answered in the same format by Spring itself, because {@code
 * spring.mvc.problemdetails.enabled} is on.
 */
@RestControllerAdvice(assignableTypes = WalletController.class)
class WalletExceptionHandler {

    @ExceptionHandler
    ProblemDetail walletNotFound(WalletNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Wallet not found");
        return problem;
    }

    @ExceptionHandler
    ProblemDetail walletAlreadyOpen(WalletAlreadyOpenException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Wallet already open");
        return problem;
    }
}
