package com.noduco.legacy;

import java.net.URI;

public class RateDemo {
    public static void main(String[] args) {
        System.out.println(new GstRateClient(URI.create(args[0])).fetchRate());
    }
}
