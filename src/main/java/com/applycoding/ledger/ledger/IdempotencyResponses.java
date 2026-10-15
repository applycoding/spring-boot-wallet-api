package com.applycoding.ledger.ledger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class IdempotencyResponses {

    private IdempotencyResponses() {
    }

    public static <T> T read(IdempotencyRecord record, Class<T> type, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(record.getResponseBody(), type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored idempotent response could not be read", ex);
        }
    }

    public static String write(Object value, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not store idempotent response", ex);
        }
    }
}
