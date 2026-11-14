package com.noduco.legacy;

import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.security.Security;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TlsDefaultsRegressionTest {
    @Test
    public void oldTlsIsOffAndTls13IsOn() throws Exception {
        List<String> enabled = Arrays.asList(
                SSLContext.getDefault().getDefaultSSLParameters().getProtocols());
        System.out.println("Default TLS protocols: " + enabled);
        assertFalse("TLSv1 must be off", enabled.contains("TLSv1"));
        assertFalse("TLSv1.1 must be off", enabled.contains("TLSv1.1"));
        assertTrue("TLSv1.3 must be on", enabled.contains("TLSv1.3"));
    }

    @Test
    public void securityPolicyStillDisablesOldTls() {
        String disabled = Security.getProperty("jdk.tls.disabledAlgorithms");
        assertTrue(disabled, disabled.contains("TLSv1.1"));
    }
}
