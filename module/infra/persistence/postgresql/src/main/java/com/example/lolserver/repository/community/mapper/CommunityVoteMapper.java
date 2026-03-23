package com.example.lolserver.repository.community.mapper;

import com.example.lolserver.domain.community.domain.Vote;
import com.example.lolserver.domain.community.domain.vo.VoteTargetType;
import com.example.lolserver.domain.community.domain.vo.VoteType;
import com.example.lolserver.repository.community.entity.CommunityVoteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CommunityVoteMapper {

    @Mapping(target = "targetType", source = "targetType", qualifiedByName = "stringToTargetType")
    @Mapping(target = "voteType", source = "voteType", qualifiedByName = "stringToVoteType")
    Vote toDomain(CommunityVoteEntity entity);

    @Mapping(target = "targetType", source = "targetType", qualifiedByName = "targetTypeToString")
    @Mapping(target = "voteType", source = "voteType", qualifiedByName = "voteTypeToString")
    CommunityVoteEntity toEntity(Vote vote);

    @Named("stringToTargetType")
    default VoteTargetType stringToTargetType(String value) {
        return value != null ? VoteTargetType.valueOf(value) : null;
    }

    @Named("targetTypeToString")
    default String targetTypeToString(VoteTargetType value) {
        return value != null ? value.name() : null;
    }

    @Named("stringToVoteType")
    default VoteType stringToVoteType(String value) {
        return value != null ? VoteType.valueOf(value) : null;
    }

    @Named("voteTypeToString")
    default String voteTypeToString(VoteType value) {
        return value != null ? value.name() : null;
    }
}
