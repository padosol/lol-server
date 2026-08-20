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
        name = "community_image",
        // V36 의 uq_ci_storage_key 와 같은 제약. 같은 키로 두 행이 생기면
        // 정리 배치가 아직 살아 있는 남의 파일을 지운다.
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ci_storage_key",
                columnNames = "storage_key"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * 첨부된 게시글. 업로드가 글 저장보다 먼저 일어나므로 NULL 을 허용한다
     * (에디터에 이미지를 넣은 뒤 글을 쓰다 만 상태 = PENDING).
     *
     * <p>{@code @ManyToOne} 을 걸지 않는 것은 community 컨텍스트의 다른 엔티티와 같은 이유다 —
     * 이미지 조회가 게시글 엔티티를 필요로 하지 않는다.
     */
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(nullable = false, length = 1024)
    private String url;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column
    private Integer width;

    @Column
    private Integer height;

    /**
     * UPLOADING / PENDING / ATTACHED / DETACHED.
     *
     * <p>{@code duo_post.status} 와 같이 문자열로 저장한다. {@code @Enumerated(ORDINAL)} 이면
     * enum 상수 순서를 바꾸는 순간 기존 행의 의미가 조용히 뒤바뀐다.
     */
    @Column(nullable = false, length = 20)
    private String status;

    // created_at/updated_at 은 JPA Auditing 이 아니라 도메인 PostImage 가 채운다
    // (community 컨텍스트 전체가 같은 방식). 정리 배치의 판정 기준이 updated_at 이므로
    // 상태를 바꾼 쪽이 시각도 함께 갱신해야 일관된다.
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
