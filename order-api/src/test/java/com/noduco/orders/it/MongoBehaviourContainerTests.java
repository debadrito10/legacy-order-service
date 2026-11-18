package com.noduco.orders.it;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.noduco.orders.domain.IdempotencyRecord;
import com.noduco.orders.domain.IdempotencyRepository;
import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.domain.OrderRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/** Proves on a real, throwaway MongoDB what OrderServiceTests can only assume with mocks. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class MongoBehaviourContainerTests {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mirror.gcr.io/library/mongo:8.0").asCompatibleSubstituteFor("mongo"));

    @Autowired
    IdempotencyRepository idempotency;

    @Autowired
    OrderRepository orders;

    @Test
    void secondInsertOfTheSameKeyIsADuplicateKeyError() {
        IdempotencyRecord rec = new IdempotencyRecord("k1", "Asha|500", null, Instant.now());
        idempotency.insert(rec);
        assertThrows(DuplicateKeyException.class, () -> idempotency.insert(rec));
    }

    @Test
    void staleSaveIsRefusedByTheVersionCheck() {
        OrderDocument saved = orders.save(new OrderDocument("Asha", 500, "INR", Instant.now()));
        OrderDocument copyA = orders.findById(saved.getId()).orElseThrow();
        OrderDocument copyB = orders.findById(saved.getId()).orElseThrow();
        orders.save(copyA);
        assertThrows(OptimisticLockingFailureException.class, () -> orders.save(copyB));
    }
}

