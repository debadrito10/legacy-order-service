package com.noduco.legacy;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InMemoryOrderRepositoryTest {
    private final InMemoryOrderRepository repo = new InMemoryOrderRepository(List.of(
            new Order("101", "Asha", 100.0),
            new Order("102", "Ravi", 200.0),
            new Order("103", "Asha", 50.0)));

    @Test
    public void findsAnExistingOrder() {
        assertEquals("Ravi", repo.findById("102").orElseThrow().getCustomer());
    }

    @Test
    public void missingOrderIsEmptyNotNull() {
        assertTrue(repo.findById("999").isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void customerResultCannotBeModified() {
        repo.findByCustomer("Asha").add(new Order("104", "Asha", 1.0));
    }

    @Test
    public void totalsAllAmounts() {
        assertEquals(350.0, repo.totalAmount(), 0.0001);
    }
}
