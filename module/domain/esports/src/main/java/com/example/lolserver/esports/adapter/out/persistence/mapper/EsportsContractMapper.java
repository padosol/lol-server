package com.example.lolserver.esports.adapter.out.persistence.mapper;

import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsCareerEventEntity;
import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsContractEntity;
import com.example.lolserver.esports.domain.CareerEvent;
import com.example.lolserver.esports.domain.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** 계약·커리어 사건 ↔ 엔티티. enum 은 이름 문자열로 저장한다. 계약의 조직 ID 는 저장하지 않고 팀에서 구해 넘긴다. */
@Mapper(componentModel = "spring")
public interface EsportsContractMapper {

    @Mapping(target = "organizationId", source = "organizationId")
    Contract toDomain(EsportsContractEntity entity, String organizationId);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EsportsContractEntity toEntity(Contract contract);

    CareerEvent toDomain(EsportsCareerEventEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    EsportsCareerEventEntity toEntity(CareerEvent event);
}
