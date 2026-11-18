package com.noduco.orders.web;

import com.noduco.orders.domain.OrderAction;
import com.noduco.orders.domain.OrderDocument;
import com.noduco.orders.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderDocument order = service.create(request.customer(), request.amountPaise());
        return ResponseEntity.created(URI.create("/orders/" + order.getId()))
                .eTag(String.valueOf(order.getVersion()))
                .body(OrderResponse.from(order));
    }

    @GetMapping
    public PageResponse list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(service.list(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable String id) {
        return withVersion(service.get(id));
    }

    @PostMapping("/{id}/actions/{action}")
    public ResponseEntity<OrderResponse> act(@PathVariable String id, @PathVariable OrderAction action,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return withVersion(service.apply(id, action, ifMatch));
    }

    private static ResponseEntity<OrderResponse> withVersion(OrderDocument order) {
        return ResponseEntity.ok()
                .eTag(String.valueOf(order.getVersion()))
                .cacheControl(CacheControl.noCache())
                .body(OrderResponse.from(order));
    }
}

