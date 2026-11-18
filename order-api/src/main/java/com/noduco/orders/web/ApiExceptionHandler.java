package com.noduco.orders.web;

import com.noduco.orders.service.OrderNotFoundException;
import com.noduco.orders.service.VersionMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    static final String BASE = "https://noduco.example/problems/";

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail notFound(OrderNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "order-not-found", "Order not found", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail invalidTransition(IllegalStateException e) {
        return problem(HttpStatus.CONFLICT, "invalid-transition", "Action not allowed", e.getMessage());
    }

    @ExceptionHandler(VersionMismatchException.class)
    public ProblemDetail versionMismatch(VersionMismatchException e) {
        return problem(HttpStatus.PRECONDITION_FAILED, "version-mismatch", "Order has changed", e.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail concurrentUpdate(OptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "concurrent-modification", "Order changed during the request", "Another request changed this order at the same time. Read it again and retry.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception e) {
        log.error("Unhandled exception", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal error",
                "Something went wrong on our side.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ex.getBody();
        body.setType(URI.create(BASE + "validation"));
        body.setTitle("Invalid request");
        body.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message", String.valueOf(f.getDefaultMessage())))
                .toList());
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setType(URI.create(BASE + type));
        p.setTitle(title);
        return p;
    }
}

