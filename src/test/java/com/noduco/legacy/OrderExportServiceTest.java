package com.noduco.legacy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderExportServiceTest {
    private final OrderExportService service = new OrderExportService();

    @Test
    public void positiveAmountIsExported() {
        ExportResult result = service.export(new Order("101", "Asha", 100.0));
        assertTrue(result instanceof ExportResult.Exported e && e.xml().contains("<id>101</id>"));
    }

    @Test
    public void zeroAmountIsRejected() {
        ExportResult result = service.export(new Order("102", "Ravi", 0.0));
        assertEquals(new ExportResult.Rejected("102", "amount must be positive"), result);
    }
}
