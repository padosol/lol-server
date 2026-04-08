package com.example.lolserver.repository.member;

import com.example.lolserver.domain.member.domain.SocialAccount;
import com.example.lolserver.repository.member.entity.SocialAccountEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SocialAccountMapper {

    SocialAccount toDomain(SocialAccountEntity entity);

    SocialAccountEntity toEntity(SocialAccount socialAccount);

    List<SocialAccount> toDomainList(List<SocialAccountEntity> entities);
}
