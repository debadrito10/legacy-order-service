package com.noduco.orders.service;

import com.noduco.orders.config.OrdersProperties;
import com.noduco.orders.domain.IdempotencyRecord;
import com.noduco.orders.domain.IdempotencyRepository;
import com.noduco.orders.domain.OrderAction;
import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.domain.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
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
    private final IdempotencyRepository idempotency;
    private final Clock clock;
    private final OrdersProperties properties;

    public OrderService(OrderRepository repository, IdempotencyRepository idempotency, Clock clock,
            OrdersProperties properties) {
        this.repository = repository;
        this.idempotency = idempotency;
        this.clock = clock;
        this.properties = properties;
    }

    public OrderDocument create(String customer, long amountPaise) {
        OrderDocument saved = repository.save(
                new OrderDocument(customer, amountPaise, properties.currency(), Instant.now(clock)));
        log.info("Created order {} on {}", saved.getId(), Thread.currentThread());
        return saved;
    }

    /** key = the Idempotency-Key header, or null. */
    public CreateResult create(String key, String customer, long amountPaise) {
        if (key == null) {
            return new CreateResult(create(customer, amountPaise), false);
        }
        String fingerprint = customer + "|" + amountPaise;

        // ATOMIC CLAIM: insert fails if the key (_id) already exists. One step, decided by MongoDB.
        IdempotencyRecord claim;
        try {
            claim = idempotency.insert(new IdempotencyRecord(key, fingerprint, null, Instant.now(clock)));
        } catch (DuplicateKeyException e) {
            return replay(idempotency.findById(key).orElseThrow(() -> new IdempotencyInProgressException(key)), fingerprint);
        }

        try {
            OrderDocument order = create(customer, amountPaise);
            idempotency.save(claim.withOrderId(order.getId()));
            return new CreateResult(order, false);
        } catch (RuntimeException e) {
            idempotency.deleteById(key);
            throw e;
        }
    }

    private CreateResult replay(IdempotencyRecord record, String fingerprint) {
        if (!record.fingerprint().equals(fingerprint)) {
            throw new IdempotencyKeyReusedException(record.key());
        }
        if (record.orderId() == null) {
            throw new IdempotencyInProgressException(record.key());
        }
        return new CreateResult(get(record.orderId()), true);
    }

    public OrderDocument addNote(String id, String note) {
        try {
            OrderDocument order = get(id);
            order.setNote(note);
            log.info("Note added to order {} for customer {}: {}", id, order.getCustomer(), note);
            return repository.save(order);
        } catch (Exception e) {
            return null;
        }
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


