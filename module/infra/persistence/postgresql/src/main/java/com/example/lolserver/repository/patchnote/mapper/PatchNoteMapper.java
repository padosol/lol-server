package com.example.lolserver.repository.patchnote.mapper;

import com.example.lolserver.domain.patchnote.application.model.PatchNoteReadModel;
import com.example.lolserver.domain.patchnote.application.model.PatchNoteSummaryReadModel;
import com.example.lolserver.domain.patchnote.application.model.patchnote.PatchNoteContent;
import com.example.lolserver.repository.patchnote.entity.PatchNoteEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PatchNoteMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper;

    public PatchNoteSummaryReadModel toSummaryReadModel(PatchNoteEntity entity) {
        return new PatchNoteSummaryReadModel(
                entity.getVersionId(),
                entity.getTitle(),
                formatDate(entity.getCreatedAt())
        );
    }

    public List<PatchNoteSummaryReadModel> toSummaryReadModelList(List<PatchNoteEntity> entities) {
        return entities.stream()
                .map(this::toSummaryReadModel)
                .toList();
    }

    public PatchNoteReadModel toReadModel(PatchNoteEntity entity) {
        PatchNoteContent content = parseContent(entity.getContent());
        return new PatchNoteReadModel(
                entity.getVersionId(),
                entity.getTitle(),
                content,
                entity.getPatchUrl(),
                formatDate(entity.getCreatedAt())
        );
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : null;
    }

    private PatchNoteContent parseContent(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(contentJson, PatchNoteContent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("패치노트 content 파싱 실패", e);
        }
    }
}
