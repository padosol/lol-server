package com.example.lolserver.repository.member;

import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.repository.member.entity.MemberEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    Member toDomain(MemberEntity entity);

    MemberEntity toEntity(Member member);
}
