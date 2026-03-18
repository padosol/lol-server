package com.example.lolserver.repository.member;

import com.example.lolserver.domain.member.domain.RiotAccountLink;
import com.example.lolserver.repository.member.entity.RiotAccountLinkEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RiotAccountLinkMapper {

    RiotAccountLink toDomain(RiotAccountLinkEntity entity);

    RiotAccountLinkEntity toEntity(RiotAccountLink link);

    List<RiotAccountLink> toDomainList(List<RiotAccountLinkEntity> entities);
}
