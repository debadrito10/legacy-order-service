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
        System.out.println("5. Bytes for Zo\u00eb in default charset: " + TextUtils.defaultCharsetSize("Zo\u00eb"));
        System.out.println("Done.");
    }
}
