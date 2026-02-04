package com.example.lolserver.repository.tiercutoff.mapper;

import com.example.lolserver.domain.tiercutoff.application.model.TierCutoffReadModel;
import com.example.lolserver.repository.tiercutoff.entity.TierCutoffEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TierCutoffMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TierCutoffReadModel toReadModel(TierCutoffEntity entity) {
        return new TierCutoffReadModel(
                entity.getId(),
                entity.getQueue(),
                entity.getTier(),
                entity.getRegion(),
                entity.getMinLeaguePoints(),
                formatDateTime(entity.getUpdatedAt())
        );
    }

    public List<TierCutoffReadModel> toReadModelList(List<TierCutoffEntity> entities) {
        return entities.stream()
                .map(this::toReadModel)
                .toList();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
}
