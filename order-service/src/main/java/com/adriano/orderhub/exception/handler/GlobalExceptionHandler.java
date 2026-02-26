package com.adriano.orderhub.exception.handler;

import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.NotFound.class)
    public ProblemDetail handleFeignNotFound(FeignException.NotFound ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Some products were not found in the catalog"
        );
        problemDetail.setTitle("Product not found");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/invalid-product"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));

        problemDetail.setProperty("invalid_params", fieldErrors);
        return problemDetail;
    }

    @ExceptionHandler(RetryableException.class)
    public ProblemDetail handleConnectionRefused(RetryableException ex) {
        return createServiceUnavailableResponse("Unable to connect to the product catalog service. Please try again later.");
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ProblemDetail handleCircuitBreakerOpen(CallNotPermittedException ex) {
        return createServiceUnavailableResponse("Service is currently unavailable. Please try again later.");
    }

    private ProblemDetail createServiceUnavailableResponse(String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, detail);
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/service-unavailable"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
