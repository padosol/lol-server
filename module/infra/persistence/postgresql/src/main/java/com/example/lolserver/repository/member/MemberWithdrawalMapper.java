package com.example.lolserver.repository.member;

import com.example.lolserver.domain.member.domain.MemberWithdrawal;
import com.example.lolserver.repository.member.entity.MemberWithdrawalEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MemberWithdrawalMapper {

    MemberWithdrawal toDomain(MemberWithdrawalEntity entity);

    MemberWithdrawalEntity toEntity(MemberWithdrawal withdrawal);
}
