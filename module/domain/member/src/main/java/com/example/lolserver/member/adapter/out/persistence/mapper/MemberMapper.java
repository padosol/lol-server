package com.example.lolserver.member.adapter.out.persistence.mapper;

import com.example.lolserver.member.domain.Member;
import com.example.lolserver.member.domain.SocialAccount;
import com.example.lolserver.member.adapter.out.persistence.entity.MemberEntity;
import com.example.lolserver.member.adapter.out.persistence.entity.SocialAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = SocialAccountMapper.class)
public interface MemberMapper {

    @Mapping(target = "socialAccounts", ignore = true)
    Member toDomain(MemberEntity entity);

    @Mapping(target = "socialAccounts", source = "socialAccounts")
    Member toDomainWithSocialAccounts(MemberEntity entity);

    @Mapping(target = "socialAccounts", ignore = true)
    MemberEntity toEntity(Member member);

    default void setSocialAccountRelationships(
            Member member, @MappingTarget MemberEntity entity) {
        if (!member.isSocialAccountsLoaded()
                || member.getSocialAccounts().isEmpty()) {
            return;
        }
        for (SocialAccount sa : member.getSocialAccounts()) {
            SocialAccountEntity saEntity = toSocialAccountEntity(sa);
            entity.addSocialAccount(saEntity);
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "socialAccounts", ignore = true)
    void updateEntityFromDomain(
            Member member, @MappingTarget MemberEntity entity);

    @Mapping(target = "member", ignore = true)
    SocialAccountEntity toSocialAccountEntity(SocialAccount sa);
}
