package com.noduco.legacy;

/** Every possible outcome of exporting an order. The list is closed. */
public sealed interface ExportResult permits ExportResult.Exported, ExportResult.Rejected {

    record Exported(String orderId, String xml) implements ExportResult { }

    record Rejected(String orderId, String reason) implements ExportResult { }
}
