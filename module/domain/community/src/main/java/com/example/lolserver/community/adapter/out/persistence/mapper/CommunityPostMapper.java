package com.example.lolserver.community.adapter.out.persistence.mapper;

import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityPostEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 카테고리만 자동 매핑이 되지 않는다. 도메인은 code, 엔티티는 id 를 들고 있어(V33)
 * 이름도 타입도 맞지 않으므로, 변환된 값을 두 번째 인자로 받아 채운다.
 * 실제 변환은 {@code CategoryCodeResolver} 가, 호출은 어댑터가 한다 —
 * 매퍼가 리포지토리를 알면 순수 변환이 아니게 된다.
 */
@Mapper(componentModel = "spring")
public interface CommunityPostMapper {

    @Mapping(target = "category", source = "categoryCode")
    Post toDomain(CommunityPostEntity entity, String categoryCode);

    @Mapping(target = "categoryId", source = "categoryId")
    CommunityPostEntity toEntity(Post post, Long categoryId);
}
