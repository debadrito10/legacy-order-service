package com.noduco.legacy;

import jakarta.xml.bind.JAXBException;

public class OrderExportService {
    private final OrderXmlExporter xmlExporter = new OrderXmlExporter();

    public ExportResult export(Order order) {
        if (order.getAmount() <= 0) {
            return new ExportResult.Rejected(order.getId(), "amount must be positive");
        }
        try {
            return new ExportResult.Exported(order.getId(), xmlExporter.toXml(order));
        } catch (JAXBException e) {
            return new ExportResult.Rejected(order.getId(), "XML error: " + e.getMessage());
        }
    }
}
