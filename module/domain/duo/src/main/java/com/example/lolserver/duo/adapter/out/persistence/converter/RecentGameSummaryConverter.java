package com.example.lolserver.duo.adapter.out.persistence.converter;

import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RecentGameSummaryConverter implements AttributeConverter<RecentGameSummary, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(RecentGameSummary attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize recent game summary", e);
        }
    }

    @Override
    public RecentGameSummary convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, RecentGameSummary.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize recent game summary", e);
        }
    }
}
