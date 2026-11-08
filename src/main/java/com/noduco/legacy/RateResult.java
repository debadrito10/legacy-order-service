package com.noduco.legacy;

public sealed interface RateResult {
    record Found(double rate) implements RateResult { }
    record Unavailable(String reason) implements RateResult { }
}
