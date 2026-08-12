package com.example.lolserver.community.adapter.out.persistence.repository;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBoardGroupI18nEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityBoardGroupI18nJpaRepository
        extends JpaRepository<CommunityBoardGroupI18nEntity, CommunityBoardGroupI18nEntity.Pk> {
}
