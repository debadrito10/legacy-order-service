package com.noduco.legacy;

import sun.misc.BASE64Encoder;
import java.nio.charset.StandardCharsets;

public class LegacyIdEncoder {
    public String encode(String id) {
        return new BASE64Encoder().encode(id.getBytes(StandardCharsets.UTF_8));
    }
}
