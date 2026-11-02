package com.ledgerline;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns every module's {@link DomainException} into an RFC 9457 problem detail
 * ({@code application/problem+json}). Spring's own errors (malformed JSON, a missing header, an id
 * that isn't a UUID) use the same format because {@code spring.mvc.problemdetails.enabled} is on.
 */
@RestControllerAdvice
class ProblemDetailsAdvice {

    @ExceptionHandler
    ProblemDetail domainException(DomainException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusOf(exception.kind()), exception.getMessage());
        problem.setTitle(exception.title());
        return problem;
    }

    private static HttpStatus statusOf(DomainException.Kind kind) {
        return switch (kind) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case RULE_VIOLATION -> HttpStatus.UNPROCESSABLE_CONTENT;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
