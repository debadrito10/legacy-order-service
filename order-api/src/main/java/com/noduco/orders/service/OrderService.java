package com.noduco.orders.service;

import com.noduco.orders.domain.OrderAction;
import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.domain.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String CURRENCY = "INR";

    private final OrderRepository repository;
    private final Clock clock;

    public OrderService(OrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public OrderDocument create(String customer, long amountPaise) {
        OrderDocument saved = repository.save(
                new OrderDocument(customer, amountPaise, CURRENCY, Instant.now(clock)));
        log.info("Created order {} on {}", saved.getId(), Thread.currentThread());
        return saved;
    }

    public OrderDocument get(String id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public OrderDocument apply(String id, OrderAction action) {
        OrderDocument order = get(id);
        order.apply(action);
        return repository.save(order);
    }
}
