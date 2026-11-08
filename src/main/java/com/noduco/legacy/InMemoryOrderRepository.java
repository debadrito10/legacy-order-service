package com.noduco.legacy;

import java.util.List;
import java.util.Optional;

public class InMemoryOrderRepository {
    private final List<Order> orders;

    public InMemoryOrderRepository(List<Order> orders) {
        this.orders = List.copyOf(orders);   // our own unmodifiable copy (Java 10)
    }

    public Optional<Order> findById(String id) {
        return orders.stream()
                .filter(o -> o.getId().equals(id))
                .findFirst();
    }

    public List<Order> findByCustomer(String customer) {
        return orders.stream()
                .filter(o -> o.getCustomer().equals(customer))
                .toList();                       // Java 16: unmodifiable result
    }

    public double totalAmount() {
        return orders.stream()
                .mapToDouble(Order::getAmount)   // method reference = o -> o.getAmount()
                .sum();
    }
}
