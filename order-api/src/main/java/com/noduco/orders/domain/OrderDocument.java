package com.noduco.orders.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

@Document("orders")
@CompoundIndex(name = "customer_status_createdAt", def = "{'customer': 1, 'status': 1, 'createdAt': -1}")
public class OrderDocument {

    @Id
    private String id;
    private String customer;
    private long amountPaise;
    private String currency;
    private OrderStatus status;
    private Instant createdAt;
    @Version
    private Long version;

    protected OrderDocument() { }

    public OrderDocument(String customer, long amountPaise, String currency, Instant createdAt) {
        this.customer = customer;
        this.amountPaise = amountPaise;
        this.currency = currency;
        this.status = OrderStatus.NEW;
        this.createdAt = createdAt;
    }

    public void apply(OrderAction action) {
        this.status = OrderStateMachine.next(status, action);
    }

    public String getId() { return id; }
    public String getCustomer() { return customer; }
    public long getAmountPaise() { return amountPaise; }
    public String getCurrency() { return currency; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}

