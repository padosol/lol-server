package com.example.lolserver.repository.duo.converter;

import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class MostChampionListConverter implements AttributeConverter<List<MostChampion>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<MostChampion> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize most champions", e);
        }
    }

    @Override
    public List<MostChampion> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize most champions", e);
        }
    }
}
