package com.noduco.legacy;

import org.junit.Test;

import static com.noduco.legacy.OrderAction.*;
import static com.noduco.legacy.OrderStatus.*;
import static org.junit.Assert.assertEquals;

public class OrderStateMachineTest {

    @Test public void newCanBePaid()       { assertEquals(PAID, OrderStateMachine.next(NEW, PAY)); }
    @Test public void paidCanBeShipped()   { assertEquals(SHIPPED, OrderStateMachine.next(PAID, SHIP)); }
    @Test public void newCanBeCancelled()  { assertEquals(CANCELLED, OrderStateMachine.next(NEW, CANCEL)); }

    @Test(expected = IllegalStateException.class)
    public void newCannotBeShipped()       { OrderStateMachine.next(NEW, SHIP); }

    @Test(expected = IllegalStateException.class)
    public void shippedCannotBeCancelled() { OrderStateMachine.next(SHIPPED, CANCEL); }
}