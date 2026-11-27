package com.noduco.orders.web;

import com.noduco.orders.service.IdempotencyInProgressException;
import com.noduco.orders.service.IdempotencyKeyReusedException;
import com.noduco.orders.service.OrderNotFoundException;
import com.noduco.orders.service.VersionMismatchException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.client.RestClientException;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ProblemDetail idempotencyInProgress(IdempotencyInProgressException e) {
        return problem(HttpStatus.CONFLICT, "idempotency-in-progress", "Request still in progress", e.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ProblemDetail idempotencyKeyReused(IdempotencyKeyReusedException e) {
        return problem(HttpStatus.valueOf(422), "idempotency-key-reused", "Idempotency-Key reused", e.getMessage());
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ProblemDetail> databaseUnavailable(DataAccessResourceFailureException e) {
        log.warn("Database unavailable: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "2")
                .body(problem(HttpStatus.SERVICE_UNAVAILABLE, "database-unavailable", "Database unavailable",
                        "The order database did not answer in time. Retry later with the same Idempotency-Key."));
    }


    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception e) {
        log.error("Unhandled exception", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal error",
                "Something went wrong on our side.");
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail gstUnavailable(RestClientException e) {
        log.warn("GST service failed: {}", e.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "gst-unavailable", "GST service unavailable",
                "The GST rate service failed or did not answer in time.");
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

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ex.getBody();
        body.setType(URI.create(BASE + "validation"));
        body.setTitle("Invalid request");
        body.setProperty("errors", ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> Map.of("field", String.valueOf(r.getMethodParameter().getParameterName()),
                                "message", String.valueOf(e.getDefaultMessage()))))
                .toList());
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail p) {
            p.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setType(URI.create(BASE + type));
        p.setTitle(title);
        p.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        return p;
    }
}




