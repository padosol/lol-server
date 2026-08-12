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

/**
 * 게시판(카테고리).
 *
 * <p>그룹을 {@code @ManyToOne} 연관으로 걸지 않고 {@code groupId} 원시 값으로 둔다.
 * 조회가 "전체를 정렬해서 한 번에" 뿐이라 지연 로딩이 이득이 없고, 그룹과 카테고리를
 * 각각 한 번씩 조회해 메모리에서 묶는 편이 정렬 계약을 SQL 한 곳에 모아둘 수 있다.
 */
@Entity
@Table(
        name = "community_category",
        uniqueConstraints = @UniqueConstraint(name = "uq_cc_code", columnNames = "code"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 게시글 category 컬럼에 저장되는 코드. 변경 금지(기존 글이 참조). */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    /** 같은 group_id 안에서의 상대 순서. 그룹이 다르면 값이 겹쳐도 정상이다. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** FALSE 면 목록/사이드바에서 숨김. 기존 글은 유지된다. */
    @Column(name = "active", nullable = false)
    private boolean active;

    /** FALSE 면 신규 작성 불가(공지 등 읽기 전용 게시판). */
    @Column(name = "writable", nullable = false)
    private boolean writable;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
