package com.noduco.legacy;

import java.lang.reflect.Field;

public class TextUtils {
    // "Fast" length: reads String's private internal array directly (a hack copied from an old blog)
    public static int fastLength(String s) {
        try {
            Field value = String.class.getDeclaredField("value");
            value.setAccessible(true);
            char[] chars = (char[]) value.get(s);
            return chars.length;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // Number of bytes when text is saved with the platform's default charset
    public static int defaultCharsetSize(String s) {
        return s.getBytes().length;
    }
}
