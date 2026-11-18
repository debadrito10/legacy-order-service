package com.noduco.orders.service;

public class IdempotencyInProgressException extends RuntimeException {
    public IdempotencyInProgressException(String key) {
        super("A request with Idempotency-Key " + key + " is still being processed; retry later");
    }
}
