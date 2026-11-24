package com.noduco.orders.domain;

import java.time.Instant;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdempotencyRepository extends MongoRepository<IdempotencyRecord, String> {

    long deleteByKeyAndCreatedAtAndOrderIdIsNull(String key, Instant createdAt);

}
