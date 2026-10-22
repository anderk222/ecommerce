package com.anderk.ecommerce.tracking.adapter.out.persistence.activity;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores the metadata of an event as a single JSON document.
 */
@Converter
public class ActivityEventJsonConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot serialize the metadata of the event to JSON", exception);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot read the metadata of the event from JSON", exception);
        }
    }
}
