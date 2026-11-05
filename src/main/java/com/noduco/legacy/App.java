package com.noduco.legacy;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Running on Java " + System.getProperty("java.version"));
        Order order = new Order("101", "Asha", 100.0);

        System.out.println("1. XML export:");
        System.out.println(new OrderXmlExporter().toXml(order));
        System.out.println("2. Encoded id: " + new LegacyIdEncoder().encode(order.getId()));
        System.out.println("3. GST: " + new TaxRules().gst(order.getAmount()));
        System.out.println("4. Fast length of customer: " + TextUtils.fastLength(order.getCustomer()));
        System.out.println("5. Bytes for Zo\u00eb in UTF-8: " + TextUtils.utf8Size("Zo\u00eb"));
        String exportMessage = switch (new OrderExportService().export(order)) {
            case ExportResult.Exported(String id, String xml) -> "exported order " + id;
            case ExportResult.Rejected(String id, String reason) -> "rejected order " + id + ": " + reason;
        };
        System.out.println("6. Export result: " + exportMessage);
        System.out.println("Done.");
    }
}
