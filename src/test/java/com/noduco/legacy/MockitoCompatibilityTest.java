package com.noduco.legacy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MockitoCompatibilityTest {
    @Test
    public void canMockAConcreteExporter() throws Exception {
        OrderXmlExporter exporter = mock(OrderXmlExporter.class);
        when(exporter.toXml(any(Order.class))).thenReturn("<order/>");

        assertEquals("<order/>", exporter.toXml(new Order("1", "Test", 1.0)));
        verify(exporter).toXml(any(Order.class));
    }
}
