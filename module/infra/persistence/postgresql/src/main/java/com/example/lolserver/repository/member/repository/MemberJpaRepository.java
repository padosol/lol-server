package com.example.lolserver.repository.member.repository;

import com.example.lolserver.repository.member.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {

    @Query("SELECT m FROM MemberEntity m "
            + "LEFT JOIN FETCH m.socialAccounts "
            + "WHERE m.id = :id")
    Optional<MemberEntity> findByIdWithSocialAccounts(
            @Param("id") Long id);
}
