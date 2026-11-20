package com.noduco.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.noduco.orders.domain.OrderAction;
import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.domain.OrderStatus;
import com.noduco.orders.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@SpringBootTest(properties = "spring.mongodb.uri=mongodb://127.0.0.1:27018/orders-test")
class OrderCacheTests {

    @Autowired
    OrderService service;

    @Autowired
    MongoTemplate mongo;

    @Test
    void actionEvictsCachedOrder() {
        String id = service.create("Cache", 500).getId();
        assertEquals(OrderStatus.NEW, service.get(id).getStatus());

        service.apply(id, OrderAction.PAY, null);

        assertEquals(OrderStatus.PAID, service.get(id).getStatus());
    }

    @Test
    void writeThatBypassesTheServiceGivesStaleRead() {
        String id = service.create("Stale", 500).getId();
        assertEquals(OrderStatus.NEW, service.get(id).getStatus());

        mongo.updateFirst(Query.query(Criteria.where("_id").is(id)),
                Update.update("status", OrderStatus.CANCELLED), OrderDocument.class);

        assertEquals(OrderStatus.NEW, service.get(id).getStatus());
        assertEquals(OrderStatus.CANCELLED, mongo.findById(id, OrderDocument.class).getStatus());
    }
}
