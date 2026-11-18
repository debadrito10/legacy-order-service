package com.noduco.orders.service;

public class VersionMismatchException extends RuntimeException {
    public VersionMismatchException(String id, String expected, Long actual) {
        super("Order " + id + " is at version " + actual + ", If-Match was " + expected);
    }
}
