package com.noduco.orders.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** One Idempotency-Key. orderId is null while the first request is still creating the order. */
@Document("idempotency_keys")
public record IdempotencyRecord(@Id String key, String fingerprint, String orderId, Instant createdAt) {

    public IdempotencyRecord withOrderId(String id) {
        return new IdempotencyRecord(key, fingerprint, id, createdAt);
    }
}
