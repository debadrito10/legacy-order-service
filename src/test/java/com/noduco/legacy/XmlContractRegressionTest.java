package com.noduco.legacy;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XmlContractRegressionTest {
    private static final Pattern CREATED_AT = Pattern.compile("<createdAt>([^<]*)</createdAt>");

    // createdAt depends on "now" and on the machine's time zone, so the structure test masks it
    private static String normalise(String xml) {
        return CREATED_AT.matcher(xml.replace("\r\n", "\n")).replaceAll("<createdAt>MASKED</createdAt>").strip();
    }

    @Test
    public void xmlStructureMatchesTheJava8Baseline() throws Exception {
        String expected;
        try (InputStream in = getClass().getResourceAsStream("/golden/order-java8.xml")) {
            expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String actual = new OrderXmlExporter().toXml(new Order("101", "Asha", 100.0));
        assertEquals(normalise(expected), normalise(actual));
    }

    // The createdAt text changes with the machine's zone (+05:30 here, Z or +00:00 on CI),
    // but the moment in time it describes must not change
    @Test
    public void createdAtKeepsTheSameInstantInAnyZone() throws Exception {
        long millis = 1_794_670_000_123L;
        String xml = new OrderXmlExporter().toXml(new Order("101", "Asha", 100.0, new Date(millis)));
        Matcher m = CREATED_AT.matcher(xml);
        assertTrue("createdAt element missing", m.find());
        assertEquals(millis, OffsetDateTime.parse(m.group(1)).toInstant().toEpochMilli());
    }
}
