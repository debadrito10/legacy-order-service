package com.noduco.orders.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotBlank @Size(max = 100) String customer,
        @Positive @Max(100_000_000) long amountPaise) { }
