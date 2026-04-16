package com.example.lolserver.repository.member;

import com.example.lolserver.domain.member.domain.SocialAccount;
import com.example.lolserver.repository.member.entity.SocialAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SocialAccountMapper {

    @Mapping(target = "memberId", source = "member.id")
    SocialAccount toDomain(SocialAccountEntity entity);

    @Mapping(target = "member", ignore = true)
    SocialAccountEntity toEntity(SocialAccount socialAccount);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "member", ignore = true)
    @Mapping(target = "linkedAt", ignore = true)
    void updateEntityFromDomain(
            SocialAccount sa,
            @MappingTarget SocialAccountEntity entity);

    List<SocialAccount> toDomainList(List<SocialAccountEntity> entities);
}
