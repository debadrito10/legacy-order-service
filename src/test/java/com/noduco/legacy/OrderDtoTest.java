package com.noduco.legacy;

import org.junit.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class OrderDtoTest {

    @Test
    public void equalDtosAreEqualAndHashTheSame() {
        OrderDto a = new OrderDto("101", "Asha", 100.0, Instant.EPOCH);
        OrderDto b = new OrderDto("101", "Asha", 100.0, Instant.EPOCH);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeAmount() {
        new OrderDto("101", "Asha", -1.0, Instant.EPOCH);
    }

    @Test
    public void jsonUsesIsoInstant() throws Exception {
        OrderDto dto = OrderDto.from(new Order("101", "Asha", 100.0, new Date(0L)));
        String expected = """
                {"id":"101","customer":"Asha","amount":100.0,"createdAt":"1970-01-01T00:00:00Z"}""";
        assertEquals(expected, new OrderDtoJsonExporter().toJson(dto));
    }
}