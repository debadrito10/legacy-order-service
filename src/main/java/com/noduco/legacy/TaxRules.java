package com.noduco.legacy;

public class TaxRules {
    // the GST rule was JavaScript run by Nashorn (removed in JDK 15; engine became null).
    // Moved to Java. Decision: non-developers can no longer edit the rule; a rate change needs a release.
    private static final double GST_RATE = 0.18;

    public double gst(double amount) {
        return amount * GST_RATE;
    }
}
