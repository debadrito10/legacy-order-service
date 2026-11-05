package com.noduco.legacy;

import java.time.Instant;
import java.util.Objects;

/** Immutable snapshot of an order, used for JSON output. */
public record OrderDto(String id, String customer, double amount, Instant createdAt) {

    public OrderDto {   // compact constructor: checks run before the fields are set
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static OrderDto from(Order order) {
        return new OrderDto(order.getId(), order.getCustomer(), order.getAmount(),
                order.getCreatedAt().toInstant());
    }
}