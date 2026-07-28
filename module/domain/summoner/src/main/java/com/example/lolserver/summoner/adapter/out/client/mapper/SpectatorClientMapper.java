package com.example.lolserver.summoner.adapter.out.client.mapper;

import com.example.lolserver.summoner.application.model.readmodel.BannedChampionReadModel;
import com.example.lolserver.summoner.application.model.readmodel.CurrentGameInfoReadModel;
import com.example.lolserver.summoner.application.model.readmodel.ParticipantReadModel;
import com.example.lolserver.summoner.application.model.readmodel.PerksReadModel;
import com.example.lolserver.summoner.adapter.out.client.restclient.spectator.model.BannedChampionVO;
import com.example.lolserver.summoner.adapter.out.client.restclient.spectator.model.CurrentGameInfoVO;
import com.example.lolserver.summoner.adapter.out.client.restclient.spectator.model.ParticipantVO;
import com.example.lolserver.summoner.adapter.out.client.restclient.spectator.model.PerksVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SpectatorClientMapper {

    SpectatorClientMapper INSTANCE = Mappers.getMapper(SpectatorClientMapper.class);

    @Mapping(target = "encryptionKey", source = "observers.encryptionKey")
    CurrentGameInfoReadModel toReadModel(CurrentGameInfoVO vo);

    @Mapping(target = "isBot", source = "bot")
    ParticipantReadModel toReadModel(ParticipantVO vo);

    PerksReadModel toReadModel(PerksVO vo);

    BannedChampionReadModel toReadModel(BannedChampionVO vo);
}
