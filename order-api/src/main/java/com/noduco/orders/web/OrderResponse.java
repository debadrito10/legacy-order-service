package com.noduco.orders.web;

import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.domain.OrderStatus;
import java.time.Instant;

public record OrderResponse(String id, String customer, long amountPaise, String currency,
                            OrderStatus status, Instant createdAt) {

    static OrderResponse from(OrderDocument o) {
        return new OrderResponse(o.getId(), o.getCustomer(), o.getAmountPaise(), o.getCurrency(),
                o.getStatus(), o.getCreatedAt());
    }
}
