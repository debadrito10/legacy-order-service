package com.noduco.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "noduco.orders")
public record OrdersProperties(String currency) { }
