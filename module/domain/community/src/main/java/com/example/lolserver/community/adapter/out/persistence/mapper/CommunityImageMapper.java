package com.example.lolserver.community.adapter.out.persistence.mapper;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityImageEntity;
import com.example.lolserver.community.domain.PostImage;
import org.mapstruct.Mapper;

/**
 * {@code ImageStatus} ↔ {@code String} 변환은 MapStruct 가 {@code name()}/{@code valueOf()} 로
 * 자동 처리한다(엔티티가 상태를 문자열 컬럼으로 저장하는 이유는 엔티티 주석 참고).
 */
@Mapper(componentModel = "spring")
public interface CommunityImageMapper {

    PostImage toDomain(CommunityImageEntity entity);

    CommunityImageEntity toEntity(PostImage image);
}
