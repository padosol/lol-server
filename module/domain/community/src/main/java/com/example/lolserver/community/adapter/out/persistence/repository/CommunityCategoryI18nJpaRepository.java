package com.example.lolserver.community.adapter.out.persistence.repository;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityCategoryI18nEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCategoryI18nJpaRepository
        extends JpaRepository<CommunityCategoryI18nEntity, CommunityCategoryI18nEntity.Pk> {
}
