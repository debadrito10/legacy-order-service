package com.noduco.orders.domain;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<OrderDocument, String> {

    List<OrderDocument> findByCustomerAndStatusOrderByCreatedAtDesc(String customer, OrderStatus status, Pageable pageable);
}