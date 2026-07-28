package com.example.lolserver.community.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "community_bookmark",
        // V31 의 uq_cb_member_post 와 같은 제약. 선언하지 않으면 엔티티로 스키마를
        // 만드는 환경(H2 create-drop 등)에서 중복 북마크가 그냥 들어간다.
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cb_member_post",
                columnNames = {"member_id", "post_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityBookmarkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    // createdAt 은 JPA Auditing 이 아니라 도메인 Bookmark.create() 가 채운다
    // (community 컨텍스트 전체가 같은 방식).
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
