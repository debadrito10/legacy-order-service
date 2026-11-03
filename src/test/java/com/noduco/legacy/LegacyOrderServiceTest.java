package com.noduco.legacy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyOrderServiceTest {
    private final Order order = new Order("101", "Asha", 100.0);

    @Test
    public void xmlContainsId() throws Exception {
        assertTrue(new OrderXmlExporter().toXml(order).contains("<id>101</id>"));
    }

    @Test
    public void jsonContainsCustomer() throws Exception {
        assertTrue(new OrderJsonExporter().toJson(order).contains("\"customer\":\"Asha\""));
    }

    @Test
    public void encodesIdAsBase64() {
        assertEquals("MTAx", new LegacyIdEncoder().encode("101"));
    }

    @Test
    public void gstIsEighteenPercent() throws Exception {
        assertEquals(18.0, new TaxRules().gst(100.0), 0.0001);
    }
}
