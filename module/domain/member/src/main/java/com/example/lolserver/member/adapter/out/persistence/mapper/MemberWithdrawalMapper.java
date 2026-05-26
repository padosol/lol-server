package com.example.lolserver.member.adapter.out.persistence.mapper;

import com.example.lolserver.member.domain.MemberWithdrawal;
import com.example.lolserver.member.adapter.out.persistence.entity.MemberWithdrawalEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MemberWithdrawalMapper {

    MemberWithdrawal toDomain(MemberWithdrawalEntity entity);

    MemberWithdrawalEntity toEntity(MemberWithdrawal withdrawal);
}
