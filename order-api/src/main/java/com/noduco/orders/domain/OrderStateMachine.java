package com.noduco.orders.domain;

public final class OrderStateMachine {

    private OrderStateMachine() { }   // only static methods: nobody creates an instance

    public static OrderStatus next(OrderStatus from, OrderAction action) {
        return switch (from) {
            case NEW -> switch (action) {
                case PAY -> OrderStatus.PAID;
                case CANCEL -> OrderStatus.CANCELLED;
                case SHIP -> throw notAllowed(from, action);
            };
            case PAID -> switch (action) {
                case SHIP -> OrderStatus.SHIPPED;
                case CANCEL -> OrderStatus.CANCELLED;
                case PAY -> throw notAllowed(from, action);
            };
            case SHIPPED, CANCELLED -> throw notAllowed(from, action);
        };
    }

    private static IllegalStateException notAllowed(OrderStatus from, OrderAction action) {
        return new IllegalStateException("Cannot " + action + " an order that is " + from);
    }
}
