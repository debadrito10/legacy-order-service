package com.noduco.orders.service;

import com.noduco.orders.domain.OrderDocument;

public record CreateResult(OrderDocument order, boolean replayed) { }
