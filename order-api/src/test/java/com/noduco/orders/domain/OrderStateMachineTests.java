package com.noduco.orders.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderStateMachineTests {

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @CsvSource({
            "NEW, PAY, PAID",
            "NEW, CANCEL, CANCELLED",
            "PAID, SHIP, SHIPPED",
            "PAID, CANCEL, CANCELLED"
    })
    void allowedTransitions(OrderStatus from, OrderAction action, OrderStatus expected) {
        assertEquals(expected, OrderStateMachine.next(from, action));
    }

    @ParameterizedTest(name = "{0} + {1} is refused")
    @CsvSource({
            "NEW, SHIP",
            "PAID, PAY",
            "SHIPPED, PAY", "SHIPPED, CANCEL", "SHIPPED, SHIP",
            "CANCELLED, PAY", "CANCELLED, CANCEL", "CANCELLED, SHIP"
    })
    void refusedTransitions(OrderStatus from, OrderAction action) {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> OrderStateMachine.next(from, action));
        assertTrue(e.getMessage().contains(action + " an order that is " + from), e.getMessage());
    }

    @Test
    void everyCombinationIsCovered() {
        Set<String> listed = Set.of("NEW-PAY", "NEW-CANCEL", "PAID-SHIP", "PAID-CANCEL",
                "NEW-SHIP", "PAID-PAY", "SHIPPED-PAY", "SHIPPED-CANCEL", "SHIPPED-SHIP",
                "CANCELLED-PAY", "CANCELLED-CANCEL", "CANCELLED-SHIP");
        for (OrderStatus s : OrderStatus.values()) {
            for (OrderAction a : OrderAction.values()) {
                assertTrue(listed.contains(s + "-" + a), "new combination not tested: " + s + " + " + a);
            }
        }
    }
}
