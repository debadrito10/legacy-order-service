package com.noduco.legacy;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class  TaxRules {
    // Business rule kept as JavaScript "so non-developers can edit it"
    private static final String GST_RULE = "amount * 0.18";

    public double gst(double amount) throws ScriptException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.put("amount", amount);
        Object result = engine.eval(GST_RULE);
        return ((Number) result).doubleValue();
    }
}
