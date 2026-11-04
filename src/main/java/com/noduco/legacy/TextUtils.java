package com.noduco.legacy;

import java.nio.charset.StandardCharsets;

public class TextUtils {
    // R5: was a reflection hack reading String's private 'value' field.
    // Blocked since JDK 16/17, and since JDK 9 the field is byte[] (compact strings), so the cast would fail anyway.
    public static int fastLength(String s) {
        return s.length();
    }

    // R6: charset made explicit. The platform default was Cp1252 on Java 8/Windows and is UTF-8 from JDK 18.
    public static int utf8Size(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }
}