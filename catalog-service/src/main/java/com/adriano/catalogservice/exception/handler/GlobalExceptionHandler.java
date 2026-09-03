package com.adriano.catalogservice.exception.handler;

import com.adriano.catalogservice.exception.product.ForbiddenException;
import com.adriano.catalogservice.exception.product.InsufficientStockException;
import com.adriano.catalogservice.exception.product.ProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        problemDetail.setTitle("Product not found");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());

        problemDetail.setTitle("Forbidden");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/forbidden"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        log.warn("Insufficient stock: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

        problemDetail.setTitle("Insufficient stock");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/insufficient-stock"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Validation exception");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));
        problemDetail.setProperty("invalid_params", fieldErrors);
        return super.handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            ServletRequestBindingException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Cobre MissingRequestHeaderException etc.
        log.warn("Bad request: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return super.handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Corpo JSON malformado ou faltando campo obrigatório.
        log.warn("Malformed request body: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return super.handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception while processing request", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.orderhub.com/errors/internal-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
