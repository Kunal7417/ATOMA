package com.atoma.marketplace.onboarding.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.UUID;

/** Treat JSON "" as null for optional UUID fields (mobile contract). */
public class LenientUuidDeserializer extends JsonDeserializer<UUID> {

    @Override
    public UUID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        var text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        return UUID.fromString(text.trim());
    }
}
