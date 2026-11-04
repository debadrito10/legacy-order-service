package com.noduco.legacy;

public class TextUtils {
    // this was a reflection hack reading String's private 'value' field.
    // Blocked since JDK 16/17, and since JDK 9 the field is byte[] (compact strings), so the cast would fail anyway.
    public static int fastLength(String s) {
        return s.length();
    }

    // Number of bytes when text is saved with the platform's default charset
    public static int defaultCharsetSize(String s) {
        return s.getBytes().length;
    }
}