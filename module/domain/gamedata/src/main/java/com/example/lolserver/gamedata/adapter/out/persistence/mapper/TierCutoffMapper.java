package com.example.lolserver.gamedata.adapter.out.persistence.mapper;

import com.example.lolserver.gamedata.application.model.TierCutoffReadModel;
import com.example.lolserver.gamedata.adapter.out.persistence.entity.TierCutoffEntity;
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
                entity.getPlatformId(),
                entity.getMinLeaguePoints(),
                entity.getLpChange(),
                entity.getUserCount(),
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
