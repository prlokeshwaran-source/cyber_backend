package com.example.cyber.common.api;

import com.example.cyber.common.exception.InsufficientFundsException;
import com.example.cyber.common.exception.ResourceNotFoundException;
import com.example.cyber.transaction.application.IdempotencyConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.dao.DataAccessException;
import com.example.cyber.security.signature.TransactionSigningUnavailableException;
import com.example.cyber.security.signature.CustomerSignatureException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail handleInsufficientFunds(InsufficientFundsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({IdempotencyConflictException.class, OptimisticLockingFailureException.class})
    ProblemDetail handleConflict(RuntimeException exception) {
        String detail = exception instanceof IdempotencyConflictException
                ? exception.getMessage()
                : "Account balance changed concurrently; retry with the same idempotency key";
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail handleBadRequest(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body or path parameter is invalid");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED,
                "This HTTP method is not supported for the requested endpoint");
    }

    @ExceptionHandler(DataAccessException.class)
    ProblemDetail handleDatabaseUnavailable(DataAccessException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Database service is temporarily unavailable");
    }

    @ExceptionHandler(TransactionSigningUnavailableException.class)
    ProblemDetail handleSigningUnavailable(TransactionSigningUnavailableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Transaction signing keys are not configured; money movement is disabled");
    }

    @ExceptionHandler(CustomerSignatureException.class)
    ProblemDetail handleInvalidCustomerSignature(CustomerSignatureException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred");
    }
}
