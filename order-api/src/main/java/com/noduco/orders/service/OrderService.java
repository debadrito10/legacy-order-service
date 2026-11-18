package com.noduco.orders.service;

import com.noduco.orders.config.OrdersProperties;
import com.noduco.orders.domain.OrderAction;
import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.domain.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final Clock clock;
    private final OrdersProperties properties;

    public OrderService(OrderRepository repository, Clock clock, OrdersProperties properties) {
        this.repository = repository;
        this.clock = clock;
        this.properties = properties;
    }

    public OrderDocument create(String customer, long amountPaise) {
        OrderDocument saved = repository.save(
                new OrderDocument(customer, amountPaise, properties.currency(), Instant.now(clock)));
        log.info("Created order {} on {}", saved.getId(), Thread.currentThread());
        return saved;
    }

    public OrderDocument get(String id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public Page<OrderDocument> list(int page, int size) {
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));
        return repository.findAll(PageRequest.of(page, size, newestFirst));
    }

    /** ifMatch = the If-Match header value, or null when the client sent none. */
    public OrderDocument apply(String id, OrderAction action, String ifMatch) {
        OrderDocument order = get(id);
        if (ifMatch != null && !ifMatch.replace("W/", "").replace("\"", "").trim().equals(String.valueOf(order.getVersion()))) {
            throw new VersionMismatchException(id, ifMatch, order.getVersion());
        }
        order.apply(action);
        return repository.save(order);
    }
}

