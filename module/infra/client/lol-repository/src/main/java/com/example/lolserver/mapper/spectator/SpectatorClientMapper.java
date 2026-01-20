package com.example.lolserver.mapper.spectator;

import com.example.lolserver.domain.spectator.application.model.BannedChampionReadModel;
import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.model.ParticipantReadModel;
import com.example.lolserver.domain.spectator.application.model.PerksReadModel;
import com.example.lolserver.restclient.spectator.model.BannedChampionVO;
import com.example.lolserver.restclient.spectator.model.CurrentGameInfoVO;
import com.example.lolserver.restclient.spectator.model.ParticipantVO;
import com.example.lolserver.restclient.spectator.model.PerksVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SpectatorClientMapper {

    SpectatorClientMapper INSTANCE = Mappers.getMapper(SpectatorClientMapper.class);

    CurrentGameInfoReadModel toReadModel(CurrentGameInfoVO vo);

    @Mapping(target = "isBot", source = "bot")
    ParticipantReadModel toReadModel(ParticipantVO vo);

    PerksReadModel toReadModel(PerksVO vo);

    BannedChampionReadModel toReadModel(BannedChampionVO vo);
}
