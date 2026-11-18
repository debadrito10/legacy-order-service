package com.noduco.orders.web;

import com.noduco.orders.domain.OrderDocument;
import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse(List<OrderResponse> items, int page, int size, long totalElements, int totalPages) {

    static PageResponse from(Page<OrderDocument> result) {
        return new PageResponse(result.getContent().stream().map(OrderResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
