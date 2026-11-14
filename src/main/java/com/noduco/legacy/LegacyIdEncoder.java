package com.noduco.legacy;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class LegacyIdEncoder {
    public String encode(String id) {
        return  Base64.getEncoder().encodeToString(id.getBytes(StandardCharsets.UTF_8));
    }
}
