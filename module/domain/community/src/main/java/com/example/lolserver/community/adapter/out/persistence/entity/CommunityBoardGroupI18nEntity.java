package com.example.lolserver.community.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 그룹의 로케일별 라벨. PK 는 (group_id, locale) 복합키다.
 */
@Entity
@Table(name = "community_board_group_i18n")
@IdClass(CommunityBoardGroupI18nEntity.Pk.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityBoardGroupI18nEntity {

    @Id
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Id
    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * JPA 복합키 식별자. record 는 no-arg 생성자가 없어 IdClass 로 쓸 수 없다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pk implements java.io.Serializable {

        private Long groupId;
        private String locale;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pk other)) {
                return false;
            }
            return java.util.Objects.equals(groupId, other.groupId)
                    && java.util.Objects.equals(locale, other.locale);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(groupId, locale);
        }
    }
}
