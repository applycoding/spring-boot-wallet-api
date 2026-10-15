package com.applycoding.ledger.ledger;

import java.util.regex.Pattern;

public final class IdempotencyKeys {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9_.:\\-]{8,200}$");

    private IdempotencyKeys() {
    }

    public static String require(String header) {
        if (header == null || !PATTERN.matcher(header).matches()) {
            throw new MissingIdempotencyKeyException();
        }
        return header;
    }
}
